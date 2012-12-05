package org.agmip.dome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.agmip.ace.AcePathfinder;
import org.agmip.ace.util.AcePathfinderUtil;
import org.agmip.util.MapUtil;
import org.agmip.common.Functions;

// Temporary imports to integrate @MengZhang codebase from another project
import org.agmip.functions.ExperimentHelper;
import org.agmip.functions.SoilHelper;


public class Generate {
    public static final Logger log = LoggerFactory.getLogger(Generate.class);

    public static ArrayList<HashMap<String, String>> run(HashMap m, String[] args, ArrayList<HashMap<String, String>> modifiers) {
        ArrayList<HashMap<String, String>> results = new ArrayList<HashMap<String, String>>();
        String fun = args[0].toUpperCase();
        String[] newArgs =  Arrays.copyOfRange(args, 1, args.length);

        if (modifiers.isEmpty()) {
            HashMap<String, ArrayList<String>> genResults = execute(m, fun, newArgs);
            for (Map.Entry<String, ArrayList<String>> entry : genResults.entrySet()) {
                String key = entry.getKey();
                int i = 0;
                for (String value : entry.getValue()) {
                    HashMap<String, String> output = new HashMap<String, String>();
                    output.put(key, value);
                    results.add(output);
                }
            }
        } else {
            // Safety to not lose what already exists.
            log.error("Multiple generators are unsupported in this version.");
            results = modifiers;
        }

        return results;
    }

    protected static void applyGeneratedRules(HashMap m, HashMap<String, String> rules, String id) {
        // Just apply each entry as a REPLACE ASSUME
        // Clear out existing events, since they are invalid!
        String exname = MapUtil.getValueOr(m, "exname", "");
        if (exname.contains("__")) {
            exname = exname.substring(0,exname.indexOf("_"));
        }
        String[] exvalue = {exname+"__"+id};
        Assume.run(m, "exname", exvalue, true);
        for (Map.Entry<String, String> rule : rules.entrySet()) {
            String[] value = {rule.getValue()};
            Assume.run(m, rule.getKey(), value, true);
        }
    }

    private static HashMap<String, ArrayList<String>> execute(HashMap m, String fun, String[] args) {
        if (fun.equals("AUTO_PDATE()")) {
            if (args.length < 4) {
                log.error("Not enough arguments for {}", fun);
                return new HashMap<String, ArrayList<String>>();
            }
            return ExperimentHelper.getAutoPlantingDate(args[0], args[1], args[2], args[3], m);
        } else {
            log.error("DOME Function {} unsupported.", fun);
            return new HashMap<String, ArrayList<String>>();
        }
    }
}