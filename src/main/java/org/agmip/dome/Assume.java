package org.agmip.dome;

import java.util.ArrayList;
import java.util.HashMap;

import org.agmip.ace.AcePathfinder;
import org.agmip.ace.util.AcePathfinderUtil;
import org.agmip.util.MapUtil;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Assume extends Command {
    public static final Logger log = LoggerFactory.getLogger(Assume.class);

    public static void run(HashMap m, String var, String[] args, boolean replace) {
        var = var.toLowerCase();
        String assumedValue;
        // Set the assumedValue
        if (args[0].startsWith("$")) {
            assumedValue = Command.getRawValue(m, args[0].substring(1).toLowerCase());
        } else {
            assumedValue = args[0];
        }

        // Handle replace vs. fill
        String path = AcePathfinder.INSTANCE.getPath(var);
        if (path == null) {
            path = "";
        }
        if (replace) {
            Assume.replace(m, var, assumedValue, path);
        } else {
            Assume.fill(m, var, assumedValue, path);
        }
    }

    private static void fill(HashMap m, String var, String val, String path) {
        log.debug("Filling {} with {}", path, val);
        val = formatVal(var, val);
        if (path.contains("@")) {
            boolean isEvent = false;
            String eventType = "";
            if (path.contains("!")) {
                String[] tmp = path.split("[@!]");
                eventType = tmp[2];
                isEvent = true;
            }
            ArrayList<HashMap<String, Object>> contents = Command.traverseAndGetSiblings(m, var);
            log.debug("Pre-insert contents: {}", contents.toString());
            if (contents.size() != 0) {
                for (HashMap<String, Object> item : contents) {
                    String liveEvent = MapUtil.getValueOr(item, "event", "");
                    if ( !isEvent || (isEvent && eventType.equals(liveEvent))) {
                        if (! varHasValue(item, var, isEvent)) {
                            item.put(AcePathfinderUtil.setEventDateVar(var, isEvent), val);
                        }
                    }
                }
            } else {
                if (path.contains("!")) {
                    // There is no contents event to start with... let's create it.
                    log.debug("Creating new event in fill for {}", var);
                    AcePathfinderUtil.insertValue(m, var, val, path);
                }
            }
        } else {
            HashMap<String, Object> pointer = AcePathfinderUtil.traverseToPoint(m, path);
            if (pointer == null) {
                pointer = m;
            }
            if (! varHasValue(pointer, var, false)){
                AcePathfinderUtil.insertValue(m, var, val, path);
            }
        }
    }

    private static void replace(HashMap m, String var, String val, String path) {
        log.debug("Replacing {} with path {}", var, path);
        val = formatVal(var, val);

        if (path.contains("@")) {
            boolean isEvent = false;
            String eventType = "";
            if (path.contains("!")) {
                isEvent = true;
                String[] tmp = path.split("[@!]");
                eventType = tmp[2];
            }
            ArrayList<HashMap<String, Object>> contents = traverseAndGetSiblings(m, var);
            if (contents.size() == 0) {
                AcePathfinderUtil.insertValue(m, var, val, path);
            } else {
                boolean replaced = false;
                for (HashMap<String, Object> item: contents) {
                    if ((isEvent && MapUtil.getValueOr(item, "event", "").equals(eventType)) || !isEvent) {
                        replaced = true;
                        item.put(AcePathfinderUtil.setEventDateVar(var, isEvent), val);
                    }
                }
                if (! replaced) {
                    AcePathfinderUtil.insertValue(m, var, val, path);
                }
            }
            log.debug("Current contents: {}", contents.toString());
        } else {
            HashMap<String, Object> pointer = AcePathfinderUtil.traverseToPoint(m, path);
            if (pointer == null) {
                log.debug("pointer not found - creating new");
                AcePathfinderUtil.insertValue(m, var, val, path);
            } else {
                pointer.put(var, val);
            }
        }
    }
    
    private static String formatVal(String var, String val) {
        if (val == null) {
            return val;
        }
        String ret = val;
        if (AcePathfinderUtil.isDate(var)) {
            ret = val.replaceAll("[-/:]", "");
            if (!ret.matches("\\d{8,8}")) {
                log.error("Found unsupported date format: {} for {}", val, var);
                ret = val;
            }
        }
        return ret;
    }
}
