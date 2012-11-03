package org.agmip.dome;

import java.util.ArrayList;
import java.util.HashMap;

import org.agmip.ace.AcePathfinder;
import org.agmip.ace.util.AcePathfinderUtil;

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
        if (path.contains("@")) {
            boolean isEvent = false;
            if (path.contains("!")) {
                isEvent = true;
            }
            ArrayList<HashMap<String, Object>> contents = Command.traverseAndGetSiblings(m, var);
            if (contents.size() != 0) {
                for (HashMap<String, Object> item : contents) {
                    if (! varHasValue(item, var, isEvent)) {
                        item.put(var, val);
                    }
                }
            } else {
                if (path.contains("!")) {
                    // There is no contents event to start with... let's create it.
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
        log.debug("Current data: {}", m.toString());
        log.debug("Replacing {} with path {}", var, path);

        if (path.contains("@")) {
            boolean isEvent = false;
            if (path.contains("!")) {
                isEvent = true;
            }
            ArrayList<HashMap<String, Object>> contents = traverseAndGetSiblings(m, var);
            if (contents.size() == 0) {
                AcePathfinderUtil.insertValue(m, var, val, path);
            } else {
                for (HashMap<String, Object> item: contents) {
                    item.put(AcePathfinderUtil.setEventDateVar(var, isEvent), val);
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
}
