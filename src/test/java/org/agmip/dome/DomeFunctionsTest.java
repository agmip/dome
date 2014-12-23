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

    @Test
    public void domeCalcTavTamp() {
        coreMap = new HashMap<String, Object>();
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19890101");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "26.3");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "16.2");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19890102");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "25");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "15.1");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19890103");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "25.1");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "15.4");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19890201");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "27.9");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "17.4");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19890202");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "27.9");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "17.4");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19890203");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "28.1");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "13.8");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19890303");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "27.5");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "13");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19890304");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "31");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "16.9");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19890305");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "32.3");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "16.5");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19900101");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "20.4");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "5.2");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19900102");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "20.1");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "3.2");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19900103");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "25");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "8.1");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19900201");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "29.6");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "13.6");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19900202");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "29.2");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "17.7");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19900203");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "30.4");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "15.6");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19900204");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "28.3");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "16.2");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19900205");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "19.4");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "6.9");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19900301");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "26.2");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "8.8");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19900302");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "26.2");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "11.7");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19900303");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "21.5");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "10.8");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19901229");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "29.3");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "16.1");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19901230");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "30.1");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "17.4");
        AcePathfinderUtil.insertValue(coreMap, "w_date", "19901231");
        AcePathfinderUtil.insertValue(coreMap, "tmax", "25");
        AcePathfinderUtil.insertValue(coreMap, "tmin", "16.9");

        addToTestArray(testMap, "tav", "20.29");
        addToTestArray(testMap, "tamp", "5.58");

        assertEquals("DOME PCTAWC() broken", testMap, DomeFunctions.getTavAndAmp(coreMap));
    }
    
    @Test
    public void domeId() {
        String domeID    = "MACHAKOS-1----BASELINE";
        String newDomeID = "MACHAKOS-1----0XFX-BASELINE";
        HashMap<String, Object> dome = new HashMap();
        DomeUtil.updateMetaInfo(dome, domeID);
        assertEquals(domeID, DomeUtil.generateDomeName(dome));
        DomeUtil.updateMetaInfo(dome, newDomeID);
        assertEquals(newDomeID, DomeUtil.generateDomeName(dome));
        HashMap<String, Object> dome2 = new HashMap();
        HashMap<String, String> info = new HashMap();
        info.put("reg_id", "MACHAKOS");
        info.put("stratum", "1");
        info.put("clim_id", "0XFX");
        info.put("description", "BASELINE");
        dome2.put("info", info);
        assertEquals(newDomeID, DomeUtil.generateDomeName(dome2));
        
    }
}
