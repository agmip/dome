package org.agmip.dome;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.agmip.ace.util.AcePathfinderUtil;

public class DomeFunctionsTest {
    private static final Logger log = LoggerFactory.getLogger(DomeFunctionsTest.class);
    HashMap<String, Object> coreMap;
    HashMap<String, ArrayList<String>> testMap;
    
    private static void addToTestArray(HashMap<String, ArrayList<String>> test, 
                                       String var, String val) {

        boolean written = false;
        if (test.containsKey(var)) {
            ArrayList<String> vals = test.get(var);
            vals.add(val);
            written = true;
        }
        
        if (!written) {
            ArrayList<String> vals = new ArrayList<String>();
            vals.add(val);
            test.put(var, vals);
        }
    }

    @Before
    public void setup() {
        log.info("======= START TEST =======");
        coreMap = new HashMap<String, Object>();
        testMap = new HashMap<String, ArrayList<String>>();
        AcePathfinderUtil.insertValue(coreMap, "icdat", "19810101");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19810101");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19810102");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19810103");
        AcePathfinderUtil.insertValue(coreMap, "pdate", "19810101");
        AcePathfinderUtil.insertValue(coreMap, "pdate", "19820102");
        AcePathfinderUtil.insertValue(coreMap, "idate", "19810101");
        AcePathfinderUtil.insertValue(coreMap, "icbl", "15");
        AcePathfinderUtil.insertValue(coreMap, "icbl", "20");
        AcePathfinderUtil.insertValue(coreMap, "sllb", "15");
        AcePathfinderUtil.insertValue(coreMap, "sllb", "20");
        ArrayList<HashMap<String, String>> testMap;
        log.info("Starting Map: {}", coreMap);
    }

    @After
    public void cleanUp() {
        coreMap = null;
        testMap = null;
        log.info("========= END TEST =========\n");
    }
    
    @Test
    public void domeDateOffset() {
        addToTestArray(testMap, "testdat", "19810108");
        assertEquals("DOME OFFSET_DATE() broken", testMap, DomeFunctions.dateOffset(coreMap, "testdat", "$icdat", "7"));
    }

    @Test
    public void domeNegativeOffsetYearBoundary() {
        addToTestArray(testMap, "testdat", "19801225");
        assertEquals("DOME OFFSET_DATE() broken", testMap, DomeFunctions.dateOffset(coreMap, "testdat", "$icdat", "-7"));
    }

    @Test
    public void domeLeapYearDateOffset() {
        addToTestArray(testMap, "testdat", "19800302");
        assertEquals("DOME OFFSET_DATE() broken", testMap, DomeFunctions.dateOffset(coreMap, "testdat", "19800228", "3"));
    }

    @Test
    public void domeSameDateOffset() {
        addToTestArray(testMap, "icdat", "19810108");
        assertEquals("DOME OFFSET_DATE() broken", testMap, DomeFunctions.dateOffset(coreMap, "icdat", "$icdat", "7"));
    }

    @Test
    public void domeNumericOffset() {
        addToTestArray(testMap, "testnum", "12345");
        assertEquals("DOME OFFSET() broken", testMap, DomeFunctions.numericOffset(coreMap, "testnum", "12342", "3"));
    }

    @Test
    public void domeNestedSourceDateOffset() {
        addToTestArray(testMap, "testdat", "19810108");
        addToTestArray(testMap, "testdat", "19810109");
        addToTestArray(testMap, "testdat", "19810110");
        assertEquals("DOME OFFSET_DATE() broken", testMap, DomeFunctions.dateOffset(coreMap, "testdat", "$w_date", "7"));
    }

    @Test
    public void domeEventSourceDateOffset() {
        addToTestArray(testMap, "testdat", "19810108");
        addToTestArray(testMap, "testdat", "19820109");
        assertEquals("DOME OFFSET_DATE() broken", testMap, DomeFunctions.dateOffset(coreMap, "testdat", "$pdate", "7"));
    }

    @Test
    public void domeSameEventVarsDateOffset() {
        addToTestArray(testMap, "pdate", "19810108");
        addToTestArray(testMap, "pdate", "19820109");
        assertEquals("DOME OFFSET_DATE() broken", testMap, DomeFunctions.dateOffset(coreMap, "pdate", "$pdate", "7"));
    }

    @Test
    public void domeDifferentEventVarsDateOffset() {
        assertEquals("DOME OFFSET_DATE() broken", new HashMap(), DomeFunctions.dateOffset(coreMap, "fedate", "$pdate", "7"));
    }

    @Test
    public void domeNestedSourceNestedTargetDateOffset() {
        addToTestArray(testMap, "sllb", "20");
        addToTestArray(testMap, "sllb", "25");
        assertEquals("DOME OFFSET() broken", testMap, DomeFunctions.numericOffset(coreMap, "sllb", "$icbl", "5"));
    }

    @Test
    public void domeMultiply() {
        AcePathfinderUtil.insertValue(coreMap, "f1", "5", "");
        log.debug("Modified map: {}", coreMap.toString());
        addToTestArray(testMap, "testnum", "25");
        assertEquals("DOME MULTIPLY() broken", testMap, DomeFunctions.multiply(coreMap, "testnum", "$f1", "5"));
    }

    @Test
    public void domeMultiplyOneNested() {
        addToTestArray(testMap, "testnum", "30");
        addToTestArray(testMap, "testnum", "40");
        assertEquals("DOME MULTIPLY() broken", testMap, DomeFunctions.multiply(coreMap, "testnum", "$icbl", "2"));
    }

    @Test
    public void domeMultiplyOneEvent() {
        addToTestArray(testMap, "testnum", "39620202");
        addToTestArray(testMap, "testnum", "39640204");
        assertEquals("DOME MULTIPLY() broken", testMap, DomeFunctions.multiply(coreMap, "testnum", "$pdate", "2"));
    }

    @Test
    public void domeMultiplyTwoNested() {
        addToTestArray(testMap, "testnum", "225");
        addToTestArray(testMap, "testnum", "400");
        assertEquals("DOME MULTIPLY() broken", testMap, DomeFunctions.multiply(coreMap, "testnum", "$icbl", "$sllb"));
    }

    @Test
    public void domeCheckICH2O() {
        coreMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(coreMap, "sllb", "30");
        AcePathfinderUtil.insertValue(coreMap, "slll", "0.22");
        AcePathfinderUtil.insertValue(coreMap, "sldul", "0.32");
        AcePathfinderUtil.insertValue(coreMap, "sloc", "0.9");
        AcePathfinderUtil.insertValue(coreMap, "sllb", "50");
        AcePathfinderUtil.insertValue(coreMap, "slll", "0.15");
        AcePathfinderUtil.insertValue(coreMap, "sldul", "0.24");
        AcePathfinderUtil.insertValue(coreMap, "sloc", "0.01");
        
        addToTestArray(testMap, "ich2o", "0.2650");
        addToTestArray(testMap, "ich2o", "0.1905");
        addToTestArray(testMap, "icbl", "30");
        addToTestArray(testMap, "icbl", "50");

        assertEquals("DOME PCTAWC() broken", testMap, DomeFunctions.percentAvailWaterContent(coreMap, "45"));
    }
}
