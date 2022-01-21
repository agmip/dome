package org.agmip.dome;

import com.rits.cloning.Cloner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.agmip.util.MapUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.agmip.util.MapUtil;

public class DomeUtil {
    private static final Logger log = LoggerFactory.getLogger(DomeUtil.class);
    private DomeUtil() {}

    public static String generateDomeName(HashMap<String, Object> dome) {
        HashMap<String, String> info;
        if (dome.containsKey("info")) {
            try {
                info = (HashMap<String, String>) dome.get("info");
            } catch (Exception ex) {
                return "";
            }

            String region   = (info.get("reg_id") == null) ? "" : info.get("reg_id");
            String stratum  = (info.get("stratum") == null) ? "" : info.get("stratum");
            String rapId    = (info.get("rap_id") == null) ? "" : info.get("rap_id");
            String manId    = (info.get("man_id") == null) ? "" : info.get("man_id");
            String rapVer   = (info.get("rap_ver") == null) ? "" : info.get("rap_ver");
            String climId   = info.get("clim_id");
            String desc     = (info.get("description") == null) ? "" : info.get("description");
            if (desc.contains("-") && climId == null) {
                climId = desc.substring(0, desc.indexOf("-"));
                desc = desc.substring(desc.indexOf("-") + 1);
            }

            String out;
            if (climId == null) {
                out = region+"-"+stratum+"-"+rapId+"-"+manId+"-"+rapVer+"--"+desc;
            } else {
                out = region+"-"+stratum+"-"+rapId+"-"+manId+"-"+rapVer+"-"+climId+"-"+desc;
            }
            return out.toUpperCase();
        } else {
            return "";
        }
    }
    
    public static void updateMetaInfo(HashMap<String, Object> dome, String domeName) {
        dome.put("info", unpackDomeName(domeName));
    }

    public static HashMap<String, String> unpackDomeName(String domeName) {
        HashMap<String, String> info = new HashMap<String, String>();
        String[] parts = domeName.toUpperCase().split("[\\-]", 7);
        log.debug("Parts length: {}", parts.length);
        // for(int i=0; i < parts.length; i++) {
        //      log.debug(parts[i]);
        // }
        if (parts.length != 6 && parts.length != 5 && parts.length != 7) {
            log.error("unpackDomeName() provided an invalid name: {}", domeName);
            return new HashMap<String, String>();
        }


        if (! parts[0].equals(""))
            info.put("reg_id",  parts[0]);
        if (! parts[1].equals(""))
            info.put("stratum", parts[1]);
        if (! parts[2].equals(""))
            info.put("rap_id",  parts[2]);
        if (! parts[3].equals(""))
            info.put("man_id", parts[3]);
        if (! parts[4].equals(""))
            info.put("rap_ver", parts[4]);
        if (parts.length <= 6) {
            if (! parts[5].equals("")) {
                info.put("description", parts[5]);
            }
        } else {
            if (parts[5] != null) {
                info.put("clim_id", parts[5]);
            }
            if (! parts[6].equals("")) {
                info.put("description", parts[6]);
            }
        }
        if (info.isEmpty()) {
            log.error("unpackDomeName() provided an invalid name: {}", domeName);
        }
        return info;
    }

    public static HashMap<String, String> getInfo(HashMap<String, Object> dome) {
        if (dome.containsKey("info")) {
            try {
                HashMap<String, String> info = (HashMap<String, String>) dome.get("info");
                if (info == null) {
                    return new HashMap<String, String>();
                }
                return info;
            } catch (Exception ex) {
                // Could not convert
                log.error("getDomeInfo() could not retreive the DOME information from {}", dome.toString());
                return new HashMap<String, String>();
            }
        } else {
            log.error("getDomeInfo() could not retreive the DOME information from {}", dome.toString());
            return new HashMap<String, String>();
        }
    }

    public static ArrayList<HashMap<String, String>> getRules(HashMap<String, Object> dome) {
        if(dome.containsKey("rules")) {
            try {
                ArrayList<HashMap<String, String>> rules = (ArrayList<HashMap<String, String>>) dome.get("rules");
                if (rules == null) {
                    return new ArrayList<HashMap<String, String>>();
                }
                return rules;
            } catch (Exception ex) {
                // Could not convert
                log.error("getDomeRules() could not retreive the DOME rules from {}", dome.toString());
                return new ArrayList<HashMap<String, String>>();
            }
        } else {
            log.error("getDomeRules() could not retreive the DOME rules from {}", dome.toString());
            return new ArrayList<HashMap<String, String>>();
        }
    }

    public static ArrayList<ArrayList<HashMap<String, String>>> getGenerators(HashMap<String, Object> dome) {
        if(dome.containsKey("generators")) {
            try {
                ArrayList<ArrayList<HashMap<String, String>>> rules = (ArrayList<ArrayList<HashMap<String, String>>>) dome.get("generators");
                if (rules == null) {
                    return new ArrayList<ArrayList<HashMap<String, String>>>();
                }
                return rules;
            } catch (Exception ex) {
                // Could not convert
                log.error("getDomeGenerators() could not retreive the DOME generators from {}", dome.toString());
                return new ArrayList<ArrayList<HashMap<String, String>>>();
            }
        } else {
            log.error("getDomeGenerators() could not retreive the DOME generators from {}", dome.toString());
            return new ArrayList<ArrayList<HashMap<String, String>>>();
        }
    }

    public static ArrayList<HashMap<String, Object>> getBatchGroup(HashMap<String, Object> dome) {
        if(dome.containsKey("batch_group")) {
            try {
                ArrayList<HashMap<String, Object>> group = (ArrayList<HashMap<String, Object>>) dome.get("batch_group");
                if (group == null) {
                    return new ArrayList<HashMap<String, Object>>();
                }
                return group;
            } catch (Exception ex) {
                // Could not convert
                log.error("getBatchGroup() could not retreive the batch DOME group from {}", dome.toString());
                return new ArrayList<HashMap<String, Object>>();
            }
        } else {
            log.error("getBatchGroup() could not retreive the batch DOME group from {}", dome.toString());
            return new ArrayList<HashMap<String, Object>>();
        }
    }

    public static ArrayList<HashMap<String, Object>> getBatchRuns(HashMap<String, Object> dome) {
        if(dome.containsKey("batch_runs")) {
            try {
                ArrayList<HashMap<String, Object>> group = (ArrayList<HashMap<String, Object>>) dome.get("batch_runs");
                if (group == null) {
                    return new ArrayList<HashMap<String, Object>>();
                }
                return group;
            } catch (Exception ex) {
                // Could not convert
                log.error("getBatchGroup() could not retreive the batch runs from {}", dome.toString());
                return new ArrayList<HashMap<String, Object>>();
            }
        } else {
            log.error("getBatchGroup() could not retreive the batch runs from {}", dome.toString());
            return new ArrayList<HashMap<String, Object>>();
        }
    }
    
    public static void insertAdjustment(HashMap<String, Object> data, HashMap<String, String> rule) {
        
        // Read the DOME rule
        String a = rule.get("args");
        if (a == null) {
            a = "";
        }
        String[] args = a.split("[|]");
        String var = rule.get("variable").toLowerCase();
        
        // Get adjust method
        String method;
        String value;
        if (!args[0].endsWith("()")) {
            method = "substitute";
            value = args[0];
        } else {
            if (args.length < 2) {
                log.warn("Arguments for Method [" + args[0] + "] is not enough to create an adjustment, this rule will be ignored");
                return;
            } else if (!args[1].equals("$" + var)) {
                log.warn("The first arguments for Method [" + args[0] + "] has to be the original variable in the batch DOME to create an adjustment, this rule will be ignored");
                return;
            }if (args[0].equals("OFFSET()")) {
                method = "delta";
                value = args[2];
            } else if (args[0].equals("MULTIPLY()")) {
                method = "multiply";
                value = args[2];
            } else {
                log.warn("Unsupported method [" + args[0] + "] found in the adjustment insertion process, this rule will be ignored");
                return;
            }
        }
        
        // Create Adjustment
        HashMap<String, String> adj = new HashMap();
        adj.put("variable", var);
        adj.put("method", method);
        adj.put("value", value);

        // Insert Agjustment
        ArrayList adjs;
        if (data.containsKey("adjustments")) {
            adjs = (ArrayList) data.get("adjustments");
        } else {
            adjs = new ArrayList();
            data.put("adjustments", adjs);
        }
        adjs.add(adj);
    }

    public static boolean hasGenerators(HashMap<String, Object> dome) {
        if (dome.containsKey("generators")) {
            ArrayList<HashMap<String, String>> rules = (ArrayList<HashMap<String, String>>) dome.get("generators");
                if (rules == null) {
                    return false;
                }
            return !rules.isEmpty();
        } else {
            return false;
        }
    }
    
    public static HashSet<String> getSWIdsSet(ArrayList<HashMap<String, Object>> arr, String... idKeys) {
        HashSet<String> ret = new HashSet();
        for (HashMap data : arr) {
            StringBuilder sb = new StringBuilder();
            for (String idKey : idKeys) {
                sb.append(MapUtil.getValueOr(data, idKey, ""));
            }
            ret.add(sb.toString());
        }
        return ret;
    }

    public static void replicateSoil(HashMap entry, HashSet soilIds, ArrayList<HashMap<String, Object>> soils) {
        String newSoilId = MapUtil.getValueOr(entry, "soil_id", "");
        HashMap data = MapUtil.getObjectOr(entry, "soil", new HashMap());
        if (data.isEmpty()) {
            return;
        }
        Cloner cloner = new Cloner();
        HashMap newData = cloner.deepClone(data);
//        ArrayList<HashMap<String, Object>> soils = MapUtil.getRawPackageContents(source, "soils");
        int count = 1;
        while (soilIds.contains(newSoilId + "_" + count)) {
            count++;
        }
        newSoilId += "_" + count;
        newData.put("soil_id", newSoilId);
        entry.put("soil_id", newSoilId);
        entry.put("soil", newData);
        soilIds.add(newSoilId);
        soils.add(newData);
    }

    public static void replicateWth(HashMap entry, HashSet wthIds, ArrayList<HashMap<String, Object>> wths) {
        String newWthId = MapUtil.getValueOr(entry, "wst_id", "");
        String climId = MapUtil.getValueOr(entry, "clim_id", "");
        HashMap data = MapUtil.getObjectOr(entry, "weather", new HashMap());
        if (data.isEmpty()) {
            return;
        }
        Cloner cloner = new Cloner();
        HashMap newData = cloner.deepClone(data);
//        ArrayList<HashMap<String, Object>> wths = MapUtil.getRawPackageContents(source, "weathers");
        String inst;
        if (newWthId.length() > 1) {
            inst = newWthId.substring(0, 2);
        } else {
            inst = newWthId + "0";
        }
        newWthId = inst + "01" + climId;
        int count = 1;
        while (wthIds.contains(newWthId) && count < 99) {
            count++;
            newWthId = String.format("%s%02d%s", inst, count, climId);
        }
        if (count == 99 && wthIds.contains(newWthId)) {
            inst = inst.substring(0, 1);
            newWthId = inst + "100" + climId;
            while (wthIds.contains(newWthId)) {
                count++;
                newWthId = String.format("%s%03d%s", inst, count, climId);
            }
        }
        newData.put("wst_id", newWthId);
        entry.put("wst_id", newWthId);
        entry.put("weather", newData);
        wthIds.add(newWthId);
        wths.add(newData);
    }
}
