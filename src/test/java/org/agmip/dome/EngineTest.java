package org.agmip.dome;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import org.agmip.ace.AcePathfinder;

import org.agmip.ace.util.AcePathfinderUtil;
import org.agmip.util.JSONAdapter;
import org.agmip.util.MapUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineTest {
    private static final Logger log = LoggerFactory.getLogger(EngineTest.class);
    private ArrayList<HashMap<String, String>> rules;
    private Engine e;

    private void createRule(String method, String var, String args) {
        createEngineRule(e, method, var, args, false);
    }

    private void createEngineRule(Engine eng, String method, String var, String args, boolean isGenerator) {
        HashMap<String, String> rule = new HashMap<String, String>();
        rule.put("cmd", method);
        if (var != null) {
            rule.put("variable", var);
        }
        if (args != null) {
            rule.put("args", args);
        }
        if (isGenerator) {
            eng.addGenerator(rule);
        } else {
            eng.addRule(rule);
        }
    }
    
    private HashMap<String, String> createRule2(String method, String var, String args) {
        HashMap<String, String> rule = new HashMap<String, String>();
        rule.put("cmd", method);
        if (var != null) {
            rule.put("variable", var);
        }
        if (args != null) {
            rule.put("args", args);
        }
        return rule;
    }





    @Before
    public void startUp() {
        rules = new ArrayList<HashMap<String,String>>();
        // This should only be used within the confines of a test.
        e = new Engine();
    }

    @Test
    public void CommandTest() {
        createRule("NOTHING", null, null);
        createRule("INFO", null, null);
        createRule("REPLACE", null, null);
        createRule("REPLACE", "icdat", "OFFSET()|$PDATE|-30");

        // We are just testing commands, no data required.
        e.apply(new HashMap<String, Object>());
    }

    @Test
    public void StaticFillTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();

        createRule("FILL", "exname", "Sample");
        createRule("FILL", "pdate", "20121024");
        createRule("FILL", "icdat", "$pdate");
        createRule("FILL", "APSIM_CULID", "sb120");
        e.apply(testMap);

        log.info("SFNnT: {}", testMap.toString());
    }

    @Test
    public void StaticFillBlanksTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "exname", "StaticFillBlanks");
        AcePathfinderUtil.insertValue(testMap, "ic_name", "StaticFillBlankIC");
        AcePathfinderUtil.insertValue(testMap, "pdate", "19810101");

        log.info("SFBT Pre: {}", testMap.toString());

        createRule("FILL", "exname", "Failed");
        createRule("FILL", "exp_dur", "1");
        createRule("FILL", "ic_name", "Failed");
        createRule("FILL", "icdat", "$pdate");
        createRule("FILL", "pdate", "99999999");
        createRule("FILL", "crid", "MAZ");
        
        e.apply(testMap);

        log.info("SFBT Post: {}", testMap.toString());
    }

    @Test
    public void StaticReplaceNonexistentTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();

        createRule("REPLACE", "exname", "Root");
        createRule("REPLACE", "icdat", "19810101");
        createRule("REPLACE", "pdate", "19840219");
        e.apply(testMap);
        log.info("SRNT: {}", testMap.toString());
    }

    @Test
    public void StaticReplaceActualTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "exname", "StaticFillBlanks");
        AcePathfinderUtil.insertValue(testMap, "ic_name", "StaticFillBlankIC");
        AcePathfinderUtil.insertValue(testMap, "pdate", "19810101");
        AcePathfinderUtil.insertValue(testMap, "crid", "MAZ");
        AcePathfinderUtil.insertValue(testMap, "pdate", "19840101");
        AcePathfinderUtil.insertValue(testMap, "crid", "SUC");

        log.info("SRAT Pre: {}", testMap.toString());

        createRule("REPLACE", "exname", "StaticReplaceActual");
        createRule("REPLACE", "ic_name", "StaticReplaceActualIC");
        createRule("REPLACE", "crid", "WHB");
        e.apply(testMap);

        log.info("SRAT Post: {}", testMap.toString());
    }

    @Test
    public void CalcReplaceBlankICDATFromEvent() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "pdate", "19810101");
        AcePathfinderUtil.insertValue(testMap, "pdate", "19820101");

        createRule("REPLACE", "icdat", "OFFSET_DATE()|$pdate|-7");
        e.apply(testMap);

        log.info("CRBIFE: {}", testMap.toString());
    }

    @Test
    public void CalcReplaceExistingICDATFromEvent() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "pdate", "19810101");
        AcePathfinderUtil.insertValue(testMap, "icdat", "19800101");

        createRule("REPLACE", "icdat", "OFFSET_DATE()|$pdate|-7");
        e.apply(testMap);

        log.info("CREIFE: {}", testMap.toString());
    }

    @Test
    public void CalcReplaceExistingPdateFromEvent() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "pdate", "19810101");
        AcePathfinderUtil.insertValue(testMap, "pdate", "19820101");
        AcePathfinderUtil.insertValue(testMap, "pdate", "19830101");
        AcePathfinderUtil.insertValue(testMap, "sdat", "19800101", "");

        log.info("==== START ====\nStarting Map: {}", testMap.toString());
        createRule("REPLACE", "pdate", "OFFSET_DATE()|$pdate|-7");
        createRule("FILL", "icdat", "OFFSET_DATE()|$pdate|-30");
        createRule("FILL", "sdat", "OFFSET_DATE()|$icdat|-30");
        e.apply(testMap);

        log.info("CREPDE: {}", testMap.toString());
    }

    @Test
    public void CalcReplaceMultiplyTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "icbl", "20");
        AcePathfinderUtil.insertValue(testMap, "icbl", "30");
        AcePathfinderUtil.insertValue(testMap, "sllb", "10");
        AcePathfinderUtil.insertValue(testMap, "sllb", "15");

        createRule("REPLACE", "sllb", "MULTIPLY()|$sllb|2");
        e.apply(testMap);

        log.info("CRMT: {}", testMap.toString());
    }

    @Test
    public void CalcFillICH2OTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "sllb", "30");
        AcePathfinderUtil.insertValue(testMap, "slll", "0.22");
        AcePathfinderUtil.insertValue(testMap, "sldul", "0.32");
        AcePathfinderUtil.insertValue(testMap, "sloc", "0.9");
        AcePathfinderUtil.insertValue(testMap, "sllb", "50");
        AcePathfinderUtil.insertValue(testMap, "slll", "0.15");
        AcePathfinderUtil.insertValue(testMap, "sldul", "0.24");
        AcePathfinderUtil.insertValue(testMap, "sloc", "0.01");

        createRule("FILL", "ich2o", "PCTAWC()|45");
        e.apply(testMap);

        log.info("CFIh2oT: {}", testMap.toString());
    }

    @Test
    public void CalcFillFullICH2OTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "sllb", "30");
        AcePathfinderUtil.insertValue(testMap, "slll", "0.22");
        AcePathfinderUtil.insertValue(testMap, "sldul", "0.32");
        AcePathfinderUtil.insertValue(testMap, "sloc", "0.9");
        AcePathfinderUtil.insertValue(testMap, "sllb", "50");
        AcePathfinderUtil.insertValue(testMap, "slll", "0.15");
        AcePathfinderUtil.insertValue(testMap, "sldul", "0.24");
        AcePathfinderUtil.insertValue(testMap, "sloc", "0.01");
        AcePathfinderUtil.insertValue(testMap, "icbl", "25");
        AcePathfinderUtil.insertValue(testMap, "icbl", "30");

        createRule("FILL", "ich2o", "PCTAWC()|45");
        e.apply(testMap);

        log.info("CRIh2oT: {}", testMap.toString());
    }

    @Test
    public void CalcReplaceFullICH2OTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "sllb", "30");
        AcePathfinderUtil.insertValue(testMap, "slll", "0.22");
        AcePathfinderUtil.insertValue(testMap, "sldul", "0.32");
        AcePathfinderUtil.insertValue(testMap, "sloc", "0.9");
        AcePathfinderUtil.insertValue(testMap, "sllb", "50");
        AcePathfinderUtil.insertValue(testMap, "slll", "0.15");
        AcePathfinderUtil.insertValue(testMap, "sldul", "0.24");
        AcePathfinderUtil.insertValue(testMap, "sloc", "0.01");
        AcePathfinderUtil.insertValue(testMap, "icbl", "25");
        AcePathfinderUtil.insertValue(testMap, "icbl", "30");

        createRule("REPLACE", "ich2o", "PCTAWC()|45");
        e.apply(testMap);

        log.info("CRIh2oT: {}", testMap.toString());
    }

    @Test
    public void CalcFEDistTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "pdate", "19820312");
        AcePathfinderUtil.insertValue(testMap, "fen_tot", "60");
        log.info("=== FERT_DIST() TEST ===");
        log.debug("Starting map: {}", testMap);
        createRule("REPLACE", "fedate", "FERT_DIST()|2|FE005|AP002|10|14|33.3|45|66.7");
        e.apply(testMap);
        log.debug("Modified map: {}", testMap.toString());
        log.info("=== END TEST ===");
    }

    @Test
    public void CalcOMDistTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "pdate", "19820312");
        AcePathfinderUtil.insertValue(testMap, "om_tot", "1000");
        log.info("=== OM_DIST() TEST ===");
        log.debug("Starting map: {}", testMap);
        createRule("FILL", "omdat", "OM_DIST()|-7|RE003|8.3|5|50|2.5");
        e.apply(testMap);
        log.debug("Modified Map: {}", testMap.toString());
        log.info("=== END TEST ===");
    }

    @Test
    public void CalcRootDistTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "sllb", "5");
        AcePathfinderUtil.insertValue(testMap, "sllb", "15");
        AcePathfinderUtil.insertValue(testMap, "sllb", "30");
        AcePathfinderUtil.insertValue(testMap, "sllb", "60");
        AcePathfinderUtil.insertValue(testMap, "sllb", "90");
        AcePathfinderUtil.insertValue(testMap, "sllb", "120");
        AcePathfinderUtil.insertValue(testMap, "sllb", "150");
        AcePathfinderUtil.insertValue(testMap, "sllb", "180");

        log.info("=== ROOT_DIST() TEST ===");
        log.info("Starting map: {}", testMap.toString());
        createRule("FILL", "sloc", "ROOT_DIST()|1.0|20|180");
        e.apply(testMap);
        log.info("Modified Map: {}", testMap.toString());
        log.info("=== END TEST ===");
    }

    @Test
    public void CalcStableCTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "icbl", "5");
        AcePathfinderUtil.insertValue(testMap, "icbl", "15");
        AcePathfinderUtil.insertValue(testMap, "icbl", "30");
        AcePathfinderUtil.insertValue(testMap, "icbl", "60");
        AcePathfinderUtil.insertValue(testMap, "icbl", "90");
        AcePathfinderUtil.insertValue(testMap, "icbl", "120");
        AcePathfinderUtil.insertValue(testMap, "icbl", "150");
        AcePathfinderUtil.insertValue(testMap, "icbl", "180");
        AcePathfinderUtil.insertValue(testMap, "sllb", "5");
        AcePathfinderUtil.insertValue(testMap, "sloc", "2.00");
        AcePathfinderUtil.insertValue(testMap, "sllb", "15");
        AcePathfinderUtil.insertValue(testMap, "sloc", "1.00");
        AcePathfinderUtil.insertValue(testMap, "sllb", "30");
        AcePathfinderUtil.insertValue(testMap, "sloc", "1.00");
        AcePathfinderUtil.insertValue(testMap, "sllb", "60");
        AcePathfinderUtil.insertValue(testMap, "sloc", "0.50");
        AcePathfinderUtil.insertValue(testMap, "sllb", "90");
        AcePathfinderUtil.insertValue(testMap, "sloc", "0.10");
        AcePathfinderUtil.insertValue(testMap, "sllb", "120");
        AcePathfinderUtil.insertValue(testMap, "sloc", "0.10");
        AcePathfinderUtil.insertValue(testMap, "sllb", "150");
        AcePathfinderUtil.insertValue(testMap, "sloc", "0.04");
        AcePathfinderUtil.insertValue(testMap, "sllb", "180");
        AcePathfinderUtil.insertValue(testMap, "sloc", "0.24");

        log.info("=== STABLEC() TEST ===");
        log.info("Starting map: {}", testMap.toString());
        createRule("FILL", "slsc", "STABLEC()|0.55|20|60");
        e.apply(testMap);
        log.info("Modified Map: {}", testMap.toString());
        log.info("=== END TEST ===");
    }

    @Test
    @Ignore
    public void FillAutoPdateTest() {
        
        log.info("=== FILL AUTO_PDATE() TEST ===");
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        URL resource = this.getClass().getResource("/mach_fast.json");
        String json = "";
        try {
            json = new Scanner(new File(resource.getPath()), "UTF-8").useDelimiter("\\A").next();
        } catch (Exception ex) {
            log.error("Unable to find mach_fast.json");
            assertTrue(false);
        }
        try {
            testMap = JSONAdapter.fromJSON(json);
        } catch (Exception ex) {
            log.error("Unable to convert JSON");
            assertTrue(false);
        }
        ArrayList<HashMap<String, Object>> fp = MapUtil.flatPack(testMap);
        log.debug("Flatpack count: {}", fp.size());
        createRule("FILL", "pdate", "AUTO_PDATE()|0501|0701|25|5");
        HashMap<String, Object> tm;
        ArrayList<HashMap<String, String>> events;
        
        // Case 1 PDATE is missing
        tm = fp.get(0);
//        AcePathfinderUtil.insertValue(testMap, "sc_year", "1981");
       events = MapUtil.getBucket(tm, "management").getDataList();
        for (int i = 0; i < events.size(); i++) {
            if ("planting".equals(events.get(i).get("event"))) {
                events.get(i).remove("date");
            }
        }
        log.info("Starting events in the Map 1: {}", events.toString());
        e.apply(tm);
        log.info("Modified events in the Map 1: {}", events.toString());
        
        // Case 2 PDATE is valid
        tm = fp.get(1);
        events = MapUtil.getBucket(tm, "management").getDataList();
        log.info("Starting events in the Map 2: {}", events.toString());
        e.apply(tm);
        log.info("Modified events in the Map 2: {}", events.toString());
        
        log.info("=== END TEST ===");
    }

    @Test
    @Ignore
    public void OffsetTest() {
        log.info("=== OFFSET() TEST ===");
        URL resource = this.getClass().getResource("/mach_fast.json");
        String json = "";
        try {
            json = new Scanner(new File(resource.getPath()), "UTF-8").useDelimiter("\\A").next();
        } catch (Exception ex) {
            log.error("Unable to find mach_fast.json");
            assertTrue(false);
        }
        createRule("REPLACE", "FEDATE", "DATE_OFFSET()|$FEDATE|-5");
        createRule("REPLACE", "FEAMN", "OFFSET()|$FEAMN|10");
        createRule("REPLACE", "FEDEP", "MULTIPLY()|$FEDEP|2");
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        try {
            testMap = JSONAdapter.fromJSON(json);
        } catch (Exception ex) {
            log.error("Unable to convert JSON");
            assertTrue(false);
        }

        ArrayList<HashMap<String, Object>> fp = MapUtil.flatPack(testMap);
        log.debug("Flatpack count: {}", fp.size());
        HashMap<String, Object> tm = fp.get(0);
        ArrayList<HashMap<String, String>> events = MapUtil.getBucket(tm, "management").getDataList();
        int i = 0;
        ArrayList<String> oringalFeEvents = new ArrayList();
        for (HashMap<String, String> event : events) {
            if ("fertilizer".equals(event.get("event"))) {
                oringalFeEvents.add(event.toString());
                log.debug("Original Fertilizer Event {} : {}", i, event.toString());
                i++;
            }
        }
        
        e.apply(tm);
        i = 0;
        for (HashMap<String, String> event : events) {
            if ("fertilizer".equals(event.get("event"))) {
                log.debug("Original Fertilizer Event {} : {}", i, oringalFeEvents.get(i));
                log.debug("_Updated Fertilizer Event {} : {}", i, event.toString());
                i++;
            }
        }
        log.info("=== END TEST ===");
    }

    @Test
    @Ignore
    public void GenerateMachakosFastTest() {
        log.info("=== GENERATE() TEST ===");
        URL resource = this.getClass().getResource("/mach_fast.json");
        String json = "";
        try {
            json = new Scanner(new File(resource.getPath()), "UTF-8").useDelimiter("\\A").next();
        } catch (Exception ex) {
            log.error("Unable to find mach_fast.json");
            assertTrue(false);
        }
        createRule("REPLACE", "sc_year", "1981");
        createRule("REPLACE", "exp_dur", "30");
        createRule("REPLACE", "pdate", "REMOVE_ALL_EVENTS()");
        createEngineRule(e, "REPLACE", "pdate", "AUTO_PDATE()|0501|0701|25|5", true);
        e.enableGenerators();
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        try {
            testMap = JSONAdapter.fromJSON(json);
        } catch (Exception ex) {
            log.error("Unable to convert JSON");
            assertTrue(false);
        }

        ArrayList<HashMap<String, Object>> fp = MapUtil.flatPack(testMap);
        log.debug("Flatpack count: {}", fp.size());
        HashMap<String, Object> tm = fp.get(0);
        e.apply(tm);
        ArrayList<HashMap<String, Object>> newExperiments = e.runGenerators(tm);
        assertEquals("Improper number of generated experiments", 30, newExperiments.size());
        int i = 0;
        for(HashMap<String, Object> exp : newExperiments) {
            i++;
            log.debug("Generated Exp {}: {}", i, exp.toString());
        }
        log.info("=== END TEST ===");
    }

    @Test
    @Ignore
    public void GenerateMachakosFastTest2() {
        log.info("=== GENERATE() TEST2 ===");
        URL resource = this.getClass().getResource("/mach_fast.json");
        String json = "";
        try {
            json = new Scanner(new File(resource.getPath()), "UTF-8").useDelimiter("\\A").next();
        } catch (Exception ex) {
            log.error("Unable to find mach_fast.json");
            assertTrue(false);
        }
        createRule("REPLACE", "sc_year", "1984");
        createRule("REPLACE", "exp_dur", "3");
        createEngineRule(e, "REPLACE", "pdate", "AUTO_REPLICATE_EVENTS()", true);
        e.enableGenerators();
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        try {
            testMap = JSONAdapter.fromJSON(json);
        } catch (Exception ex) {
            log.error("Unable to convert JSON");
            assertTrue(false);
        }

        ArrayList<HashMap<String, Object>> fp = MapUtil.flatPack(testMap);
        log.debug("Flatpack count: {}", fp.size());
        HashMap<String, Object> tm = fp.get(0);
        e.apply(tm);
        ArrayList<HashMap<String, Object>> newExperiments = e.runGenerators(tm);
        assertEquals("Improper number of generated experiments", 3, newExperiments.size());
        int i = 0;
        for(HashMap<String, Object> exp : newExperiments) {
            i++;
            ArrayList arr = MapUtil.getBucket(exp, "management").getDataList();
            assertEquals("Improper number of generated events in experiments " + i, 3, arr.size());
            for (int j = 0; j < arr.size(); j++) {
                log.debug("Generated events {} in Exp {}: {}", j + 1, i, arr.get(j).toString());
            }
            
        }
        log.info("=== END TEST ===");
    }

    @Test
    @Ignore
    public void GenerateMachakosFastTest3() {
        log.info("=== GENERATE() TEST3 ===");
        URL resource = this.getClass().getResource("/mach_fast.json");
        String json = "";
        try {
            json = new Scanner(new File(resource.getPath()), "UTF-8").useDelimiter("\\A").next();
        } catch (Exception ex) {
            log.error("Unable to find mach_fast.json");
            assertTrue(false);
        }
        ArrayList<HashMap<String, String>> rules;
        
        rules= new ArrayList<HashMap<String, String>>();
        rules.add(createRule2("REPLACE", "sc_year", "1981"));
        rules.add(createRule2("REPLACE", "exp_dur", "5"));
//        rules.add(createRule2("REPLACE", "pdate", "REMOVE_ALL_EVENTS()"));
        e.addGenGroup(rules, createRule2("REPLACE", "pdate", "AUTO_PDATE()|0501|0701|25|5"));
        
        rules = new ArrayList<HashMap<String, String>>();
        e.addGenGroup(rules, createRule2("REPLACE", "pdate", "AUTO_REPLICATE_EVENTS()"));
        
        rules = new ArrayList<HashMap<String, String>>();
        rules.add(createRule2("REPLACE", "sadat", "OFFSET_DATE()|$PDATE|-30"));
        rules.add(createRule2("REPLACE", "SLRGF", "ROOT_DIST()|1.0|20|200"));
        e.addGenGroup(rules, new HashMap());
        
        e.enableGenerators();
        log.debug("Added generators: {}", e.getGenerators());
        
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        try {
            testMap = JSONAdapter.fromJSON(json);
        } catch (Exception ex) {
            log.error("Unable to convert JSON");
            assertTrue(false);
        }

        ArrayList<HashMap<String, Object>> fp = MapUtil.flatPack(testMap);
        log.debug("Flatpack count: {}", fp.size());
        HashMap<String, Object> tm = fp.get(0);
        ArrayList<HashMap<String, Object>> newExperiments = e.applyStg(tm);
        assertEquals("Improper number of generated experiments", 5, newExperiments.size());
        int i = 0;
        for(HashMap<String, Object> exp : newExperiments) {
            i++;
            log.debug("Generated Events in Exp {}: {}, SADAT:{}", i, MapUtil.getBucket(exp, "management").getDataList().toString(), exp.get("sadat"));
        }
        i = 0;
        for(HashMap<String, Object> exp : newExperiments) {
            i++;
            log.debug("Generated Exp {}: {}", i, exp.toString());
        }
        log.debug("Generated soils: {}", testMap.get("soils"));
        log.info("=== END TEST3 ===");
    }

    @Test
    @Ignore
    public void GenerateMachakosFastTest4() {
        log.info("=== GENERATE() TEST4 ===");
        URL resource = this.getClass().getResource("/mach_fast.json");
        String json = "";
        try {
            json = new Scanner(new File(resource.getPath()), "UTF-8").useDelimiter("\\A").next();
        } catch (Exception ex) {
            log.error("Unable to find mach_fast.json");
            assertTrue(false);
        }
        ArrayList<HashMap<String, String>> rules;
        
        rules= new ArrayList<HashMap<String, String>>();
        rules.add(createRule2("REPLACE", "exp_dur", "5"));
        e.addGenGroup(rules, createRule2("REPLACE", "pdate", "AUTO_REPLICATE_EVENTS()"));
        
        rules = new ArrayList<HashMap<String, String>>();
        rules.add(createRule2("REPLACE", "sc_year", "1991"));
        rules.add(createRule2("REPLACE", "exp_dur", "5"));
        e.addGenGroup(rules, createRule2("REPLACE", "pdate", "AUTO_PDATE()|0501|0701|25|5"));
        
        rules = new ArrayList<HashMap<String, String>>();
        rules.add(createRule2("REPLACE", "sadat", "OFFSET_DATE()|$PDATE|-30"));
        e.addGenGroup(rules, new HashMap());
        
        e.enableGenerators();
        log.debug("Added generators: {}", e.getGenerators());
        
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        try {
            testMap = JSONAdapter.fromJSON(json);
        } catch (Exception ex) {
            log.error("Unable to convert JSON");
            assertTrue(false);
        }

        ArrayList<HashMap<String, Object>> fp = MapUtil.flatPack(testMap);
        log.debug("Flatpack count: {}", fp.size());
        HashMap<String, Object> tm = fp.get(0);
        ArrayList<HashMap<String, Object>> newExperiments = e.applyStg(tm);
        assertEquals("Improper number of generated experiments", 5, newExperiments.size());
        int i = 0;
        for(HashMap<String, Object> exp : newExperiments) {
            i++;
            log.debug("Generated Events in Exp {}: {}, SADAT:{}", i, MapUtil.getBucket(exp, "management").getDataList().toString(), exp.get("sadat"));
        }
        i = 0;
        for(HashMap<String, Object> exp : newExperiments) {
            i++;
            log.debug("Generated Exp {}: {}", i, exp.toString());
        }
        log.info("=== END TEST ===");
    }

    @Test
    @Ignore
    public void GenerateMachakosFastTest5() {
        log.info("=== GENERATE() TEST5 ===");
        URL resource = this.getClass().getResource("/mach_fast.json");
        String json = "";
        try {
            json = new Scanner(new File(resource.getPath()), "UTF-8").useDelimiter("\\A").next();
        } catch (Exception ex) {
            log.error("Unable to find mach_fast.json");
            assertTrue(false);
        }
        ArrayList<HashMap<String, String>> rules;
        
//        rules= new ArrayList<HashMap<String, String>>();
//        rules.add(createRule2("REPLACE", "exp_dur", "5"));
//        e.addGenGroup(rules, createRule2("REPLACE", "pdate", "AUTO_REPLICATE_EVENTS()"));
//        
//        rules = new ArrayList<HashMap<String, String>>();
//        rules.add(createRule2("REPLACE", "sc_year", "1991"));
//        rules.add(createRule2("REPLACE", "exp_dur", "5"));
//        e.addGenGroup(rules, createRule2("REPLACE", "pdate", "AUTO_PDATE()|0501|0701|25|5"));
        
        rules = new ArrayList<HashMap<String, String>>();
        rules.add(createRule2("REPLACE", "wst_id", "AAAA"));
        rules.add(createRule2("REPLACE", "soil_id", "BBBB"));
        e.addGenGroup(rules, new HashMap());
        
        e.enableGenerators();
        log.debug("Added generators: {}", e.getGenerators());
        
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        try {
            testMap = JSONAdapter.fromJSON(json);
        } catch (Exception ex) {
            log.error("Unable to convert JSON");
            assertTrue(false);
        }

//        ArrayList<HashMap<String, Object>> fp = MapUtil.flatPack(testMap);
        ArrayList<HashMap<String, Object>> fp = MapUtil.getRawPackageContents(testMap, "experiments");
        log.debug("Flatpack count: {}", fp.size());
        HashMap<String, Object> tm = fp.get(0);
        ArrayList<HashMap<String, Object>> newExperiments = e.applyStg(tm);
        assertEquals("Improper number of generated experiments", 1, newExperiments.size());
        int i = 0;
        for(HashMap<String, Object> exp : newExperiments) {
            i++;
            log.debug("Generated WST_ID in Exp {}: {}, {}", i, MapUtil.getValueOr(exp, "wst_id", "N/A"), MapUtil.getValueOr((HashMap) MapUtil.getObjectOr(exp, "weather", new HashMap()), "wst_id", "N/a"));
            log.debug("Generated WST_ID in Exp {}: {}, {}", i, MapUtil.getValueOr(exp, "soil_id", "N/A"), MapUtil.getValueOr((HashMap) MapUtil.getObjectOr(exp, "soil", new HashMap()), "soil_id", "N/a"));
            log.debug("Generated Exp {}: {}", i, exp.toString());
        }
//        i = 0;
//        for(HashMap<String, Object> exp : newExperiments) {
//            i++;
//            log.debug("Generated Exp {}: {}", i, exp.toString());
//        }
        ArrayList<HashMap<String, Object>> soils = MapUtil.getRawPackageContents(testMap, "soils");
        for(HashMap<String, Object> soil : soils) {
            log.debug("Soil data {}: {}", i, MapUtil.getValueOr(soil, "soil_id", "N/A"));
        }
        ArrayList<HashMap<String, Object>> wths = MapUtil.getRawPackageContents(testMap, "weathers");
        for(HashMap<String, Object> wth : wths) {
            log.debug("Weather data {}: {}", i, MapUtil.getValueOr(wth, "wst_id", "N/A"));
        }
        log.info("=== END TEST ===");
    }

    @Test
    @Ignore
    public void GenerateMachakosFullTest() {
        log.info("=== GENERATE() TEST ===");
        URL resource = this.getClass().getResource("/mach_full.json");
        String json = "";
        try {
            json = new Scanner(new File(resource.getPath()), "UTF-8").useDelimiter("\\A").next();
        } catch (Exception ex) {
            log.error("Unable to find mach_full.json");
            assertTrue(false);
        }
        createRule("REPLACE", "sc_year", "1981");
        createRule("REPLACE", "exp_dur", "30");
        createRule("REPLACE", "pdate", "REMOVE_ALL_EVENTS()");
        createEngineRule(e, "REPLACE", "pdate", "AUTO_PDATE()|0501|0701|25|5", true);
        e.enableGenerators();
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        try {
            testMap = JSONAdapter.fromJSON(json);
        } catch (Exception ex) {
            log.error("Unable to convert JSON");
            assertTrue(false);
        }

        ArrayList<HashMap<String, Object>> fp = MapUtil.flatPack(testMap);
        ArrayList<HashMap<String, Object>> full = new ArrayList<HashMap<String, Object>>();
        log.debug("Flatpack count: {}", fp.size());
        for (HashMap<String, Object> tm : fp) {
            e.apply(tm);
            ArrayList<HashMap<String, Object>> newExperiments = e.runGenerators(tm);
            full.addAll(newExperiments);
        }
        ArrayList<HashMap<String, Object>> exp = MapUtil.getRawPackageContents(testMap, "experiments");
        exp.clear();
        exp.addAll(full);
        log.debug("Replaced with generated data");
        fp = MapUtil.flatPack(testMap);
        log.debug("Flatpack count: {}", fp.size());


        Engine postEngine = new Engine();
        createEngineRule(postEngine, "REPLACE", "fedate", "FERT_DIST()|2|FE005|AP002|10|14|33.3|45|66.7", false);

        int i=0;
        int target = fp.size();

        for(HashMap<String, Object> post : fp) {
            i++;
            double pct = (i / target) * 100;
            //if (pct % 5 == 0) {
                log.debug("{}%", pct);
            //}
            postEngine.apply(post);
        }

        log.debug("51st Entry: {}", Command.traverseAndGetSiblings(fp.get(51), "pdate"));
        
        log.info("=== END TEST ===");
    }

    @Test
    public void CalcTavAmpTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "w_date", "19890101");
        AcePathfinderUtil.insertValue(testMap, "tmax", "26.3");
        AcePathfinderUtil.insertValue(testMap, "tmin", "16.2");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19890102");
        AcePathfinderUtil.insertValue(testMap, "tmax", "25");
        AcePathfinderUtil.insertValue(testMap, "tmin", "15.1");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19890103");
        AcePathfinderUtil.insertValue(testMap, "tmax", "25.1");
        AcePathfinderUtil.insertValue(testMap, "tmin", "15.4");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19890201");
        AcePathfinderUtil.insertValue(testMap, "tmax", "27.9");
        AcePathfinderUtil.insertValue(testMap, "tmin", "17.4");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19890202");
        AcePathfinderUtil.insertValue(testMap, "tmax", "27.9");
        AcePathfinderUtil.insertValue(testMap, "tmin", "17.4");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19890203");
        AcePathfinderUtil.insertValue(testMap, "tmax", "28.1");
        AcePathfinderUtil.insertValue(testMap, "tmin", "13.8");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19890303");
        AcePathfinderUtil.insertValue(testMap, "tmax", "27.5");
        AcePathfinderUtil.insertValue(testMap, "tmin", "13");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19890304");
        AcePathfinderUtil.insertValue(testMap, "tmax", "31");
        AcePathfinderUtil.insertValue(testMap, "tmin", "16.9");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19890305");
        AcePathfinderUtil.insertValue(testMap, "tmax", "32.3");
        AcePathfinderUtil.insertValue(testMap, "tmin", "16.5");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19900101");
        AcePathfinderUtil.insertValue(testMap, "tmax", "20.4");
        AcePathfinderUtil.insertValue(testMap, "tmin", "5.2");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19900102");
        AcePathfinderUtil.insertValue(testMap, "tmax", "20.1");
        AcePathfinderUtil.insertValue(testMap, "tmin", "3.2");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19900103");
        AcePathfinderUtil.insertValue(testMap, "tmax", "25");
        AcePathfinderUtil.insertValue(testMap, "tmin", "8.1");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19900201");
        AcePathfinderUtil.insertValue(testMap, "tmax", "29.6");
        AcePathfinderUtil.insertValue(testMap, "tmin", "13.6");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19900202");
        AcePathfinderUtil.insertValue(testMap, "tmax", "29.2");
        AcePathfinderUtil.insertValue(testMap, "tmin", "17.7");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19900203");
        AcePathfinderUtil.insertValue(testMap, "tmax", "30.4");
        AcePathfinderUtil.insertValue(testMap, "tmin", "15.6");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19900204");
        AcePathfinderUtil.insertValue(testMap, "tmax", "28.3");
        AcePathfinderUtil.insertValue(testMap, "tmin", "16.2");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19900205");
        AcePathfinderUtil.insertValue(testMap, "tmax", "19.4");
        AcePathfinderUtil.insertValue(testMap, "tmin", "6.9");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19900301");
        AcePathfinderUtil.insertValue(testMap, "tmax", "26.2");
        AcePathfinderUtil.insertValue(testMap, "tmin", "8.8");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19900302");
        AcePathfinderUtil.insertValue(testMap, "tmax", "26.2");
        AcePathfinderUtil.insertValue(testMap, "tmin", "11.7");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19900303");
        AcePathfinderUtil.insertValue(testMap, "tmax", "21.5");
        AcePathfinderUtil.insertValue(testMap, "tmin", "10.8");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19901229");
        AcePathfinderUtil.insertValue(testMap, "tmax", "29.3");
        AcePathfinderUtil.insertValue(testMap, "tmin", "16.1");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19901230");
        AcePathfinderUtil.insertValue(testMap, "tmax", "30.1");
        AcePathfinderUtil.insertValue(testMap, "tmin", "17.4");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19901231");
        AcePathfinderUtil.insertValue(testMap, "tmax", "25");
        AcePathfinderUtil.insertValue(testMap, "tmin", "16.9");

        log.info("=== TAVAMP() TEST ===");
        log.info("Starting map: {}", testMap.toString());
        createRule("FILL", "TAV,TAMP", "TAVAMP()");
        e.apply(testMap);
        log.info("Modified Map: {}", testMap.toString());
        log.info("=== END TEST ===");
    }

    @Test
    public void CalcEtoTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        
        AcePathfinderUtil.insertValue(testMap, "wst_lat", "26.65");
        AcePathfinderUtil.insertValue(testMap, "wst_long", "-80.633");
        AcePathfinderUtil.insertValue(testMap, "wst_elev", "3.00");
        AcePathfinderUtil.insertValue(testMap, "wndht", "10.00");
        AcePathfinderUtil.insertValue(testMap, "amth", "0.3", "weather");
        AcePathfinderUtil.insertValue(testMap, "bmth", "0.4", "weather");
        AcePathfinderUtil.insertValue(testMap, "psyvnt", "Forced", "weather");
        AcePathfinderUtil.insertValue(testMap, "w_date", "19890823");
        AcePathfinderUtil.insertValue(testMap, "srad", "13.6");
        AcePathfinderUtil.insertValue(testMap, "tmax", "26.4");
        AcePathfinderUtil.insertValue(testMap, "tmin", "12.8");
        AcePathfinderUtil.insertValue(testMap, "sunh", "");
        AcePathfinderUtil.insertValue(testMap, "tavd", "");
        AcePathfinderUtil.insertValue(testMap, "wind", "180");
        AcePathfinderUtil.insertValue(testMap, "rhmnd", "30", AcePathfinder.INSTANCE.getPath("w_date"));
        AcePathfinderUtil.insertValue(testMap, "rhmxd", "50", AcePathfinder.INSTANCE.getPath("w_date"));
        AcePathfinderUtil.insertValue(testMap, "vprsd", "1.6");
        AcePathfinderUtil.insertValue(testMap, "tdew", "13.5");
        AcePathfinderUtil.insertValue(testMap, "tdry", "25.3");
        AcePathfinderUtil.insertValue(testMap, "twet", "22.2");

        log.info("=== REFET() TEST ===");
        log.info("Starting map: {}", testMap.toString());
        createRule("FILL", "ETO", "REFET()");
        e.apply(testMap);
        log.info("Modified Map: {}", testMap.toString());
        log.info("=== END TEST ===");
    }

    @Test
    public void CalcIcnDistTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "icbl", "15");
        AcePathfinderUtil.insertValue(testMap, "icbl", "30");
        AcePathfinderUtil.insertValue(testMap, "icbl", "60");
        AcePathfinderUtil.insertValue(testMap, "icbl", "90");
        AcePathfinderUtil.insertValue(testMap, "icbl", "120");
        AcePathfinderUtil.insertValue(testMap, "icbl", "150");
        AcePathfinderUtil.insertValue(testMap, "icbl", "180");
        AcePathfinderUtil.insertValue(testMap, "sllb", "15");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.15");
        AcePathfinderUtil.insertValue(testMap, "sllb", "30");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.16");
        AcePathfinderUtil.insertValue(testMap, "sllb", "60");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.21");
        AcePathfinderUtil.insertValue(testMap, "sllb", "90");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.23");
        AcePathfinderUtil.insertValue(testMap, "sllb", "120");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.31");
        AcePathfinderUtil.insertValue(testMap, "sllb", "150");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.31");
        AcePathfinderUtil.insertValue(testMap, "sllb", "180");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.31");

        log.info("=== ICN_DIST() TEST ===");
        log.info("Starting map: {}", testMap.toString());
        createRule("FILL", "icn_tot,ichn4,icno3", "ICN_DIST()|25");
        e.apply(testMap);
        log.info("Modified Map: {}", testMap.toString());
        log.info("=== END TEST ===");
    }

    @Test
    public void CalcPaddyTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "pdate", "19820312");
        log.info("=== PADDY() TEST ===");
        log.debug("Starting map: {}", testMap);
        createRule("REPLACE", "idate", "PADDY()|3|2|150|-3|20|5|4|30|10|11|50|15");
        e.apply(testMap);
        log.debug("Modified map: {}", testMap.toString());
        log.info("=== END TEST ===");
    }

    @Test
    public void CreateNewEventTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "pdate", "19820312");
        log.info("=== NEW_EVENT() TEST ===");
        log.debug("Starting map: {}", testMap);
        createRule("CREATE", "Irrigation", "NEW_EVENT()|3|irop|IR010|irval|150");
        e.apply(testMap);
        log.debug("Modified map: {}", testMap.toString());
        log.info("=== END TEST ===");
    }

    @Test
    public void SplittingSoilLayerTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "sllb", "10");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.15");
        AcePathfinderUtil.insertValue(testMap, "sllb", "40");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.16");
        AcePathfinderUtil.insertValue(testMap, "sllb", "80");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.21");
        AcePathfinderUtil.insertValue(testMap, "sllb", "110");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.23");
        AcePathfinderUtil.insertValue(testMap, "sllb", "180");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.31");
        AcePathfinderUtil.insertValue(testMap, "sllb", "250");
        AcePathfinderUtil.insertValue(testMap, "slbdm", "1.31");
        AcePathfinderUtil.insertValue(testMap, "pdate", "19820312");
        log.info("=== LYRSET() TEST ===");
        log.debug("Starting map: {}", testMap);
        createRule("REPLACE", "SLLB", "LYRSET()|123");
        e.apply(testMap);
        log.debug("Modified map: {}", testMap.toString());
        assertEquals("LYRSET(): expected layer size is 11", 11, MapUtil.getBucket(testMap, "soil").getDataList().size());
        log.info("=== END TEST ===");
    }
    
    @After
    public void tearDown() {
        e = null;
    }
}
