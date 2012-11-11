package org.agmip.dome;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.agmip.util.MapUtil;

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

            String region   = MapUtil.getValueOr(info, "region", "");
            String stratum  = MapUtil.getValueOr(info, "stratum", "");
            String rapId    = MapUtil.getValueOr(info, "rap_id", "");
            String manId    = MapUtil.getValueOr(info, "man_id", "");
            String rapVer   = MapUtil.getValueOr(info, "rap_ver", "");
            // Minimun length is 4

            return region+"-"+stratum+"-"+rapId+"-"+manId+"-"+rapVer;
        } else {
            return "";
        }
    }

    public static HashMap<String, String> unpackDomeName(String domeName) {
        HashMap<String, String> info = new HashMap<String, String>();
        String[] parts = domeName.split("\\-");
        log.debug("{}", parts);
        log.debug("{}", parts.length);
        if (parts.length != 4 && parts.length != 5) {
            log.error("unpackDomeName() provided an invalid name: {}", domeName);
            return new HashMap<String, String>();
        }

        for(int i=0; i < parts.length; i++) {
            log.debug(parts[i]);
        }
        if (! parts[0].equals(""))
            info.put("region",  parts[0]);
        if (! parts[1].equals(""))
            info.put("stratum", parts[1]);
        if (! parts[2].equals(""))
            info.put("rap_id",  parts[2]);
        if (! parts[3].equals(""))
            info.put("man_id",  parts[3]);
        if (parts.length == 5) {
            info.put("rap_ver", parts[4]);
        }
        return info;
    }

    public static HashMap<String, String> getDomeInfo(HashMap<String, Object> dome) {
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

    public static ArrayList<HashMap<String, String>> getDomeRules(HashMap<String, Object> dome) {
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
}