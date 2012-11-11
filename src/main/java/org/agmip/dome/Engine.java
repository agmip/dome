package org.agmip.dome;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.agmip.dome.DomeUtil;

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
    private final ArrayList<HashMap<String, String>> rules;

    /**
     * Construct a new engine with the ruleset passed in.
     * @param dome A full DOME
     */
    public Engine(HashMap<String, Object> dome) {
        this.rules = DomeUtil.getDomeRules(dome);
    }

    /**
     * Construct a new engine with the ruleset passed in.
     * @param rules A DOME ruleset.
     */
    public Engine(ArrayList<HashMap<String, String>> rules) {
        this.rules = rules;
    }

    protected Engine() {
        this.rules = new ArrayList<HashMap<String,String>>();
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
            String args[] = a.split("[|]");

            if (cmd.equals("INFO")) {
                log.info("Recevied an INFO command");
            } else if (cmd.equals("FILL") || cmd.equals("REPLACE")) {
                boolean replace = true;
                if (cmd.equals("FILL")) replace=false;
                if (args[0].endsWith("()")) {
                    Calculate.run(data, rule.get("variable"), args, replace); 
                } else {
                    Assume.run(data, rule.get("variable"), args, replace);
                }
            } else {
                log.info("Invalid command: [{}]", cmd);
            }
        }
    }

    protected void addRule(HashMap<String,String> rule) {
        rules.add(rule);
    }
}
