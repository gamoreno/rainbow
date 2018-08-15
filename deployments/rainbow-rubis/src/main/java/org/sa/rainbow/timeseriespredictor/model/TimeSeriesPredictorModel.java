package org.sa.rainbow.timeseriespredictor.model;
import java.util.Vector;

import org.sa.rainbow.core.error.RainbowException;

public class TimeSeriesPredictorModel implements Cloneable {

	private enum ObservationFeeder { AVERAGE, LAST };
	private ObservationFeeder ObservationFeederMode = ObservationFeeder.LAST;
	
	Double m_predictor;
	String m_modelArgs;
	long m_trainingLength;
	int m_horizon = -1;
	
	/**
	 * Since it is hard to synchronize observations from gauges with
	 * the evaluation period, the observations are stored here
	 * until it is time to generate the environment DTMC. At that point,
	 * the average of the observations is fed as a single observation
	 * to the predictor. That way, we get one observation per evaluation
	 * period.
	 */
	Vector<Double> m_observations = new Vector<Double>();

    /**
     * load PLADAPT wrapper
     */
    static {
        System.loadLibrary("pladapt_wrap");
      }
    
    public TimeSeriesPredictorModel (String modelArgs, long trainingLength) {
    	m_modelArgs = modelArgs;
    	m_trainingLength = trainingLength;
    }
    
    public void observe(double value) {
    	m_observations.add(Double.valueOf(value));
    }
    
    protected void feedObservations() {
    	if (!m_observations.isEmpty()) {
    		double observation = 0;
    		if (ObservationFeederMode == ObservationFeeder.AVERAGE) {
		    	double sum = 0;
		    	for (Double o : m_observations) {
		    		sum += o.doubleValue();
		    	}

		    	observation = sum / m_observations.size();
    		} else {
    			observation = m_observations.lastElement();
    		}
    		
	    	m_predictor = new Double(observation);
	    	m_observations.clear();
    	}
    }
    
    public void setHorizon(int value) throws RainbowException {
    	if (m_predictor != null) {
    		throw new RainbowException("TimeSeriesPredictorModel: attempt to set horizon after first observation");
    	}
    	m_horizon = value;
    }
    
    /**
     * Returns a one step ahead prediction
     * 
     * @return prediction, or null if no prediction can be made (no observations yet)
     */
    public Double getSimplePrediction() {
    	feedObservations();
    	if (m_predictor == null) {
    		return null; // no observations yet
    	}
    	return m_predictor;
    }

    @Override
    public Object clone () throws CloneNotSupportedException {
        throw new CloneNotSupportedException("TimeSeriesPredictorModel");
    }
}
