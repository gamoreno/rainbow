package org.sa.rainbow.rubis.adaptation;

import java.util.HashMap;

import org.ho.yaml.Yaml;
import org.sa.rainbow.core.Rainbow;
import org.sa.rainbow.core.adaptation.AdaptationExecutionOperatorT;
import org.sa.rainbow.core.adaptation.AdaptationTree;
import org.sa.rainbow.model.acme.rubis.RubisModelHelper;
import org.sa.rainbow.stitch.core.Strategy;

import pladapt.EnvironmentDTMCPartitioned;
import pladapt.GenericConfiguration;
import pladapt.GenericConfigurationManager;
import pladapt.JavaSDPAdaptationManager;
import pladapt.SDPAdaptationManager;
import pladapt.StringVector;

public class AdaptationManager extends AdaptationManagerBase {
	private static final String PLASDP_REACH_PATH = "rainbow.adaptation.plasdp.reachPath";
    private static final String PLASDP_REACH_MODEL = "rainbow.adaptation.plasdp.reachModel";
    private JavaSDPAdaptationManager m_adaptMgr;

	public AdaptationManager() {
		// TODO Auto-generated constructor stub
	}

    protected void initializeAdaptationMgr(RubisModelHelper rubisModel) {
        log("Starting PLA-SDP Adaptation Manager initialization");
        super.initializeAdaptationMgr(rubisModel);
        m_adaptMgr = new JavaSDPAdaptationManager();

        // define configuration space
        GenericConfigurationManager configMgr = new GenericConfigurationManager();
        GenericConfiguration configTemplate = configMgr.getConfigurationTemplate();
        configTemplate.setInt(RubisModelHelper.CONFIG_SERVERS, 0);
        configTemplate.setInt(RubisModelHelper.CONFIG_DIMMER, 0);
        configTemplate.setInt(RubisModelHelper.CONFIG_ADD_SERVER_PROGRESS, 0);
        
        int maxServers = rubisModel.getMaxServers();
        int addServerLatencyPeriods = rubisModel.getAddServerLatencyPeriods();
        
        for (int dimmerLevels = 0; dimmerLevels < rubisModel.getDimmerLevels(); dimmerLevels++) {
            for (int servers = 0; servers < maxServers; servers++) {
                for (int bootProgress = 0; bootProgress <= addServerLatencyPeriods; bootProgress++) {
                	GenericConfiguration config = configMgr.addNewConfiguration();
                	config.setInt(RubisModelHelper.CONFIG_SERVERS, servers);
                	config.setInt(RubisModelHelper.CONFIG_DIMMER, dimmerLevels);
                	config.setInt(RubisModelHelper.CONFIG_ADD_SERVER_PROGRESS, bootProgress);
                }
            }
        }
        
    	// create configuration parameters.
        // the following hardcoded values are like the constants above
    	HashMap<String, Object> params = new HashMap<>();
        params.put(SDPAdaptationManager.getNO_LATENCY(), Boolean.FALSE);
        params.put(SDPAdaptationManager.getREACH_PATH(), Rainbow.instance().getProperty(PLASDP_REACH_PATH));
        params.put(SDPAdaptationManager.getREACH_MODEL(), Rainbow.instance().getProperty(PLASDP_REACH_MODEL));
		params.put(SDPAdaptationManager.getREACH_SCOPE(),
				"S=" + maxServers + " TAP#=" + addServerLatencyPeriods + " D=" + rubisModel.getDimmerLevels());
        String yamlParams = Yaml.dump(params);
        
        m_adaptMgr.initialize(configMgr, yamlParams);
        log("PLA-SDP Adaptation Manager Initialized");
    }
    
	protected AdaptationTree<Strategy> checkAdaptationImpl(RubisModelHelper rubisModel,
			EnvironmentDTMCPartitioned env) {
    	AdaptationTree<Strategy> at = null;
    	
        GenericConfiguration currentConfig = new GenericConfiguration();
        currentConfig.setInt(RubisModelHelper.CONFIG_SERVERS, rubisModel.getNumActiveServers() - 1);
        currentConfig.setInt(RubisModelHelper.CONFIG_DIMMER, rubisModel.getCurrentDimmerLevel() - 1);
        int progress = rubisModel.getAddServerTacticProgress();
        currentConfig.setInt(RubisModelHelper.CONFIG_ADD_SERVER_PROGRESS, progress);
        
        log("current configuration is " + currentConfig);
        
        RubisUtilityFunction utilityFunction = new RubisUtilityFunction(m_model, rubisModel);
        
        //m_adaptMgr.setDebug(true);
        StringVector tactics = m_adaptMgr.evaluate(currentConfig, env, utilityFunction, m_horizon);

        if (tactics.isEmpty()) {
        	log("no adaptation required");
        } else {
//        	at = new AdaptationTree<Strategy> (getStrategy(tactics.get(0)));
//        	at = new AdaptationTree<Strategy> (getStrategy("IncDimmer"));
        	at = new AdaptationTree<Strategy>(AdaptationExecutionOperatorT.PARALLEL);
	        for (int t = 0; t < tactics.size(); t++) {
	        	log(tactics.get(t));
	        	Strategy strategy = getStrategy(tactics.get(t)); // Strategy has tactic name
	        	at.addLeaf(strategy);
	        }
        }

        return at;
    }


}
