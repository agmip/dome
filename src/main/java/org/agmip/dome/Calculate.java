package org.agmip.dome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.agmip.ace.AcePathfinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.agmip.ace.util.AcePathfinderUtil;
import org.agmip.common.Functions;

// Temporary imports to integrate @MengZhang codebase from another project
import org.agmip.functions.ExperimentHelper;
import org.agmip.functions.SoilHelper;
import org.agmip.functions.WeatherHelper;
import org.agmip.util.MapUtil;


public class Calculate extends Command {
    public static final Logger log = LoggerFactory.getLogger(Calculate.class);

    public static void run(HashMap m, String var, String[] args, boolean replace) {
        var = var.toLowerCase();
        String fun = args[0].toUpperCase();
        String[] newArgs =  Arrays.copyOfRange(args, 1, args.length);
        HashMap<String, ArrayList<String>> calcResults = null;
        boolean mapModified = false;
        boolean destructiveMode = false;

	log.debug("Attempting to apply DOME function: {}", fun);
        // These functions use the proper modifcation protocols.
        if (fun.equals("OFFSET_DATE()") || fun.equals("DATE_OFFSET()")) {
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
        } else if (fun.equals("TRANSPOSE()")) {
            if (newArgs.length < 1) {
                log.error("Not enough arguments for {}", fun);
                return;
            } else {
                ArrayList<String> inputArr = new ArrayList();
                inputArr.addAll(Arrays.asList(newArgs));
                calcResults = new HashMap();
                calcResults.put(var, inputArr);
            }
        }  else if (fun.equals("PCTAWC()")) {
            if (newArgs.length != 1) {
                log.error("Invalid number of arguments for {}", fun);
                return;
            } else {
                destructiveMode = true;
                calcResults = DomeFunctions.percentAvailWaterContent(m, newArgs[0]);
            }
        // These functions modify the map directly.
        // } else if (fun.equals("AUTO_PDATE()")) {
        //     if (newArgs.length < 4) {
        //         log.error("Not enough arguments for {}", fun);
        //         return;
        //     }
        //     calcResults = ExperimentHelper.getAutoPlantingDate(newArgs[0], newArgs[1], newArgs[2], newArgs[3], m);
        //     //mapModified = true;
        } else if (fun.equals("TAVAMP()")) {
            if (newArgs.length != 0) {
                log.warn("Too many arguments for {}", fun);
            }
            calcResults = DomeFunctions.getTavAndAmp(m);
        } else if (fun.equals("REFET()")) {
            if (newArgs.length != 0) {
                log.warn("Too many arguments for {}", fun);
            }
            calcResults = WeatherHelper.getEto(m);
        } else if (fun.equals("ICN_DIST()")) {
            if (newArgs.length < 1) {
                log.error("Not enough arguments for {}", fun);
                return;
            }
            calcResults = SoilHelper.getIcnDistribution(m, newArgs[0]);
        } else if (fun.equals("FERT_DIST()")) {
            if (newArgs.length < 6) {
                log.error("Not enough arguments for {}", fun);
                return;
            } else {
                int numOfApplications = Functions.numericStringToBigInteger(newArgs[0]).intValue();
                if (newArgs.length < ((numOfApplications*2)+4)) {
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
                    ArrayList<HashMap<String, String>> feEvents = ExperimentHelper.getFertDistribution(m, newArgs[0], newArgs[1], newArgs[2], newArgs[3], offsetArr, pctArr);
                    ArrayList<HashMap<String, String>> events = MapUtil.getBucket(m, "management").getDataList();
                    events.addAll(feEvents);
                    mapModified = true;
                }
            }
        } else if (fun.equals("OM_DIST()")) {
            if (newArgs.length < 6) {
                log.error("Not enough arguments for {}", fun);
                return;
            }
            ArrayList<HashMap<String, String>> newEvents = ExperimentHelper.getOMDistribution(m, newArgs[0], newArgs[1], newArgs[2], newArgs[3], newArgs[4], newArgs[5]);
            ArrayList<HashMap<String, String>> events = MapUtil.getBucket(m, "management").getDataList();
//            events.clear();
            events.addAll(newEvents);
            mapModified = true;
        } else if (fun.equals("ROOT_DIST()")) {
//            HashMap soilData = MapUtil.getObjectOr(m, "soil", new HashMap());
//            String appliedDomeFuns = MapUtil.getValueOr(soilData, "applied_dome_functions", "").toUpperCase();
//            if (appliedDomeFuns.contains("ROOT_DIST()")) {
//                log.debug("Skip applying ROOT_DIST since it has already been applied to this soil site data.");
//                mapModified = true;
//            } else {
            if (newArgs.length < 3) {
                log.error("Not enough arguments for {}", fun);
                return;
            }
            calcResults = SoilHelper.getRootDistribution(m, var, newArgs[0], newArgs[1], newArgs[2]);
//                if (appliedDomeFuns.equals("")) {
//                    appliedDomeFuns = "ROOT_DIST()";
//                } else {
//                    appliedDomeFuns += "|ROOT_DIST()";
//                }
//                soilData.put("applied_dome_functions", appliedDomeFuns);
//            }
//            mapModified = true;
        } else if (fun.equals("STABLEC()")) {
            HashMap soilData = MapUtil.getObjectOr(m, "soil", new HashMap());
            String appliedDomeFuns = MapUtil.getValueOr(soilData, "applied_dome_functions", "").toUpperCase();
            if (appliedDomeFuns.contains("STABLEC()")) {
                log.debug("Skip applying STABLEC since it has already been applied to this soil site data.");
                mapModified = true;
            } else {
                if (newArgs.length < 3) {
                    log.error("Not enough arguments for {}", fun);
                    return;
                }
                calcResults = ExperimentHelper.getStableCDistribution(m, newArgs[0], newArgs[1], newArgs[2]);
                if (appliedDomeFuns.equals("")) {
                    appliedDomeFuns = "STABLEC()";
                } else {
                    appliedDomeFuns += "|STABLEC()";
                }
                soilData.put("applied_dome_functions", appliedDomeFuns);
            }
//            mapModified = true;
        } else if (fun.equals("REMOVE_ALL_EVENTS()")) {
            if (! replace) {
                log.error("Cannot remove all events from a FILL command");
                return;
            } else {
                DomeFunctions.removeAllEventsExceptCropInfo(m);
                mapModified = true;
            }
        } else if (fun.equals("AUTO_PDATE()")) {
            if (newArgs.length < 4) {
                log.error("Not enough arguments for {}", fun);
                return;
            }
            calcResults = ExperimentHelper.getAutoFillPlantingDate(m, newArgs[0], newArgs[1], newArgs[2], newArgs[3]);
        } else if (fun.equals("PADDY()")) {
            if (newArgs.length < 6) {
                log.error("Not enough arguments for {}", fun);
                return;
            } else {
                int numOfApplications = Functions.numericStringToBigInteger(newArgs[0]).intValue();
                int reqNum = numOfApplications * 3 + 3;
                if (newArgs.length < reqNum) {
                    log.error("Not enough arguments for {}", fun);
                    return;
                } else {
                    ArrayList<String> offset = new ArrayList<String>();
                    ArrayList<String> maxVal = new ArrayList<String>();
                    ArrayList<String> minVal = new ArrayList<String>();
                    if (newArgs.length > reqNum) {
                        log.warn("Too many arguments for {}, will only apply first {} group of bund information", fun, numOfApplications);
                    }
                    for (int i = 3; i < reqNum ; i++) {
                        if (i % 3 == 0) {
                            offset.add(newArgs[i]);
                        } else if (i % 3 == 1) {
                            maxVal.add(newArgs[i]);
                        } else {
                            minVal.add(newArgs[i]);
                        }
                    }
                    log.debug("Calling with offset: {}, max: {} and min: {}", offset, maxVal, minVal);

                    String [] offsetArr = offset.toArray(new String[offset.size()]);
                    String [] maxArr = maxVal.toArray(new String[maxVal.size()]);
                    String [] minArr = minVal.toArray(new String[minVal.size()]);
                    
                    ArrayList<HashMap<String, String>> irEvents = ExperimentHelper.getPaddyIrrigation(m, newArgs[0], newArgs[1], newArgs[2], offsetArr, maxArr, minArr);
                    ArrayList<HashMap<String, String>> events = MapUtil.getBucket(m, "management").getDataList();
                    events.addAll(irEvents);
                    mapModified = true;
                }
            } 
        } else if (fun.equals("AUTO_IDATE()")) {
            if (newArgs.length < 4) {
                log.error("Not enough arguments for {}", fun);
                return;
            } else {
                int numOfApplications = Functions.numericStringToBigInteger(newArgs[0]).intValue();
                int reqNum = numOfApplications * 2 + 2;
                if (newArgs.length < reqNum) {
                    log.error("Not enough arguments for {}", fun);
                    return;
                } else {
                    ArrayList<String> gddArr = new ArrayList<String>();
                    ArrayList<String> irvalArr = new ArrayList<String>();
                    if (newArgs.length > reqNum) {
                        log.warn("Too many arguments for {}, will only apply first {} group of irrigation information", fun, numOfApplications);
                    }
                    for (int i = 2; i < reqNum ; i++) {
                        if (i % 2 == 0) {
                            gddArr.add(newArgs[i]);
                        } else {
                            irvalArr.add(newArgs[i]);
                        }
                    }
                    log.debug("Calling with GDD: {} and IRVAL: {}", gddArr, irvalArr);

                    String [] maxArr = gddArr.toArray(new String[gddArr.size()]);
                    String [] minArr = irvalArr.toArray(new String[irvalArr.size()]);
                    
                    ArrayList<HashMap<String, String>> irEvents = ExperimentHelper.getAutoIrrigationEvent(m, newArgs[0], newArgs[1], maxArr, minArr);
                    ArrayList<HashMap<String, String>> events = MapUtil.getBucket(m, "management").getDataList();
                    if (replace) {
                        ArrayList<HashMap<String, String>> newEvents = new ArrayList();
                        for (HashMap event : events) {
                            if (!"irrigation".equals(MapUtil.getValueOr(event, "event", ""))) {
                                newEvents.add(event);
                            }
                        }
                        if (newEvents.size() != events.size()) {
                            events.clear();
                            events.addAll(newEvents);
                        }
                    }
                    events.addAll(irEvents);
                    mapModified = true;
                }
            }
        } else if (fun.equals("LYRSET()")) {
            String path = AcePathfinder.INSTANCE.getPath(var);
            boolean isICLayers = false;
            if (path == null || !path.contains("soilLayer")) {
                log.warn("LYRSET() could not work for {}, please try SLLB or ICBL instead", var);
                return;
            } else if (path.contains("initial_conditions")) {
                isICLayers = true;
            }
            int topLyrNum = 2;
            String[] lyrThk = {"10", "10"};
            if (newArgs.length > topLyrNum) {
                log.warn("Too many top layer thickness for {}, will only apply first {} layer thickness", fun, topLyrNum);
            }
            for (int i = 0; i < lyrThk.length && i < newArgs.length; i++) {
                lyrThk[i] = newArgs[i];
            }
            HashMap soilData;
            if (!isICLayers) {
                soilData = MapUtil.getObjectOr(m, "soil", new HashMap());
                String appliedDomeFuns = MapUtil.getValueOr(soilData, "applied_dome_functions", "").toUpperCase();
                if (appliedDomeFuns.contains("LYRSET()")) {
                    log.debug("Skip applying LYRSET since it has already been applied to this soil site data.");
                    return;
                } else {
                    if (appliedDomeFuns.equals("")) {
                        appliedDomeFuns = "LYRSET()";
                    } else {
                        appliedDomeFuns += "|LYRSET()";
                    }
                    soilData.put("applied_dome_functions", appliedDomeFuns);
                }
            } else {
                soilData = MapUtil.getObjectOr(m, "initial_conditions", new HashMap());
            }
            if (newArgs.length != 0) {
                log.warn("Too many arguments for {}", fun);
            }
            ArrayList<HashMap<String, String>> newLayers = SoilHelper.splittingSoillayer(m, isICLayers, lyrThk[0], lyrThk[1]);
            ArrayList<HashMap<String, String>> layers = new MapUtil.BucketEntry(soilData).getDataList();
            if (newLayers.size() > layers.size()) {
//                    layers.clear();
//                    layers.addAll(newLayers);
                soilData.put("soilLayer", newLayers);
            }
            mapModified = true;
        } else if (fun.equals("PTCALC()")) {
//            HashMap soilData = MapUtil.getObjectOr(m, "soil", new HashMap());
//            String appliedDomeFuns = MapUtil.getValueOr(soilData, "applied_dome_functions", "").toUpperCase();
//            if (appliedDomeFuns.contains("PTCALC()")) {
//                log.debug("Skip applying PTCALC since it has already been applied to this soil site data.");
//                mapModified = true;
//            } else {
            if (newArgs.length < 2) {
                log.error("Not enough arguments for {}", fun);
                return;
            }
            ArrayList vars = new ArrayList();
            for (int i = 1; i < newArgs.length; i++) {
                vars.add(newArgs[i].toLowerCase());
            }
            calcResults = SoilHelper.getSoilValsFromOthPara(m, newArgs[0], vars);
//                if (appliedDomeFuns.equals("")) {
//                    appliedDomeFuns = "PTCALC()";
//                } else {
//                    appliedDomeFuns += "|PTCALC()";
//                }
//                soilData.put("applied_dome_functions", appliedDomeFuns);
//            }
//            mapModified = true;
        } else if (fun.equals("SHIFT_EVENTS()")) {
            if (newArgs.length < 2) {
                log.error("Not enough arguments for {}", fun);
                return;
            } else if (newArgs.length > 2) {
                log.warn("Too much arguments for {}", fun);
            }
            String shiftType = newArgs[0];
            String days;
            if ("ABSOLUTE".equalsIgnoreCase(shiftType)) {
                String pdate = ExperimentHelper.getFstPdate(m, "");
                if (pdate.equals("")) {
                    log.error("Can not find original PDATE for {}", fun);
                    return;
                }
                days = Functions.calcDAP(newArgs[1], pdate);
            } else if ("RELATIVE".equalsIgnoreCase(shiftType)) {
                days = newArgs[1];
            } else {
                log.error("Unrecognized shift type for {}", fun);
                return;
            }
            ExperimentHelper.shiftEvents(m, days);
            mapModified = true;
        } else if (fun.equals("CTWN_FUN()")) {
            if (newArgs.length < 1) {
                log.error("Not enough arguments for {}", fun);
                return;
            } else if (newArgs.length > 6) {
                log.warn("Too much arguments for {}", fun);
            }
            String[] newArgs2 = {"", "", "", "", "", ""};
            for (int i = 0; i < newArgs.length; i++) {
                newArgs2[i] = newArgs[i];
            }
            ExperimentHelper.setCTWNAdjustments(m, newArgs2[0], newArgs2[1], newArgs2[2], newArgs2[3], newArgs2[4], newArgs2[5]);
            mapModified = true;
        } else if (fun.equals("CLIM_CO2()")) {
            if (newArgs.length < 1) {
                log.error("Not enough arguments for {}", fun);
                return;
            } else if (newArgs.length > 2) {
                log.warn("Too much arguments for {}", fun);
            }
            String[] newArgs2 = {"", "", "", "", "", newArgs[0]};
            if (newArgs.length > 1) {
                newArgs2[0] = newArgs[1];
            }
            ExperimentHelper.setCTWNAdjustments(m, newArgs2[0], newArgs2[1], newArgs2[2], newArgs2[3], newArgs2[4], newArgs2[5]);
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
    public static void create(HashMap m, String var, String[] args) {
        var = var.toLowerCase();
        String fun = args[0].toUpperCase();
        String[] newArgs =  Arrays.copyOfRange(args, 1, args.length);

	log.debug("Attempting to apply DOME function: {}", fun);
        if (fun.equals("NEW_EVENT()")) {
            
            if (newArgs.length < 1) {
                log.error("Not enough arguments for {}", fun);
            } else if (newArgs.length % 2 != 1) {
                log.warn("There is unpaired variable for {}", fun);
            }
            HashMap<String, String> info  = new HashMap<String, String>();
            for (int i = 2; i < newArgs.length; i += 2) {
                info.put(newArgs[i -1].toLowerCase(), newArgs[i]);
            }
            HashMap<String, String> newEvent = ExperimentHelper.createEvent(m, var, newArgs[0], info, true);
            if (!newEvent.isEmpty()) {
                MapUtil.getBucket(m, "management").getDataList().add(newEvent);
            } else {
                log.warn("No event has been generated");
            }
        } else {
            log.error("DOME Function {} unsupported", fun);
        }
    }
    private static void execute(HashMap<String, Object> m, HashMap<String, ArrayList<String>> calcResults, boolean replace, boolean destructiveMode) {
        log.debug("Executing with: {}", calcResults);
        if (calcResults.isEmpty()) {return;}

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
                log.debug("CALC EXECUTE() pointer: {}", pointer);
                int pointerSize = pointer.size();
                int sourceSize = values.size();

                if (destructiveMode && pointerSize == 0) {
                    log.debug("Destructive adding values for {}", targetVariable);
                    for (String value: values) {
                        AcePathfinderUtil.insertValue(m, targetVariable, value, targetPath);
                    }
                    continue;
                }

                for (int i=0, j=0; i < pointerSize; i++) {
                    String liveEvent = "";
                    Object objEvent = pointer.get(i).get("event");
                    if (objEvent != null) {
                        liveEvent = (String) objEvent;
                    }
                    if (liveEvent.equals(eventType)) {
                        log.debug("Level 1 passed, i: {} ss: {}", i, sourceSize);
                        if ( j < sourceSize ) {
                            log.debug("Level 2 passed");
                            if (replace || (!replace && !varHasValue(pointer.get(i), targetVariable, isEvent))) {
                                // Replace if only I have something for you.
                                log.debug("Level 3, writing [{}] now", var);
                                pointer.get(i).put(var, values.get(j));
                                if (isEvent) {
                                    j++;
                                }
                            }
                        }
                    }
                    if (!isEvent) {
                        j++;
                    }
                }
            } else {
                // This is not nested only need the first value.
                log.debug("targetPath is [{}]", targetPath);
                if (values.isEmpty()) {
                    continue;
                }
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
