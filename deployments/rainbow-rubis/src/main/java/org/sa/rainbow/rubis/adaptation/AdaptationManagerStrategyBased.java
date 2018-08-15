package org.sa.rainbow.rubis.adaptation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.sa.rainbow.core.Rainbow;
import org.sa.rainbow.core.RainbowComponentT;
import org.sa.rainbow.core.RainbowConstants;
import org.sa.rainbow.core.adaptation.AdaptationTree;
import org.sa.rainbow.model.acme.rubis.RubisModelHelper;
import org.sa.rainbow.stitch.core.Strategy;
import org.sa.rainbow.stitch.visitor.Stitch;
import org.sa.rainbow.util.Util;

public class AdaptationManagerStrategyBased extends AdaptationManagerBase {
	private static final String NO_OP_STRATEGY = "NoOp";
	private static final String PRISM_TEMPLATE = "rainbow.adaptation.plasb.prismTemplate";
	private static final String PRISM_RESULT_PREFIX = "Result:";
	private static final String ENVIRONMENT_TAG = "//#environment";
	private static final String INIT_TAG = "//#init";

	public AdaptationManagerStrategyBased() {
		// TODO Auto-generated constructor stub
	}
	
    protected void initializeAdaptationMgr(RubisModelHelper rubisModel) {
        log("Starting SB-PLA Adaptation Manager initialization");
        super.initializeAdaptationMgr(rubisModel);
        log("SB-PLA Adaptation Manager Initialized");
    }
	

	/**
	 * Generates a complete PRISM model injecting the environment and the
	 * initialization sections
	 * 
	 * @throws IOException
	 */
	protected File getPrismModelFile(String strategy, String initPRISM, String envPRISM) throws IOException {
		File stitchPath = Util.getRelativeToPath(Rainbow.instance().getTargetPath(),
				Rainbow.instance().getProperty(RainbowConstants.PROPKEY_SCRIPT_PATH));
		File templatePath = new File(stitchPath,  Rainbow.instance().getProperty(PRISM_TEMPLATE));
		if (!templatePath.exists()) {
			return null;
		}

		File strategyModel = new File(stitchPath, strategy + ".prism");
		if (!strategyModel.exists()) {
			return null;
		}

		BufferedReader in = new BufferedReader(new FileReader(templatePath));
		File modelPath = new File(stitchPath, strategy + "-complete.prism");
		FileWriter out = new FileWriter(modelPath);

		String line;
		while ((line = in.readLine()) != null) {
			if (line.startsWith(ENVIRONMENT_TAG)) {
				out.write(envPRISM);
			} else if (line.startsWith(INIT_TAG)) {
				out.write(initPRISM);
			} else {
				out.write(line);
			}
			out.write('\n');
		}
		in.close();
		
		// append strategy PRISM model
		BufferedReader strategyIn = new BufferedReader(new FileReader(strategyModel));
		while ((line = strategyIn.readLine()) != null) {
			out.write(line);
			out.write('\n');
		}
		strategyIn.close();

		out.close();
		return modelPath;
	}

	/**
	 * Invokes PRISM to evaluate a strategy
	 * @throws RuntimeException
	 */
	protected double evaluateStrategy(String strategy, String initPRISM, String envPRISM) throws RuntimeException {
		boolean hasResult = false;
		double result = 0;
		try {
			File modelPath = getPrismModelFile(strategy, initPRISM, envPRISM);
			if (modelPath != null && modelPath.exists()) {
				ArrayList<String> command = new ArrayList<String>();
				command.add("prism");
				command.add(modelPath.getPath());
				command.add("-pctl");
				command.add("R=? [ F \"final\" ]");
				command.add("-ex");
				String[] commandArray = command.toArray(new String[command.size()]);
				Process p = Runtime.getRuntime().exec(commandArray);
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = input.readLine()) != null) {
					if (line.startsWith(PRISM_RESULT_PREFIX)) {
						result = Double.parseDouble(line.substring(PRISM_RESULT_PREFIX.length()).trim().split(" ")[0]);
						hasResult = true;
					}
				}
				input.close();
				//modelPath.delete();
			}
		} catch (Exception e) {
			m_reportingPort.error(RainbowComponentT.ADAPTATION_MANAGER, e.toString());
		}

		if (!hasResult) {
			throw new RuntimeException("Could not get value of strategy " + strategy);
		}

		return result;
	}

	protected AdaptationTree<Strategy> checkAdaptationImpl(RubisModelHelper rubisModel) {
		AdaptationTree<Strategy> at = null;
        Map<String, Strategy> applicableStrategies = new HashMap<String, Strategy> ();
        for (Stitch stitch : m_repertoire) {
            if (!stitch.script.isApplicableForSystem (m_model)) {
                m_reportingPort.trace (getComponentType (), "x. skipping " + stitch.script.getName ());
                continue; // skip checking this script
            }
            for (Strategy strategy : stitch.script.strategies) {
                // check condition of Strategy applicability
                if (strategy.isApplicable (new HashMap<String, Object>())) {
                    applicableStrategies.put (strategy.getName(), strategy);
                }
            }
        }
        if (applicableStrategies.size () == 0) { // can't do adaptation
            log ("No applicable Strategies to do an adaptation!");
            return null;
        }
        
        // add NoOp strategy
        applicableStrategies.put(NO_OP_STRATEGY, null);

		String bestStrategy = NO_OP_STRATEGY; // default to not adapting
		double bestValue = -Double.MAX_VALUE;
		for (String strategy : applicableStrategies.keySet()) {
			double value = 1;
			log("**** value of " + strategy + " = " + value);
			if (value > bestValue) {
				bestStrategy = strategy;
				bestValue = value;
			}
		}
		log("Selected strategy " + bestStrategy);
		Strategy selectedStrategy = applicableStrategies.get(bestStrategy);
		if (selectedStrategy != null) { // it's not NoOp
			at = new AdaptationTree<Strategy> (selectedStrategy);
		}
		
		return at;
	}

	protected String generateInitBlock(RubisModelHelper rubisModel) {
		StringBuilder init = new StringBuilder();
		init.append("const int TAddServer_LATENCY = ");
		init.append(rubisModel.getAddServerLatencyPeriods());
		
		init.append(";\nconst int HORIZON = ");
		init.append(m_horizon);
		
		init.append(";\nconst double PERIOD = ");
		double periodMsec = Double
				.parseDouble(Rainbow.instance().getProperty(RainbowConstants.PROPKEY_MODEL_EVAL_PERIOD));
		init.append(periodMsec / 1000);
		
		init.append(";\nconst int DIMMER_LEVELS = ");
		init.append(rubisModel.getDimmerLevels());
		
		init.append(";\nconst double DIMMER_MARGIN = ");
		init.append(rubisModel.getDimmerMargin());
		
		init.append(";\nconst int MAX_SERVERS = ");
		init.append(rubisModel.getMaxServers());
		
		init.append(";\nconst double RT_THRESHOLD = ");
		init.append(rubisModel.getRTThreshold());
		
		init.append(";\nconst int ini_servers = ");
		init.append(rubisModel.getNumActiveServers());
		
		init.append(";\nconst int ini_dimmer = ");
		init.append(rubisModel.getCurrentDimmerLevel());
		
		init.append(";\nconst double serviceTimeMean = ");
		init.append(rubisModel.getEstimatedOptServiceTime());
		
		init.append(";\nconst double serviceTimeVariance = ");
		init.append(rubisModel.getEstimatedOptServiceTimeVariance());
		
		init.append(";\nconst double lowServiceTimeMean = ");
		init.append(rubisModel.getEstimatedBasicServiceTime());
		
		init.append(";\nconst double lowServiceTimeVariance = ");
		init.append(rubisModel.getEstimatedBasicServiceTimeVariance());
		
		init.append(";\nconst int threads = ");
		init.append(rubisModel.getNumThreadsPerServer());
		init.append(";\n");
		return init.toString();
	}

}
