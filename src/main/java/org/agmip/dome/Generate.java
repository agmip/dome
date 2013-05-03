package org.agmip.dome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.agmip.util.MapUtil;

// Temporary imports to integrate @MengZhang codebase from another project
import org.agmip.functions.ExperimentHelper;


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

    public static ArrayList<ArrayList<HashMap<String, String>>> runEvent(HashMap m, String[] args, ArrayList<ArrayList<HashMap<String, String>>> modifiers) {
        ArrayList<ArrayList<HashMap<String, String>>> results;
        String fun = args[0].toUpperCase();
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);

        if (modifiers.isEmpty()) {
            // Conceptually, there should be a base number (lowest value? of
            // entries (eg. pdate has 3 entries). Typically, this would be the
            // divisor for all other maps. (eg. fdate has 6 entries: 2 per pdate)
            // If there is only ONE entry for the map (only pdate) then we do
            // not need to run this
//            if( genResults.size() > 1) {
//                String lowestKey = "";
//                Integer lowestCount = null;
//                // Here is where we rebuild the map for the following stuff.
//                for (Map.Entry<String, ArrayList<String>> entry: genResults.entrySet()) {
//                    if (lowestCount == null) {
//                        lowestCount = entry.getValue().size();
//                    } else {
//                        if (lowestCount < entry.getValue().size()) {
//                            lowestCount
//                        }
//                    }
//                }
//            }
            results = executeEvent(m, fun, newArgs);
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
            exname = exname.substring(0,exname.indexOf("__"));
        }
        if (id != null) {
            String[] exvalue = {exname+"__"+id};
            Assume.run(m, "exname", exvalue, true);
        }
        for (Map.Entry<String, String> rule : rules.entrySet()) {
            String[] value = {rule.getValue()};
            Assume.run(m, rule.getKey(), value, true);
        }
    }

    protected static void applyReplicatedEvents(HashMap m, ArrayList<HashMap<String, String>> events, String id) {
        applyGeneratedRules(m, new HashMap(), id);
        ArrayList<HashMap<String, String>> oringEvents = MapUtil.getBucket(m, "management").getDataList();
        oringEvents.clear();
        oringEvents.addAll(events);
    }

    private static HashMap<String, ArrayList<String>> execute(HashMap m, String fun, String[] args) {
        if (fun.equals("AUTO_PDATE()")) {
            if (args.length < 4) {
                log.error("Not enough arguments for {}", fun);
                return new HashMap<String, ArrayList<String>>();
            }
            m.put("origin_pdate", ExperimentHelper.getFstPdate(m, ""));
            return ExperimentHelper.getAutoPlantingDate(m, args[0], args[1], args[2], args[3]);
        } else {
            log.error("DOME Function {} unsupported.", fun);
            return new HashMap<String, ArrayList<String>>();
        }
    }

    private static ArrayList<ArrayList<HashMap<String, String>>> executeEvent(HashMap m, String fun, String[] args) {
        if (fun.equals("AUTO_REPLICATE_EVENTS()")) {
            if (args.length != 0) {
                log.warn("Too many arguments for {}", fun);
            }
            return ExperimentHelper.getAutoEventDate(m);
        } else {
            log.error("DOME Function {} unsupported. 2", fun);
            return new ArrayList<ArrayList<HashMap<String, String>>>();
        }
    }
}
