package org.agmip.dome;

import com.rits.cloning.Cloner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.agmip.util.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Engine of the batch DOME, which reads in a batch with multiple DOME
 * ruleset and applies the rules to a dataset.
 *
 * Terms <ul> <li><strong>Command</strong> which method (FILL/REPLACE) are we
 * working in</li> <li><strong>Function</strong> a function used to populate the
 * command</li> <li><strong>Static</strong> populate the command with a static
 * reference (either a variable or a value)</li> </ul>
 *
 */
public class BatchEngine {

    private static final Logger log = LoggerFactory.getLogger(BatchEngine.class);
    /* Each group contains multilpe non-generating rules at first, one and only one generating rule is allowed in the last of the array. */
    private final ArrayList<HashMap<String, Object>> batchGroup;
    /* A list of keys to extract from the resulting generated datasets. */
    HashSet<String> keysToExtractFinal = new HashSet<String>();
//    private int cur = 0;
    private final String domeName;
    private int nextIdx = 0;

    /**
     * Construct a new engine with the ruleset passed in.
     *
     * @param batchDome A batch-run DOME
     */
    public BatchEngine(HashMap<String, Object> batchDome) {
        this.batchGroup = DomeUtil.getBatchGroup(batchDome);
        this.domeName = DomeUtil.generateDomeName(batchDome);
    }

    /**
     * Construct a new engine with the ruleset passed in.
     *
     * @param batchGroup A DOME batch group.
     * @param domeName
     */
    public BatchEngine(ArrayList<HashMap<String, Object>> batchGroup, String domeName) {
        this.batchGroup = batchGroup;
        this.domeName = domeName;
    }

    protected BatchEngine() {
        this.batchGroup = new ArrayList<HashMap<String, Object>>();
        this.domeName = "";
    }
    
    public String getDomeName() {
        return this.domeName;
    }
    
    public String getBatchName() {
        if (hasNext()) {
            return this.domeName + "-b" + getCurGroupId();
        } else {
            return this.domeName;
        }
    }

    /**
     * Add more rules to the Engine
     *
     * @param batchGroup new set of batch to append (from another DOME)
     */
    public void appendRules(ArrayList<HashMap<String, Object>> batchGroup) {
        this.batchGroup.addAll(batchGroup);
    }
    
    public boolean hasNext() {
        return nextIdx < batchGroup.size();
    }
    
    public String getCurGroupId() {
        if (nextIdx - 1 > -1 && nextIdx - 1 < batchGroup.size()) {
            return MapUtil.getValueOr(batchGroup.get(nextIdx - 1), "group_id", "");
        } else {
            return "";
        }
    }

    public String getNextGroupId() {
        if (nextIdx > -1 && nextIdx < batchGroup.size()) {
            return MapUtil.getValueOr(batchGroup.get(nextIdx), "group_id", "");
        } else {
            return "";
        }
    }
    
    public ArrayList<HashMap<String, Object>> getNextBatchRun() {
        HashMap<String, Object> batch = batchGroup.get(nextIdx);
        ArrayList<HashMap<String, Object>> run = DomeUtil.getBatchRuns(batch);
        nextIdx++;
        return run;
    }
    
    protected ArrayList<HashMap<String, Object>> getCurrentBatchRun() {
        HashMap<String, Object> batch = batchGroup.get(nextIdx - 1);
        return DomeUtil.getBatchRuns(batch);
    }

    /**
     * Apply the rule set to the dataset passed in.
     *
     * @param source A dataset to modify according to the DOME ruleset.
     */
    public void applyNext(HashMap<String, Object> source) {
        
        if (hasNext()) {
            ArrayList<HashMap<String, Object>> runs = getNextBatchRun();
            ArrayList<HashMap<String, Object>> flattenedData = MapUtil.flatPack(source);
            ArrayList<HashMap<String, Object>> newExps = new ArrayList();
            ArrayList<HashMap> soilDataArr = new ArrayList();
            ArrayList<HashMap> wthDataArr = new ArrayList();
            ArrayList<Engine> expEngines = new ArrayList();
            ArrayList<Engine> soilEngines = new ArrayList();
            ArrayList<Engine> wthEngines = new ArrayList();
            
            Cloner c = new Cloner();
            HashMap<String, HashMap<String, ArrayList<HashMap<String, String>>>> soilDomeMap = new HashMap();
            HashMap<String, HashMap<String, ArrayList<HashMap<String, String>>>> wthDomeMap = new HashMap();
            HashSet<String> soilIds = DomeUtil.getSWIdsSet(MapUtil.getRawPackageContents(source, "soils"), "soil_id");
            HashSet<String> wstIds = DomeUtil.getSWIdsSet(MapUtil.getRawPackageContents(source, "weathers"), "wth_id", "clim_id");
            
            for (HashMap<String, Object> entry : flattenedData) {
                
                String soilId = MapUtil.getValueOr(entry, "soil_id", "");
                String wstId = MapUtil.getValueOr(entry, "wst_id", "");
                String climId = MapUtil.getValueOr(entry, "clim_id", "");
                HashMap soilData = (HashMap) entry.remove("soil");
                HashMap wthData = (HashMap) entry.remove("weather");
                
                for (HashMap<String, Object> run : runs) {
                    
                    // Replicate the experiment entry
                    HashMap<String, Object> newExp = c.deepClone(entry);
                    newExp.put("soil", soilData);
                    newExp.put("weather", wthData);
                    String exname = MapUtil.getValueOr(newExp, "exname", "");
                    String runNum = MapUtil.getValueOr(run, "batch_run#", "");
                    if (exname.contains("_b")) {
                        exname = exname.substring(0,exname.indexOf("_b"));
                    }
                    String[] exvalue = {exname+"_b"+runNum};
                    Assume.run(newExp, "exname", exvalue, true);
                    
                    // Initialize the batch engine
                    BatchRunEngine e = new BatchRunEngine(run); // TODO for dome name
                    
                    // Update the data link for soil and weather data
                    updateSWDataLink(newExp, source, e);
                    
                    // Check if it is necessary to replicate the soil data
                    ArrayList<HashMap<String, String>> sRules = e.extractSoilRules();
                    HashMap<String, ArrayList<HashMap<String, String>>> lastAppliedSoilDomes = soilDomeMap.get(soilId);
                    if (lastAppliedSoilDomes == null) {
                        soilDataArr.add(entry);
                        soilEngines.add(new Engine(sRules, this.domeName + "-b" + getCurGroupId() + "-" + runNum));
                        lastAppliedSoilDomes = new HashMap();
                        lastAppliedSoilDomes.put(runNum, sRules);
                        soilDomeMap.put(soilId, lastAppliedSoilDomes);
                    } else if (!lastAppliedSoilDomes.containsKey(runNum)) {
                        boolean isSameRules = false;
                        for (ArrayList<HashMap<String, String>> rules : lastAppliedSoilDomes.values()) {
                            if (rules.equals(sRules)) {
                                isSameRules = true;
                                break;
                            }
                        }
                        if (!isSameRules) {
                            DomeUtil.replicateSoil(entry, soilIds, MapUtil.getRawPackageContents(source, "soils"));
                            soilDataArr.add(entry);
                            soilEngines.add(new Engine(sRules, this.domeName + "-b" + getCurGroupId() + "-" + runNum));
                            lastAppliedSoilDomes.put(runNum, sRules);
                        }
                    }
                    
                    // Check if it is necessary to replicate the weather data
                    ArrayList<HashMap<String, String>> wRules = e.extractWthRules();
                    HashMap<String, ArrayList<HashMap<String, String>>> lastAppliedWthDomes = wthDomeMap.get(wstId+climId);
                    if (lastAppliedWthDomes == null) {
                        wthDataArr.add(entry);
                        wthEngines.add(new Engine(wRules, this.domeName + "-b" + getCurGroupId() + "-" + runNum));
                        lastAppliedWthDomes = new HashMap();
                        lastAppliedWthDomes.put(runNum, wRules);
                        wthDomeMap.put(wstId+climId, lastAppliedWthDomes);
                    } else if (!lastAppliedWthDomes.containsKey(runNum)) {
                        boolean isSameRules = false;
                        for (ArrayList<HashMap<String, String>> rules : lastAppliedWthDomes.values()) {
                            if (rules.equals(wRules)) {
                                isSameRules = true;
                                break;
                            }
                        }
                        if (!isSameRules) {
                            DomeUtil.replicateWth(entry, wstIds, MapUtil.getRawPackageContents(source, "weathers"));
                            wthDataArr.add(entry);
                            wthEngines.add(new Engine(wRules, this.domeName + "-b" + getCurGroupId() + "-" + runNum));
                            lastAppliedWthDomes.put(runNum, wRules);
                        }
                    }
                    
                    // Add to the batch apply list
                    expEngines.add(e);
                    newExps.add(newExp);
                    newExp.put("batch_dome_applied", "Y");
                    newExp.put("batch_dome", this.domeName);
                    newExp.put("batch_run#", run.get("batch_run#"));
                }
            }
            
            // TODO should be in the outside of the loop, which means experiment also need to be applied with DOME later
            // Apply batch DOME
            for (int i = 0; i < soilDataArr.size(); i++) {
                soilEngines.get(i).apply(soilDataArr.get(i));
            }
            for (int i = 0; i < wthDataArr.size(); i++) {
                wthEngines.get(i).apply(wthDataArr.get(i));
            }
            for (int i = 0; i < newExps.size(); i++) {
                expEngines.get(i).apply(newExps.get(i));
            }
            
            // Update the original data set
            ArrayList<HashMap<String, Object>> exp = MapUtil.getRawPackageContents(source, "experiments");
            exp.clear();
            exp.addAll(newExps);

//            curIdx++;
        } else {
            // TODO
        }
        
    }
    
    private void updateSWDataLink(HashMap entry, HashMap source, Engine e) {
        
        String soilId = MapUtil.getValueOr(entry, "soil_id", "");
        String wstId = MapUtil.getValueOr(entry, "wst_id", "");
        boolean isClimIDChanged = e.updateWSRef(entry, true, true);
        String newSoilId = MapUtil.getValueOr(entry, "soil_id", "");
        String newWstId = MapUtil.getValueOr(entry, "wst_id", "");
        if (!newSoilId.equals(soilId)) {
            ArrayList<HashMap> soilArr = MapUtil.getObjectOr(source, "soils", new ArrayList());
            for (HashMap soilData : soilArr) {
                if (newSoilId.equals(MapUtil.getValueOr(soilData, "soil_id", ""))) {
                    entry.put("soil", soilData);
                    break;
                }
            }
        }
        if (isClimIDChanged || !newWstId.equals(wstId)) {
            ArrayList<HashMap> wthArr = MapUtil.getObjectOr(source, "weathers", new ArrayList());
            for (HashMap wthData : wthArr) {
                if (newSoilId.equals(MapUtil.getValueOr(wthData, "wst_id", ""))) {
                    entry.put("weather", wthData);
                    break;
                }

            }
        }

    }

    public ArrayList<String> currentModifiedVarList() {
        ArrayList<String> ret = new ArrayList<String>();
        ArrayList<HashMap<String, Object>> runs = getCurrentBatchRun();
        for (HashMap<String, Object> run : runs) {
            ret.addAll((new Engine(run)).modifiedVarList());
        }
        return ret;
    }

    protected class BatchRunEngine extends Engine {
        
        public BatchRunEngine(HashMap<String, Object> run) {
            super(run);
        }
        
        @Override
        protected void applyRule(HashMap<String, Object> data, HashMap<String, String> rule) {
            if (isAdjWthRule(rule)) {
                DomeUtil.insertAdjustment(data, rule);
            } else {
                super.applyRule(data, rule);
            }
        }
        
        @Override
        protected boolean isWthRules(HashMap<String, String> rule) {
            boolean isWRule = false;
            String cmd = rule.get("cmd").toUpperCase();
            String var = rule.get("variable").toLowerCase();

            // NPE defender
            if (var == null) {
                log.error("Invalid rule: {}", rule.toString());
                return isWRule;
            }

            String a = rule.get("args");
            if (a == null) {
                a = "";
            }
            String[] args = a.split("[|]");

            if (cmd.equals("INFO")) {
                log.debug("Recevied an INFO command");
            } else if (cmd.equals("FILL") || cmd.equals("REPLACE") || cmd.equals("REPLACE_FIELD_ONLY") || cmd.equals("REPLACE_STRATEGY_ONLY")) {
                // If it is simple calculation or set value directly to the soil/weather variable
                if (!args[0].endsWith("()")
                        || args[0].equals("OFFSET()")
                        || args[0].equals("MULTIPLY()")
                        || args[0].equals("OFFSET_DATE()")
                        || args[0].equals("DATE_OFFSET()")
                        || args[0].equals("ROOT_DIST()")
                        || args[0].equals("LYRSET()")
                        || args[0].equals("TRANSPOSE()")
                        || args[0].equals("REDUCEWP()")) {
                    if (!var.equals("wst_id") && !var.equals("clim_id") && !isAdjWthRule(rule)) { // Add isAdjWthRule for Batch
                        String path = Command.getPathOrRoot(var);
                        String[] paths = path.split(",");
                        for (String p : paths) {
                            if (p.equals("weather") || p.equals("weather@dailyWeather")) {
                                isWRule = true;
                                break;
                            }
                        }
                    }
                } // If call function which might change soil/weather data
                else if (args[0].equals("TAVAMP()")) {
                    isWRule = true;
                }
            }
            return isWRule;
        }
        
        private boolean isAdjWthRule(HashMap<String, String> rule) {
            List<String> vars = Arrays.asList(new String[]{"tmax", "tmin", "srad", "wind", "rain", "co2y", "tdew"});
            List<String> methods = Arrays.asList(new String[]{"OFFSET()", "MULTIPLY()"});
            
            String var = rule.get("variable").toLowerCase();
            String a = rule.get("args");
            if (a == null) {
                a = "";
            }
            String[] args = a.split("[|]");
            return (var != null && vars.contains(var) && (!args[0].endsWith("()") || methods.contains(args[0])));
        }
    }
}
