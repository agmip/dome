package org.agmip.dome;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

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
        log.debug("Starting map: {}", testMap);
        createRule("REPLACE", "fedate", "FERT_DIST()|2|FE005|AP002|10|14|33.3|45|66.7");
        e.apply(testMap);
        log.debug("Modified map: {}", testMap.toString());
    }

    @Test
    public void CalcOMDistTest() {
        HashMap<String, Object> testMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(testMap, "pdate", "19820312");
        AcePathfinderUtil.insertValue(testMap, "omamt", "1000");
        log.debug("Starting map: {}", testMap);
        createRule("FILL", "omdat", "OM_DIST()|-7|RE003|8.3|5|50|2.5");
        e.apply(testMap);
        log.debug("Modified Map: {}", testMap.toString());
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
    
    @After
    public void tearDown() {
        e = null;
    }
}
