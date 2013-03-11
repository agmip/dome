package org.agmip.dome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.agmip.dome.DomeUtil;
import org.agmip.ace.util.AcePathfinderUtil;

import com.rits.cloning.Cloner;

/**
 * The Engine of the DOME, which reads in a DOME ruleset and applies
 * the rules to a dataset.
 * 
 * Terms
 * <ul>
 * <li><strong>Command</strong> which method (FILL/REPLACE) are we working in</li>
 * <li><strong>Function</strong> a function used to populate the command</li>
 * <li><strong>Static</strong> populate the command with a static reference
 *  (either a variable or a value)</li>
 * </ul>
 *
 */
public class Engine {
    private static final Logger log = LoggerFactory.getLogger(Engine.class);
    private ArrayList<HashMap<String, String>> rules;
    private ArrayList<HashMap<String, String>> generators;
    private boolean allowGenerators;

    /**
     * Construct a new engine with the ruleset passed in.
     * @param dome A full DOME
     * @param allowGenerators allow generators to be run
     */
    public Engine(HashMap<String, Object> dome, boolean allowGenerators) {
        this.rules = DomeUtil.getRules(dome);
        this.generators = DomeUtil.getGenerators(dome);
        this.allowGenerators = allowGenerators;
    }

    /**
     * Construct a new engine with the ruleset passed in. Generators are 
     * <strong>not</strong> allowed by default.
     * @param dome A full DOME
     */
    public Engine(HashMap<String, Object> dome) {
        this.rules = DomeUtil.getRules(dome);
        this.generators = DomeUtil.getGenerators(dome);
        this.allowGenerators = false;
    }

    /**
     * Construct a new engine with the ruleset passed in.
     * @param rules A DOME ruleset.
     */
    public Engine(ArrayList<HashMap<String, String>> rules) {
        this.rules = rules;
        this.generators = new ArrayList<HashMap<String, String>>();
        this.allowGenerators = false;
    }

    protected Engine() {
        this.rules = new ArrayList<HashMap<String,String>>();
        this.generators = new ArrayList<HashMap<String, String>>();
        this.allowGenerators = false;
    }


    /**
     * Add more rules to the Engine
     * @param rules new set of rules to append (from another DOME)
     */
    public void appendRules(ArrayList<HashMap<String, String>> rules) {
        this.rules.addAll(rules);
    }

    /**
     * Apply the ruleset to the dataset passed in.
     *
     * @param data A dataset to modify according to the DOME ruleset.
     */
    public void apply(HashMap<String, Object> data) {
        for (HashMap<String, String> rule: rules) {
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
                if (cmd.equals("FILL")) replace=false;
                if (args[0].endsWith("()")) {
                    Calculate.run(data, rule.get("variable"), args, replace);
                } else {
                    if (cmd.equals("REPLACE_FIELD_ONLY")) {
                        log.debug("Found FIELD_ONLY replace");
                    }
                    if ( data.containsKey("seasonal_dome_applied")) {
                        log.info("Replace not applied due to FIELD_ONLY restriction");
                    } else {
                        log.debug("Found data without seasonal_dome_applied set.");
                        Assume.run(data, rule.get("variable"), args, replace);
                    }
                }
            } else {
                log.error("Invalid command: [{}]", cmd);
            }
        }
    }

    /**
     * Run the generators on the dataset passed in. This will generate a number
     * of additional datasets based on the original dataset.
     * 
     * @param data A dataset to run the generators on
     * @param keysToExtract A list of keys to extract from the resulting
     *                      generated datasets.
     *
     * @return A {@code HashMap} of just the exported keys.
     */
    public ArrayList<HashMap<String, Object>> runGenerators(HashMap<String, Object> data) {
        if (this.allowGenerators) {
            log.debug("Starting generators");
            ArrayList<HashMap<String, Object>> results = new ArrayList<HashMap<String, Object>>();
            HashSet<String> keysToExtract = new HashSet<String>();
            ArrayList<HashMap<String, String>> gAcc = new ArrayList<HashMap<String, String>>();
            // Run the generators
            for (HashMap<String, String> generator: generators) {
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

                gAcc = Generate.run(data, args, gAcc);
            }
            // On the output of "each" generation, put the export blocks into results
            if (! keysToExtract.contains("weather")) {
                data.remove("weather");
            }
            if (! keysToExtract.contains("soil")) {
                data.remove("soil");
            }
            Cloner cloner = new Cloner();
            int i = 0;
            for (HashMap<String, String> rules : gAcc) {
                i++;
                Generate.applyGeneratedRules(data, rules, ""+i);
                results.add(cloner.deepClone(data));
            }
            return results;
            // return the results.
        } else {
            log.error("You cannot run generators in this mode.");
            return new ArrayList<HashMap<String, Object>>();
        }

    }

    protected void addRule(HashMap<String,String> rule) {
        rules.add(rule);
    }

    protected void addGenerator(HashMap<String, String> generator) {
        generators.add(generator);
    }

    protected void enableGenerators() {
        this.allowGenerators = true;
    }

    protected void disableGenerators() {
        this.allowGenerators = false;
    }
}
