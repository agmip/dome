package org.agmip.dome;

import com.rits.cloning.Cloner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.agmip.util.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Engine of the DOME, which reads in a DOME ruleset and applies the rules
 * to a dataset.
 *
 * Terms <ul> <li><strong>Command</strong> which method (FILL/REPLACE) are we
 * working in</li> <li><strong>Function</strong> a function used to populate the
 * command</li> <li><strong>Static</strong> populate the command with a static
 * reference (either a variable or a value)</li> </ul>
 *
 */
public class Engine {

    private static final Logger log = LoggerFactory.getLogger(Engine.class);
    private ArrayList<HashMap<String, String>> rules;
    private ArrayList<HashMap<String, String>> generators;
    private boolean allowGenerators;
    /* Each group contains multilpe non-generating rules at first, one and only one generating rule is allowed in the last of the array. */
    private ArrayList<ArrayList<HashMap<String, String>>> genGroups;
    private ArrayList<HashMap<String, String>> genRules = null;
    /* A list of keys to extract from the resulting generated datasets. */
    HashSet<String> keysToExtractFinal = new HashSet<String>();
    private int cur = 0;

    /**
     * Construct a new engine with the ruleset passed in.
     *
     * @param dome A full DOME
     * @param allowGenerators allow generators to be run
     */
    public Engine(HashMap<String, Object> dome, boolean allowGenerators) {
        this.rules = DomeUtil.getRules(dome);
        this.generators = new ArrayList<HashMap<String, String>>();
        if (allowGenerators) {
            this.genGroups = DomeUtil.getGenerators(dome);
        } else {
            this.genGroups = new ArrayList<ArrayList<HashMap<String, String>>>();
        }
        this.allowGenerators = allowGenerators;
    }

    /**
     * Construct a new engine with the ruleset passed in. Generators are
     * <strong>not</strong> allowed by default.
     *
     * @param dome A full DOME
     */
    public Engine(HashMap<String, Object> dome) {
        this(dome, false);
    }

    /**
     * Construct a new engine with the ruleset passed in.
     *
     * @param rules A DOME ruleset.
     */
    public Engine(ArrayList<HashMap<String, String>> rules) {
        this.rules = rules;
        this.generators = new ArrayList<HashMap<String, String>>();
        this.genGroups = new ArrayList<ArrayList<HashMap<String, String>>>();
        this.allowGenerators = false;
    }

    protected Engine() {
        this.rules = new ArrayList<HashMap<String, String>>();
        this.generators = new ArrayList<HashMap<String, String>>();
        this.genGroups = new ArrayList<ArrayList<HashMap<String, String>>>();
        this.allowGenerators = false;
    }

    /**
     * Add more rules to the Engine
     *
     * @param rules new set of rules to append (from another DOME)
     */
    public void appendRules(ArrayList<HashMap<String, String>> rules) {
        this.rules.addAll(rules);
    }

    /**
     * Apply the rule set to the dataset passed in.
     *
     * @param data A dataset to modify according to the DOME ruleset.
     */
    public void apply(HashMap<String, Object> data) {
        for (HashMap<String, String> rule : rules) {
            applyRule(data, rule);
        }
    }

    private void applyRule(HashMap<String, Object> data, HashMap<String, String> rule) {
        String cmd = rule.get("cmd").toUpperCase();

        // NPE defender
        if (rule.get("variable") == null) {
            log.error("Invalid rule: {}", rule.toString());
            return;
        }

        String a = rule.get("args");
        if (a == null) {
            a = "";
        }
        String[] args = a.split("[|]");

        if (cmd.equals("INFO")) {
            log.debug("Recevied an INFO command");
        } else if (cmd.equals("FILL") || cmd.equals("REPLACE") || cmd.equals("REPLACE_FIELD_ONLY")) {
            boolean replace = true;
            if (cmd.equals("FILL")) {
                replace = false;
            }
            if (args[0].endsWith("()")) {
                Calculate.run(data, rule.get("variable"), args, replace);
            } else {
                if (cmd.equals("REPLACE_FIELD_ONLY")) {
                    log.debug("Found FIELD_ONLY replace");
                    if (data.containsKey("seasonal_dome_applied")) {
                        log.warn("Replace for {} not applied due to FIELD_ONLY restriction", rule.get("variable"));
                    } else {
                        log.debug("Found data without seasonal_dome_applied set.");
                        Assume.run(data, rule.get("variable"), args, replace);
                    }
                } else {
                    Assume.run(data, rule.get("variable"), args, replace);
                }
            }
        } else {
            log.error("Invalid command: [{}]", cmd);
        }
    }

    /**
     * Apply the groups of strategy rules to the dataset passed in.
     *
     * @param data The data set
     * @return The list of new generated data set
     */
    public ArrayList<HashMap<String, Object>> applyStg(HashMap<String, Object> data) {
        ArrayList<HashMap<String, Object>> dataArr = new ArrayList<HashMap<String, Object>>();
        dataArr.add(data);
        return applyStg(dataArr);
    }

    /**
     * Apply the groups of strategy rules to the dataset passed in.
     *
     * @param dataArr The list of data set
     * @return The list of new generated data set
     */
    public ArrayList<HashMap<String, Object>> applyStg(ArrayList<HashMap<String, Object>> dataArr) {

        ArrayList<HashMap<String, Object>> results = new ArrayList();

        // Check if there is generator command group left
        if (hasMoreGenRules()) {
            // Apply rules to each entry of data
            for (HashMap<String, Object> data : dataArr) {
                ArrayList<HashMap<String, Object>> arr = new ArrayList();
                if (genRules != null && !genRules.isEmpty()) {
                    for (int i = 0; i < genRules.size() - 1; i++) {
                        HashMap<String, String> rule = genRules.get(i);
                        applyRule(data, rule);
                    }
                    HashMap<String, String> gemRule = genRules.get(genRules.size() - 1);
                    if (!gemRule.isEmpty()) {
                        generators.add(gemRule);
                        arr = runGenerators(data, cur != genGroups.size() - 1);
                        generators.clear();
                    }
                }
                if (arr.isEmpty()) {
                    results.add(data);
                } else {
                    results.addAll(arr);
                }
            }

            // Try yo apply next group of genertators
            results = applyStg(results);
        } else {
            // Finish recursion
            results = dataArr;
            // Remove reference
            boolean wthRefFlg = !keysToExtractFinal.contains("weather");
            boolean soilRefFlg = !keysToExtractFinal.contains("soil");
            for (HashMap result : results) {
                if (wthRefFlg) {
                    result.remove("weather");
                }
                if (soilRefFlg) {
                    result.remove("soil");
                }
            }
        }

        return results;
    }

    /**
     * Run the generators on the dataset passed in. This will generate a number
     * of additional datasets based on the original dataset.
     *
     * @param data A dataset to run the generators on
     *
     * @return A {@code HashMap} of just the exported keys.
     */
    public ArrayList<HashMap<String, Object>> runGenerators(HashMap<String, Object> data) {
        return runGenerators(data, false);
    }

    /**
     * Run the generators on the dataset passed in. This will generate a number
     * of additional datasets based on the original dataset.
     *
     * @param data A dataset to run the generators on
     * @param refLeftFlg A flag for if the references of all extractable data
     * (weather/soil) are left in the result. True for left, False for not left.
     *
     * @return A {@code HashMap} of just the exported keys.
     */
    public ArrayList<HashMap<String, Object>> runGenerators(HashMap<String, Object> data, boolean refLeftFlg) {
        if (this.allowGenerators) {
            log.debug("Starting generators");
            ArrayList<HashMap<String, Object>> results = new ArrayList<HashMap<String, Object>>();
            HashSet<String> keysToExtract = new HashSet<String>();
            ArrayList<HashMap<String, String>> gAcc = new ArrayList<HashMap<String, String>>();
            ArrayList<ArrayList<HashMap<String, String>>> newEventArrs = new ArrayList<ArrayList<HashMap<String, String>>>();
            // Run the generators
            for (HashMap<String, String> generator : generators) {
                // NPE defender
                if (generator.get("variable") == null) {
                    log.error("Invalid generator: {}", generator.toString());
                    return new ArrayList<HashMap<String, Object>>();
                }

                String path = Command.getPathOrRoot(generator.get("variable"));
                if (path.contains("weather")) {
                    keysToExtract.add("weather");
                } else if (path.contains("soil")) {
                    keysToExtract.add("soil");
                } else {
                    keysToExtract.add("experiment");
                }

                String a = generator.get("args");
                if (a == null) {
                    a = "";
                }
                String[] args = a.split("[|]");

                if (args[0].toUpperCase().equals("AUTO_REPLICATE_EVENTS()")) {
                    newEventArrs = Generate.runEvent(data, args, newEventArrs);
                } else {
                    gAcc = Generate.run(data, args, gAcc);
                }
            }
            // On the output of "each" generation, put the export blocks into results
            HashMap tempRefHolder = new HashMap();
            if (!keysToExtract.contains("weather")) {
                tempRefHolder.put("weather", data.remove("weather"));
            }
            if (!keysToExtract.contains("soil")) {
                tempRefHolder.put("soil", data.remove("soil"));
            }
            if (refLeftFlg) {
                this.keysToExtractFinal.addAll(keysToExtract);
            }
            Cloner cloner = new Cloner();
            if (newEventArrs.isEmpty()) {
                int i = 0;
                for (HashMap<String, String> rules : gAcc) {
                    i++;
                    Generate.applyGeneratedRules(data, rules, "" + i);
                    HashMap newData = cloner.deepClone(data);
                    if (refLeftFlg) {
                        newData.putAll(tempRefHolder);
                    }
                    results.add(newData);
                }
            } else {
                if (refLeftFlg) {
                    data.putAll(tempRefHolder);
                }
                ArrayList<HashMap<String, String>> oringEvents = MapUtil.getBucket(data, "management").getDataList();

                for (int i = 1; i < newEventArrs.size(); i++) {
                    ArrayList<HashMap<String, String>> eventArr = newEventArrs.get(i);
                    oringEvents.addAll(eventArr);
//                    HashMap newData = cloner.deepClone(data);
                }
                results.add(data);
            }
            return results;
            // return the results.
        } else {
            log.error("You cannot run generators in this mode.");
            return new ArrayList<HashMap<String, Object>>();
        }

    }

    protected void addRule(HashMap<String, String> rule) {
        rules.add(rule);
    }

    protected void addGenerator(HashMap<String, String> generator) {
        generators.add(generator);
    }

    protected void addGenGroup(ArrayList<HashMap<String, String>> rules, HashMap<String, String> generator) {
        ArrayList<HashMap<String, String>> genGroup = new ArrayList<HashMap<String, String>>();
        genGroup.addAll(rules);
        genGroup.add(generator);
        genGroups.add(genGroup);
    }

    protected void enableGenerators() {
        this.allowGenerators = true;
    }

    protected void disableGenerators() {
        this.allowGenerators = false;
    }

    private boolean hasMoreGenRules() {
        if (cur < genGroups.size()) {
            genRules = genGroups.get(cur);
            cur++;
            return true;
        } else {
            genRules = null;
            return false;
        }
    }

    /**
     * Get the list of loaded generator rules
     *
     * @return The list of DOME command string with format
     * (command,variable,arguments)
     */
    public ArrayList<String> getGenerators() {
        ArrayList<String> genList = new ArrayList<String>();
        for (ArrayList<HashMap<String, String>> genGroup : genGroups) {
            if (!genGroup.isEmpty()) {
                HashMap<String, String> genRule = genGroup.get(genGroup.size() - 1);
                if (!genRule.isEmpty()) {
                    genList.add(genRule.get("cmd") + "," + genRule.get("variable") + "," + genRule.get("args"));
                }
            }
        }

        return genList;
    }
}
