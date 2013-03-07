package org.agmip.dome;

import java.util.ArrayList;
import java.util.HashMap;

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
            String desc     = (info.get("description") == null) ? "" : info.get("description");

            String out = region+"-"+stratum+"-"+rapId+"-"+manId+"-"+rapVer+"-"+desc;
            return out.toUpperCase();
        } else {
            return "";
        }
    }

    public static HashMap<String, String> unpackDomeName(String domeName) {
        HashMap<String, String> info = new HashMap<String, String>();
        String[] parts = domeName.toUpperCase().split("[\\-]", 6);
        log.debug("Parts length: {}", parts.length);
        // for(int i=0; i < parts.length; i++) {
        //      log.debug(parts[i]);
        // }
        if (parts.length != 6 && parts.length != 5) {
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
        if (parts.length == 6 && ! parts[5].equals(""))
            info.put("description", parts[5]);

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

    public static ArrayList<HashMap<String, String>> getGenerators(HashMap<String, Object> dome) {
        if(dome.containsKey("generators")) {
            try {
                ArrayList<HashMap<String, String>> rules = (ArrayList<HashMap<String, String>>) dome.get("generators");
                if (rules == null) {
                    return new ArrayList<HashMap<String, String>>();
                }
                return rules;
            } catch (Exception ex) {
                // Could not convert
                log.error("getDomeGenerators() could not retreive the DOME generators from {}", dome.toString());
                return new ArrayList<HashMap<String, String>>();
            }
        } else {
            log.error("getDomeGenerators() could not retreive the DOME generators from {}", dome.toString());
            return new ArrayList<HashMap<String, String>>();
        }
    }

    public static boolean hasGenerators(HashMap<String, Object> dome) {
        if (dome.containsKey("generators")) {
            ArrayList<HashMap<String, String>> rules = (ArrayList<HashMap<String, String>>) dome.get("generators");
                if (rules == null) {
                    return false;
                }
                if (rules.size() == 0) {
                    return false;
                } else {
                    return true;
                }
        } else {
            return false;
        }
    }
}
