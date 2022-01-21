package org.agmip.dome;

import com.rits.cloning.Cloner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.agmip.functions.ExperimentHelper;
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
    private boolean isSWExtracted = false;
    /* Each group contains multilpe non-generating rules at first, one and only one generating rule is allowed in the last of the array. */
    private ArrayList<ArrayList<HashMap<String, String>>> genGroups;
    private ArrayList<HashMap<String, String>> genRules = null;
    /* A list of keys to extract from the resulting generated datasets. */
    HashSet<String> keysToExtractFinal = new HashSet<String>();
    HashSet<String> skipVarList = new HashSet();
    private int cur = 0;
    private String domeName;

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
        this.domeName = DomeUtil.generateDomeName(dome);
    }
    
    public Engine(HashMap<String, Object> dome, boolean allowGenerators, ArrayList<String> skipVarList) {
        this(dome, allowGenerators);
        this.skipVarList.addAll(skipVarList);
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

    public Engine(HashMap<String, Object> dome, ArrayList<String> skipVarList) {
        this(dome, false);
        this.skipVarList.addAll(skipVarList);
    }

    /**
     * Construct a new engine with the ruleset passed in.
     *
     * @param rules A DOME ruleset.
     * @param domeName
     */
    public Engine(ArrayList<HashMap<String, String>> rules, String domeName) {
        this.rules = rules;
        this.generators = new ArrayList<HashMap<String, String>>();
        this.genGroups = new ArrayList<ArrayList<HashMap<String, String>>>();
        this.allowGenerators = false;
        this.domeName = domeName;
    }
    
    public Engine(ArrayList<HashMap<String, String>> rules, String domeName, ArrayList<String> skipVarList) {
        this(rules, domeName);
        this.skipVarList.addAll(skipVarList);
    }

    protected Engine() {
        this.rules = new ArrayList<HashMap<String, String>>();
        this.generators = new ArrayList<HashMap<String, String>>();
        this.genGroups = new ArrayList<ArrayList<HashMap<String, String>>>();
        this.allowGenerators = false;
        this.domeName = "";
    }
    
    public String getDomeName() {
        return this.domeName;
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
            if (!isSWExtracted || (!isSoilRules(rule) && !isWthRules(rule))) {
                applyRule(data, rule);
            }
        }
    }

    protected void applyRule(HashMap<String, Object> data, HashMap<String, String> rule) {
        if (isSkippedRule(rule)) {
            return;
        }
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
        } else if (cmd.equals("FILL") || cmd.equals("REPLACE") || cmd.equals("REPLACE_FIELD_ONLY") || cmd.equals("REPLACE_STRATEGY_ONLY")) {
            boolean replace = true;
            if (cmd.equals("FILL")) {
                replace = false;
            }
            if (args[0].endsWith("()")) {
                if (cmd.equals("REPLACE_FIELD_ONLY")) {
                    log.debug("Found FIELD_ONLY replace");
                    if (data.containsKey("seasonal_dome_applied")) {
                        log.info("Replace for {} not applied due to FIELD_ONLY restriction", rule.get("variable"));
                    } else {
                        log.debug("Found data without seasonal_dome_applied set.");
                        Calculate.run(data, rule.get("variable"), args, replace);
                    }
                } else if (cmd.equals("REPLACE_STRATEGY_ONLY")) {
                    log.debug("Found STRATEGY_ONLY replace");
                    if (!data.containsKey("seasonal_dome_applied")) {
                        log.info("Replace for {} not applied due to STRATEGY_ONLY restriction", rule.get("variable"));
                    } else {
                        log.debug("Found data with seasonal_dome_applied set.");
                        Calculate.run(data, rule.get("variable"), args, replace);
                    }
                } else {
                    Calculate.run(data, rule.get("variable"), args, replace);
                }
            } else {
                if (cmd.equals("REPLACE_FIELD_ONLY")) {
                    log.debug("Found FIELD_ONLY replace");
                    if (data.containsKey("seasonal_dome_applied")) {
                        log.info("Replace for {} not applied due to FIELD_ONLY restriction", rule.get("variable"));
                    } else {
                        log.debug("Found data without seasonal_dome_applied set.");
                        Assume.run(data, rule.get("variable"), args, replace);
                    }
                } else if (cmd.equals("REPLACE_STRATEGY_ONLY")) {
                    log.debug("Found STRATEGY_ONLY replace");
                    if (!data.containsKey("seasonal_dome_applied")) {
                        log.info("Replace for {} not applied due to STRATEGY_ONLY restriction", rule.get("variable"));
                    } else {
                        log.debug("Found data with seasonal_dome_applied set.");
                        Assume.run(data, rule.get("variable"), args, replace);
                    }
                } else {
                    Assume.run(data, rule.get("variable"), args, replace);
                }
            }
        } else if (cmd.equals("CREATE")) {
            Calculate.create(data, rule.get("variable"), args);
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
        // Check if there is generator command group left
        if (hasMoreGenRules()) {
            // Apply rules to input data
            ArrayList<HashMap<String, Object>> arr = new ArrayList();
            if (genRules != null && !genRules.isEmpty()) {
                for (int i = 0; i < genRules.size() - 1; i++) {
                    HashMap<String, String> rule = genRules.get(i);
                    applyRule(data, rule);
                }
                // Get generator rules
                HashMap<String, String> gemRule = genRules.get(genRules.size() - 1);
                if (!gemRule.isEmpty()) {
                    generators.add(gemRule);
                    arr = runGenerators(data, cur != genGroups.size());
                    generators.clear();
                }
            }

            // Try yo apply next group of genertators
            if (arr.isEmpty()) {
                return applyStg(data);
            } else {
                return applyStg(arr);
            }
        } else {
            ArrayList<HashMap<String, Object>> results = new ArrayList();
            results.add(data);
            return results;
        }
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
            if (genRules != null && !genRules.isEmpty()) {
                // Apply non-generator rules to each entry of data
                for (HashMap<String, Object> data : dataArr) {
                    ArrayList<HashMap<String, Object>> arr = new ArrayList();
                    for (int i = 0; i < genRules.size() - 1; i++) {
                        HashMap<String, String> rule = genRules.get(i);
                        applyRule(data, rule);
                    }
                }
                // Apply generator rules to whole data set
                HashMap<String, String> gemRule = genRules.get(genRules.size() - 1);
                if (!gemRule.isEmpty()) {
                    generators.add(gemRule);
                    results = runGenerators(dataArr, cur != genGroups.size());
                    generators.clear();
                } else {
                    results = dataArr;
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
                for (HashMap<String, String> grules : gAcc) {
                    i++;
                    Generate.applyGeneratedRules(data, grules, "" + i);
                    HashMap newData = cloner.deepClone(data);
                    if (refLeftFlg) {
                        newData.putAll(tempRefHolder);
                    }
                    results.add(newData);
                }
            } else {
                int i = 0;
                for (ArrayList<HashMap<String, String>> eventArr : newEventArrs) {
                    i++;
                    Generate.applyReplicatedEvents(data, eventArr, "" + i);
                    HashMap newData = cloner.deepClone(data);
                    if (refLeftFlg) {
                        newData.putAll(tempRefHolder);
                    }
                    results.add(newData);
                }
            }
            return results;
            // return the results.
        } else {
            log.error("You cannot run generators in this mode.");
            return new ArrayList<HashMap<String, Object>>();
        }

    }

    /**
     * Run the generators on the dataset passed in. This will generate a number
     * of additional datasets based on the original dataset.
     *
     * @param dataArr A list of dataset to run the generators on
     * @param refLeftFlg A flag for if the references of all extractable data
     * (weather/soil) are left in the result. True for left, False for not left.
     *
     * @return A {@code HashMap} of just the exported keys.
     */
    public ArrayList<HashMap<String, Object>> runGenerators(ArrayList<HashMap<String, Object>> dataArr, boolean refLeftFlg) {
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
                    String[] pdates = new String[dataArr.size()];
                    for (int i = 0; i < dataArr.size(); i++) {
                        pdates[i] = "";
                        HashMap<String, Object> data = dataArr.get(i);
                        ArrayList<HashMap<String, String>> events = MapUtil.getBucket(data, "management").getDataList();
                        for (HashMap<String, String> event : events) {
                            if ("planting".equals(MapUtil.getValueOr(event, "event", ""))) {
                                pdates[i] = MapUtil.getValueOr(event, "date", "");
                                break;
                            }
                        }
                    }
                    HashMap<String, Object> data;
                    if (dataArr.isEmpty()) {
                        data = new HashMap();
                    } else {
                        data = dataArr.get(0);
                    }
                    newEventArrs = ExperimentHelper.getAutoEventDate(data, pdates);
                } else {
                    HashMap<String, Object> data;
                    if (dataArr.isEmpty()) {
                        data = new HashMap();
                    } else {
                        data = dataArr.get(0);
                    }
                    gAcc = Generate.run(data, args, gAcc);
                }
            }
            // On the output of "each" generation, put the export blocks into results
            if (refLeftFlg) {
                this.keysToExtractFinal.addAll(keysToExtract);
            }
            if (newEventArrs.isEmpty()) {
                if (gAcc.size() != dataArr.size()) {
                    log.error("The number of calculated values is not match with the number of generated experiments");
                    return results;
                }

                for (int i = 0; i < gAcc.size(); i++) {
                    Generate.applyGeneratedRules(dataArr.get(i), gAcc.get(i), null);
                }
                results = dataArr;
            } else {
                if (newEventArrs.size() != dataArr.size()) {
                    log.error("The number of calculated events is not match with the number of generated experiments");
                    return results;
                }

                for (int i = 0; i < newEventArrs.size(); i++) {
                    Generate.applyReplicatedEvents(dataArr.get(i), newEventArrs.get(i), null);
                }
                results = dataArr;
            }
            return results;
            // return the results.
        } else {
            log.error("You cannot run generators in this mode.");
            return new ArrayList<HashMap<String, Object>>();
        }

    }

    public boolean updateWSRef(HashMap<String, Object> exp, boolean isStgDome, boolean isStgMode) {

        boolean isClimIDchanged = false;
        for (HashMap<String, String> rule : rules) {
//            if (isSkippedRule(rule)) {
//                continue;
//            }
            boolean isSkippedRule = isSkippedRule(rule);
            String var = MapUtil.getValueOr(rule, "variable", "").toLowerCase();
            String cmd = MapUtil.getValueOr(rule, "cmd", "").toUpperCase();
            if (var.equals("clim_id")) {

                String wst_id = MapUtil.getValueOr(exp, "wst_id", "");
                String val = MapUtil.getValueOr(rule, "args", "").toUpperCase();
                if (val.equals("")) {
                    val = "0XXX";
                }

                // scan seasonal strategy dome, or overlay dome in overlay mode
                if (!isSkippedRule && (isStgDome || (!isStgMode && val.startsWith("0")))) {
                    exp.remove("soil");
                    exp.remove("weather");
                    exp.put("clim_id", val);
                    isClimIDchanged = true;
                }

                if (!isSkippedRule && !isStgMode && !val.startsWith("0")) {
                    log.warn("Invalid CLIM_ID assigned for baseline weather data: {}", val);
                }

                // Commented this statement to avoid destroy the updated linkage, both REPLACE and FILL
                if (!cmd.equals("INFO")) {
                    rule.put("cmd", "INFO");
                }
            } else if (var.equals("wst_id") || var.equals("soil_id")) {
                if (!isSkippedRule) {
                    if (!cmd.equals("FILL")) {
                        rule.put("cmd", "REPLACE");
                    }
                    exp.remove("soil");
                    exp.remove("weather");
                    applyRule(exp, rule);
                }
                // Commented this statement to avoid destroy the updated linkage
                if (!cmd.equals("FILL")) {
                    rule.put("cmd", "INFO");
                }
            }
        }
        return isClimIDchanged;
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

    public ArrayList<HashMap<String, String>> extractSoilRules() {
        ArrayList<HashMap<String, String>> sRules;
        if (allowGenerators) {
            sRules = new ArrayList<HashMap<String, String>>();
            for (ArrayList<HashMap<String, String>> genGroup : genGroups) {
                sRules.addAll(extractSoilRules(genGroup));
            }
        } else {
            sRules = extractSoilRules(rules);
        }
        isSWExtracted = true;

        return sRules;
    }

    public ArrayList<HashMap<String, String>> extractWthRules() {
        ArrayList<HashMap<String, String>> wRules;
        if (allowGenerators) {
            wRules = new ArrayList<HashMap<String, String>>();
            for (ArrayList<HashMap<String, String>> genGroup : genGroups) {
                wRules.addAll(extractWthRules(genGroup));
            }
        } else {
            wRules = extractWthRules(rules);
        }
        isSWExtracted = true;

        return wRules;
    }

    protected ArrayList<HashMap<String, String>> extractSoilRules(ArrayList<HashMap<String, String>> rules) {
        ArrayList<HashMap<String, String>> swRules = new ArrayList<HashMap<String, String>>();
        for (HashMap<String, String> rule : rules) {
            if (isSoilRules(rule)) {
                swRules.add(rule);
            }
        }
        return swRules;
    }

    protected ArrayList<HashMap<String, String>> extractWthRules(ArrayList<HashMap<String, String>> rules) {
        ArrayList<HashMap<String, String>> swRules = new ArrayList<HashMap<String, String>>();
        for (HashMap<String, String> rule : rules) {
            if (isWthRules(rule)) {
                swRules.add(rule);
            }
        }
        return swRules;
    }

    protected boolean isSoilRules(HashMap<String, String> rule) {
        boolean isSWRule = false;
        String cmd = rule.get("cmd").toUpperCase();
        String var = rule.get("variable").toLowerCase();

        // NPE defender
        if (var == null) {
            log.error("Invalid rule: {}", rule.toString());
            return isSWRule;
        }

        String a = rule.get("args");
        if (a == null) {
            a = "";
        }
        String[] args = a.split("[|]");

        if (cmd.equals("INFO")) {
            log.debug("Recevied an INFO command");
        } else if (cmd.equals("FILL") || cmd.equals("REPLACE") || cmd.equals("REPLACE_FIELD_ONLY") || cmd.equals("REPLACE_STRATEGY_ONLY")) {
            // If it is simple calculation or set value directly to the soil/weather variable
            if (!args[0].endsWith("()")
                    || args[0].equals("OFFSET()")
                    || args[0].equals("MULTIPLY()")
                    || args[0].equals("OFFSET_DATE()")
                    || args[0].equals("DATE_OFFSET()")
                    || args[0].equals("ROOT_DIST()")
                    || args[0].equals("LYRSET()")
                    || args[0].equals("TRANSPOSE()")
                    || args[0].equals("REDUCEWP()")) {
                if (!var.equals("soil_id")) {
                    String path = Command.getPathOrRoot(var);
                    String[] paths = path.split(",");
                    for (String p : paths) {
                        if (p.equals("soil") || p.equals("soil@soilLayer")) {
                            isSWRule = true;
                            break;
                        }
                    }
                }
            } // If call function which might change soil/weather data
            else if (args[0].equals("STABLEC()")
                    || args[0].equals("PTCALC()")) {
                isSWRule = true;
            }
        }
        return isSWRule;
    }

    protected boolean isWthRules(HashMap<String, String> rule) {
        boolean isWRule = false;
        String cmd = rule.get("cmd").toUpperCase();
        String var = rule.get("variable").toLowerCase();

        // NPE defender
        if (var == null) {
            log.error("Invalid rule: {}", rule.toString());
            return isWRule;
        }

        String a = rule.get("args");
        if (a == null) {
            a = "";
        }
        String[] args = a.split("[|]");

        if (cmd.equals("INFO")) {
            log.debug("Recevied an INFO command");
        } else if (cmd.equals("FILL") || cmd.equals("REPLACE") || cmd.equals("REPLACE_FIELD_ONLY") || cmd.equals("REPLACE_STRATEGY_ONLY")) {
            // If it is simple calculation or set value directly to the soil/weather variable
            if (!args[0].endsWith("()")
                    || args[0].equals("OFFSET()")
                    || args[0].equals("MULTIPLY()")
                    || args[0].equals("OFFSET_DATE()")
                    || args[0].equals("DATE_OFFSET()")
                    || args[0].equals("ROOT_DIST()")
                    || args[0].equals("LYRSET()")
                    || args[0].equals("TRANSPOSE()")
                    || args[0].equals("REDUCEWP()")) {
                if (!var.equals("wst_id") && !var.equals("clim_id")) {
                    String path = Command.getPathOrRoot(var);
                    String[] paths = path.split(",");
                    for (String p : paths) {
                        if (p.equals("weather") || p.equals("weather@dailyWeather")) {
                            isWRule = true;
                            break;
                        }
                    }
                }
            } // If call function which might change soil/weather data
            else if (args[0].equals("TAVAMP()")) {
                isWRule = true;
            }
        }
        return isWRule;
    }
    
    protected ArrayList<String> modifiedVarList(ArrayList<HashMap<String, String>> rules) {
        ArrayList<String> ret = new ArrayList<String>();
        for (HashMap<String, String> rule : rules) {
            String var = rule.get("variable").toLowerCase();
            String cmd = rule.get("cmd").toUpperCase();
            if (!cmd.equals("INFO") || var.equals("wst_id") || var.equals("clim_id")) {
                ret.add(var);
            }
        }
        return ret;
    }

    public ArrayList<String> modifiedVarList() {
        ArrayList<String> ret;
        if (allowGenerators) {
            ret  = new ArrayList<String>();
            for (ArrayList<HashMap<String, String>> genGroup : genGroups) {
                ret.addAll(modifiedVarList(genGroup));
            }
        } else {
            ret = modifiedVarList(rules);
        }
        isSWExtracted = true;

        return ret;
    }
    
    protected boolean isSkippedRule(HashMap<String, String> rule) {
        String var = rule.get("variable");
        if (var == null) {
            return true;
        } else {
            var = var.toLowerCase();
        }
        return skipVarList.contains(var);
    }
}
