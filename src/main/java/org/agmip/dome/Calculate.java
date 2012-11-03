package org.agmip.dome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.agmip.ace.AcePathfinder;
import org.agmip.ace.util.AcePathfinderUtil;
import org.agmip.common.Functions;

// Temporary imports to integrate @MengZhang codebase from another project
import org.agmip.functions.ExperimentHelper;
import org.agmip.functions.SoilHelper;


public class Calculate extends Command {
    public static final Logger log = LoggerFactory.getLogger(Calculate.class);

    public static void run(HashMap m, String var, String[] args, boolean replace) {
        String fun = args[0].toUpperCase();
        String[] newArgs =  Arrays.copyOfRange(args, 1, args.length);
        HashMap<String, ArrayList<String>> calcResults = null;
        boolean mapModified = false;
        boolean destructiveMode = false;

        // These functions use the proper modifcation protocols.
        if (fun.equals("OFFSET_DATE()")) {
            if (newArgs.length < 2) {
                log.error("Not enough arguments for {}", fun);
                return;
            } else {
                calcResults = DomeFunctions.dateOffset(m, var, newArgs[0], newArgs[1]);
            }
        } else if (fun.equals("OFFSET()")) {
            if (newArgs.length < 2) {
                log.error("Not enough arguments for {}", fun);
                return;
            } else {
                calcResults = DomeFunctions.numericOffset(m, var, newArgs[0], newArgs[1]);
           }
        } else if (fun.equals("MULTIPLY()")) {
            if (newArgs.length < 2) {
                log.error("Not enough arguments for {}", fun);
                return;
            } else {
                calcResults = DomeFunctions.multiply(m, var, newArgs[0], newArgs[1]);
            }
        } else if (fun.equals("PCTAWC()")) {
            if (newArgs.length != 1) {
                log.error("Invalid number of arguments for {}", fun);
            } else {
                destructiveMode = true;
                calcResults = DomeFunctions.percentAvailWaterContent(m, newArgs[0]);
            }
        // These functions use older modification protocols.
        // These functions modify the map directly.
        } else if (fun.equals("FERT_DIST()")) {
            if (newArgs.length < 6) {
                log.error("Not enough arguments for {}", fun);
                return;
            } else {
                int numOfApplications = Functions.numericStringToBigInteger(newArgs[0]).intValue();
                if (newArgs.length != ((numOfApplications*2)+4)) {
                    log.error("Not enough arguments for {}", fun);
                    return;
                } else {
                    ArrayList<String> offset = new ArrayList<String>();
                    ArrayList<String> pct = new ArrayList<String>();
                    for (int i=4; i < newArgs.length; i++) {
                        if (i%2 == 0) {
                            offset.add(newArgs[i]);
                        } else {
                            pct.add(newArgs[i]);
                        }
                    }
                    log.debug("Calling with offset: {} and pct: {}", offset, pct);
                    
                    String [] offsetArr = offset.toArray(new String[offset.size()]);
                    String [] pctArr = pct.toArray(new String[pct.size()]);
                    ExperimentHelper.getFertDistribution(newArgs[0], newArgs[1], newArgs[2], newArgs[3], offsetArr, pctArr, m);
                    mapModified = true;
                }
            }
        } else if (fun.equals("OM_DIST()")) {
            if (newArgs.length < 6) {
                log.error("Not enough arguments for {}", fun);
                return;
            }
            ExperimentHelper.getOMDistribution(newArgs[0], newArgs[1], newArgs[2], newArgs[3], newArgs[4], newArgs[5], m);
            mapModified = true;
        } else if (fun.equals("ROOT_DIST()")) {
            if (newArgs.length < 3) {
                log.error("Not enough arguments for {}", fun);
                return;
            }
            SoilHelper.getRootDistribution(newArgs[0], newArgs[1], newArgs[2], m);
            mapModified = true;
        } else if (fun.equals("STABLEC()")) {
            if (newArgs.length < 3) {
                log.error("Not enough arguments for {}", fun);
                return;
            }
            ExperimentHelper.getStableCDistribution(newArgs[0], newArgs[1], newArgs[2], m);
            mapModified = true;
        } else {
            log.error("DOME Function {} unsupported", fun);
            return;
        }
        if (! mapModified) {
            Calculate.execute(m, calcResults, replace, destructiveMode);
        }
    }

    // private static void fill(HashMap<String, Object> m, HashMap<String, ArrayList<String>> calcResults) {
    //     log.error("Not yet implemented.");
    // }
    
    private static void execute(HashMap<String, Object> m, HashMap<String, ArrayList<String>> calcResults, boolean replace, boolean destructiveMode) {
        for (Map.Entry<String, ArrayList<String>> entry : calcResults.entrySet()) {
            String targetVariable = entry.getKey();
            String targetPath = Command.getPathOrRoot(targetVariable);
            ArrayList<String> values = entry.getValue();
            String var = targetVariable;

            if (targetPath.contains("@")) {
                // This is nested
                boolean isEvent = false;
                String eventType = "";
                if (targetPath.contains("!")) {
                    String[] tmp = targetPath.split("[@!]");
                    eventType = tmp[2];
                    isEvent = true;
                    var = AcePathfinderUtil.setEventDateVar(targetVariable, isEvent);
                }

                ArrayList<HashMap<String, Object>> pointer = traverseAndGetSiblings(m, targetVariable);
                int pointerSize = pointer.size();
                int sourceSize = values.size();

                if (destructiveMode && pointerSize == 0) {
                    log.debug("Destructive adding values for {}", targetVariable);
                    for (String value: values) {
                        AcePathfinderUtil.insertValue(m, targetVariable, value, targetPath);
                    }
                    continue;
                }

                for (int i=0; i < pointerSize; i++) {
                    String liveEvent = "";
                    Object objEvent = pointer.get(i).get("event");
                    if (objEvent != null) {
                        liveEvent = (String) objEvent;
                    }
                    if (liveEvent.equals(eventType)) {
                        log.debug("Level 1 passed, i: {} ss: {}", i, sourceSize);
                        if ( i < sourceSize ) {
                            log.debug("Level 2 passed");
                            if (replace || (!replace && !varHasValue(pointer.get(i), targetVariable, isEvent))) {
                                // Replace if only I have something for you.
                                log.debug("Level 3, writing [{}] now", var);
                                pointer.get(i).put(var, values.get(i));
                            }
                        }
                    }
                }
            } else {
                // This is not nested only need the first value.
                log.debug("targetPath is [{}]", targetPath);
                if (targetPath.equals("")) {
                    if (replace || (!replace && !varHasValue(m, targetVariable, false))) {
                        m.put(targetVariable, values.get(0));
                    }
                } else {
                    HashMap<String, Object> pointer = AcePathfinderUtil.traverseToPoint(m, targetPath);
                    if (pointer == null) {
                        log.debug("pointer not found - creating new");
                        AcePathfinderUtil.insertValue(m, targetVariable, values.get(0), targetPath);
                    } else {
                        if (replace || (!replace && !varHasValue(pointer, targetVariable, false))) {
                            pointer.put(targetVariable, values.get(0));
                        }
                    }
                }
            }
        }
    }
}