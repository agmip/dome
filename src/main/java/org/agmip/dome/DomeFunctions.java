package org.agmip.dome;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.agmip.ace.AcePathfinder;
import org.agmip.ace.util.AcePathfinderUtil;
import org.agmip.util.MapUtil;
import org.agmip.common.Functions;
import org.agmip.functions.WeatherHelper;


public class DomeFunctions {
    private static final Logger log = LoggerFactory.getLogger(DomeFunctions.class);
    private static final String MULTIPLY_DEFAULT_FACTOR="1";
    /**
     * Do not instatiate this
     */
    private DomeFunctions () {}
    
    /**
     * Wrapper around the {@code dateOffset()} method of {@code org.agmip.common.Functions}.
     *
     * @param m AgMIP simulation description
     * @param var Variable to output
     * @param base Base date to offset
     * @param offset number of days to offset
     *
     * @return dome compatable modification
     */
    public static HashMap<String, ArrayList<String>> dateOffset(HashMap m, String var, String base, String offset) {
        return offset(m, var, base, offset, true);
    }
    
    public static HashMap<String, ArrayList<String>> numericOffset(HashMap m, String var, String base, String offset) {
        return offset(m, var, base, offset, false);
    }

    private static HashMap<String, ArrayList<String>> offset(HashMap m, String targetVariable, String source, String offset, boolean isDateOffset) {
        HashMap<String, ArrayList<String>> results = new HashMap<String, ArrayList<String>>();
        ArrayList<String> toOffset = buildInputArray(m, source);
        String sourceVariable = "";
        String sourceEventType = "";
        String targetPath = AcePathfinder.INSTANCE.getPath(targetVariable);
        if (source.startsWith("$")) {
            sourceVariable = source.substring(1).toLowerCase();
        }

        if (targetPath == null) targetPath = "";

        if (targetPath.contains("@")) {
            boolean targetIsEvent = false;
            String targetEventType = "";
            if (targetPath.contains("!")) {
                targetIsEvent = true;
                String[] tmp = targetPath.split("[@!]");
                targetEventType = tmp[2];
                if (! sourceVariable.equals(targetVariable.toLowerCase())) {
                    log.error("Unable to use OFFSET methods with different event variables. [{}] [{}]", sourceVariable, targetVariable.toLowerCase());
                    return results;
                }
            }
        }

        // Actually do the offset work
        log.debug("Items to offset: {}", toOffset.toString());
        ArrayList<String> result = new ArrayList<String>();
        for (String entry: toOffset) {
            String finalValue;
            //HashMap<String, String> result = new HashMap<String, String>();
            if (isDateOffset) {
                log.debug("Calling dateOffset() with {}, {}", entry, offset);
                result.add(Functions.dateOffset(entry, offset));
            } else {
                log.debug("Calling numericOffset() with {}, {}", entry, offset);
                result.add(Functions.numericOffset(entry, offset));
            }
        }
        results.put(targetVariable, result);
        log.debug("Offset results: {}", results.toString());
        return results;
    }

    public static HashMap<String, ArrayList<String>> multiply(HashMap m, String targetVariable, String f1, String f2) {
        // Comparing two variable like this will be a pain in the butt. Safety, if f1 or f2 run out before the other
        // and both f1 and f2 sizes are > 1, then multiply the remainder by 1 (TO BE REVIEWED).
        HashMap<String, ArrayList<String>> results = new HashMap<String, ArrayList<String>>();
        ArrayList<String> factors1 = buildInputArray(m, f1);
        ArrayList<String> factors2 = buildInputArray(m, f2);
        ArrayList<String> output = new ArrayList<String>();

        log.debug("F1: {}", factors1.toString());
        log.debug("F2: {}", factors2.toString());

        int f1Size = factors1.size();

        if (f1Size < 2) {
            String fact1 = MULTIPLY_DEFAULT_FACTOR;
            if (f1Size == 1) {
                fact1 = factors1.get(0);
                if (fact1 == null) {
                    log.error("Missing factor to multiply: {}", f1);
                    return results;
                }
            }
            for (String fact2 : factors2) {
                if (fact2 == null) {
                    log.error("Missing factor to multiply: {}", f2);
                    return new HashMap<String, ArrayList<String>>();
                }
                output.add(Functions.multiply(fact1, fact2));
            }
        } else {
            int f2Size = factors2.size();
            int iter = (f1Size >= f2Size) ? f1Size : f2Size;
            for (int i=0; i < iter; i++) {
                String fact1 = MULTIPLY_DEFAULT_FACTOR;
                String fact2 = (f2Size == 1) ? factors2.get(0) : MULTIPLY_DEFAULT_FACTOR;

                if (i < f1Size) {
                    fact1 = factors1.get(i);
                    if (fact1 == null) {
                        log.error("Missing factor to multiply: {}", f1);
                        return new HashMap<String, ArrayList<String>>();
                    }
                }

                if (i < f2Size) {
                    fact2 = factors2.get(i);
                    if (fact2 == null) {
                        log.error("Missing factor to multiply: {}", f2);
                        return new HashMap<String, ArrayList<String>>();
                    }
                }
                output.add(Functions.multiply(fact1, fact2));
            }
        }
        results.put(targetVariable, output);
        log.debug("Multiply() results: {}", results.toString());
        return results;
    }

    public static HashMap<String, ArrayList<String>> percentAvailWaterContent(HashMap m, String icswp) {
        HashMap<String, ArrayList<String>> results = new HashMap<String, ArrayList<String>>();
        ArrayList<String> outputICH2O = new ArrayList<String>();
        ArrayList<String> outputICBL = new ArrayList<String>();
        // Get the soils
        ArrayList<HashMap<String, Object>> soils = Command.traverseAndGetSiblings(m, "sllb");

        if (soils.isEmpty()) {
            log.error("Missing required soil information to calculate ICH2O");
            return results;
        }
        for (HashMap<String, Object> sl : soils) {
            if (!sl.containsKey("slll") || !sl.containsKey("sldul") || !sl.containsKey("sllb")) {
                log.error("Missing SLLL, SLDUL, or SLLB. Cannot calculate ICH2O");
                return results;
            }

            BigDecimal slll; 
            BigDecimal sldul; 
            BigDecimal icswpd;
            BigDecimal sllb;
            try {
                slll = new BigDecimal(MapUtil.getValueOr(sl, "slll", "0.01"));
                sldul = new BigDecimal(MapUtil.getValueOr(sl, "sldul", "0.0"));
                sllb = new BigDecimal(MapUtil.getValueOr(sl, "sllb", "0.0"));
                icswpd = new BigDecimal(icswp);
                icswpd = icswpd.divide(new BigDecimal(100));
            } catch (Exception ex) {
                // It's all or nothing please.
                log.error("Unable to convert a string to a number for PCTAWC()");
                return new HashMap<String, ArrayList<String>>();
            }

            String icsw = sldul.subtract(slll).multiply(icswpd).add(slll).toString();
            outputICH2O.add(icsw);
            outputICBL.add(sllb.toString());
        }
        results.put("icbl", outputICBL);
        results.put("ich2o", outputICH2O);
        log.debug("PCTAWC() returned: {}", results.toString());
        return results;
    }
    
    public static HashMap<String, ArrayList<String>> getTavAndAmp(HashMap m) {
        HashMap<String, ArrayList<String>> results = new HashMap<String, ArrayList<String>>();
        // Check if the data is already been applied with the DOME function
        HashMap wthData;
        if (m.containsKey("weather") || !m.containsKey("dailyWeather")) {
            wthData = MapUtil.getObjectOr(m, "weather", new HashMap());
        } else {
            wthData = m;
        }
        String appliedDomeFuns = MapUtil.getValueOr(wthData, "applied_dome_functions", "").toUpperCase();
        if (appliedDomeFuns.contains("TAVAMP()")) {
            log.debug("Skip applying TAVAMP since it has already been applied to this weather site data.");
            return results;
        } else {
            if (appliedDomeFuns.equals("")) {
                appliedDomeFuns = "TAVAMP()";
            } else {
                appliedDomeFuns += "|TAVAMP()";
            }
        }
        
        HashMap<String, String> ret = WeatherHelper.getTavAndAmp(m);
        String tav = ret.get("tav");
        if (tav == null) {
            log.error("Failed to calculate TAV with given data set");
        } else {
            ArrayList<String> arr = new ArrayList();
            arr.add(tav);
            results.put("tav", arr);
        }
        String tamp = ret.get("tamp");
        if (tamp == null) {
            log.error("Failed to calculate TAMP with given data set");
        } else {
            ArrayList<String> arr = new ArrayList();
            arr.add(tamp);
            results.put("tamp", arr);
        }
        // Add the applied function name to the flags
        wthData.put("applied_dome_functions", appliedDomeFuns);
        return results;
    }

    public static void removeAllEvents(HashMap m) {
        log.debug("ENTERING THE FRAY!!!");
        String path = AcePathfinder.INSTANCE.getPath("pdate");
        log.debug("Looking for path ! {}", path);
        HashMap pointer = AcePathfinderUtil.traverseToPoint(m, path);
        if (pointer != null) {
            log.debug("Pointer RAE: {}", pointer.toString());
            pointer.remove("events");
        }
    }

    public static void removeAllEventsExceptCropInfo(HashMap m) {
        log.debug("ENTERING THE FRAY2!!!");
        String path = AcePathfinder.INSTANCE.getPath("pdate");
        log.debug("Looking for path ! {}", path);
        HashMap pointer = AcePathfinderUtil.traverseToPoint(m, path);
        if (pointer != null) {
            log.debug("Pointer RAE: {}", pointer.toString());
            ArrayList<HashMap<String, String>> events = new MapUtil.BucketEntry(pointer).getDataList();
            ArrayList<HashMap<String, String>> newEvents = new ArrayList<HashMap<String, String>>();
            for (HashMap<String, String> event : events) {
                if (MapUtil.getValueOr(event, "event", "").equals("planting")) {
                    HashMap<String, String> plEvent = new HashMap<String, String>();
                    plEvent.putAll(event);
                    plEvent.remove("date");
                    newEvents.add(plEvent);
                }
            }
            events.clear();
            events.addAll(newEvents);
        }
    }

    protected static class KVPair {
        private final String key;
        private final String value;

        public KVPair(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public int hashCode() {
            return 47 * (key.hashCode() + value.hashCode());
        }

        public boolean equals(Object obj) {
            if (! (obj instanceof KVPair)) return false;
            if (this == obj) return true;
            return ((this.key == ((KVPair)obj).getKey())  && (this.value == ((KVPair)obj).getValue()));
        }

        public String toString() {
            return "("+key+", "+value+")";
        }
    }

    private static ArrayList<String> buildInputArray(HashMap<String, Object> m, String source) {
        log.debug("BIA source: [{}]", source);
        String sourceVariable = "";
        String sourceEventType = "";
        ArrayList<String> results = new ArrayList<String>();
        if (source.startsWith("$")) {
            // This is a variable to offset
            sourceVariable = source.substring(1).toLowerCase();
            String sourcePath = Command.getPathOrRoot(sourceVariable);
            String sourceValue;
            boolean sourceIsEvent = false;

            if (sourcePath.contains("@")) {
                if (sourcePath.contains("!")) {
                    String[] tmp = sourcePath.split("[@!]");
                    sourceIsEvent = true;
                    sourceEventType = tmp[2];
                }

                ArrayList<HashMap<String, Object>> pointer = Command.traverseAndGetSiblings(m, sourceVariable);
                //log.debug("Current pointer [{}]: {}", sourceVariable, pointer);
                for (HashMap<String, Object> entry : pointer) {
                    if ((sourceIsEvent && (((String) entry.get("event"))).equals(sourceEventType)) || (! sourceIsEvent)){
                        String var =  AcePathfinderUtil.setEventDateVar(sourceVariable, sourceIsEvent);
                        log.debug("Looking for var {} in {}", var, entry.toString());
                        results.add((String) entry.get(var));
                    }
                }
            } else {
                sourceValue = Command.getRawValue(m, sourceVariable);
                results.add(sourceValue);
            }
        } else {
            // This is a static item to offset.
            results.add(source);
        }
        return results;
    }
}
