/**    
  * Copyright (C) 2015, Laboratorio di Valutazione delle Prestazioni - Politecnico di Milano

  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation; either version 2 of the License, or
  * (at your option) any later version.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.

  * You should have received a copy of the GNU General Public License
  * along with this program; if not, write to the Free Software
  * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
  */
package jmt.engine.math;

/**
 * An inverted version of sample statistics, used to evaluate throughput samples.
 * @author Marco Bertoli
 */
public class InverseSampleStatistics extends SampleStatistics {
	// Batching is used to group together multiple samples. In the future this could be made dynamic.
	private static final int BATCH_SIZE = 50;
	
	private double prevWeight;
	private double prevSampleXWeight;
	private double prevTotalIntervalWeights;
	private double prevMax;
	private double prevMin;
	private double prevWeigthedSum2;
	private double prevWeigthedSum3;
	private double prevWeigthedSum4;
	private double[] prevIntervalWeights;
	
	// The following variables handles batches
	private int batchSamples = 0;
	private double currentWeight = 0.0;
	private double currentSampleXWeight = 0.0;
	private double currentSimulationTime = 0.0;

	/* (non-Javadoc)
	 * @see jmt.engine.math.SampleStatistics#putNewSample(double, double, double)
	 */
	@Override
	public void putNewSample(double weight, double sample_x_weight, double simulationTime) {
		// Increment the current batch
		currentWeight += weight;
		currentSampleXWeight += sample_x_weight;
		currentSimulationTime = simulationTime;
		batchSamples++;
		
		// Process the current batch when it's full
		if (batchSamples >= BATCH_SIZE) {
			processSamples(currentWeight, currentSampleXWeight, currentSimulationTime);
			
			// Reset for the next batch
			batchSamples = 0;
			currentWeight = 0.0;
			currentSampleXWeight = 0.0;
			currentSimulationTime = 0.0;
		}	
	}

	
	public void processSamples(double weight, double sample_x_weight, double simulationTime) {
		if (weight == 0 && prevWeight == 0) {
			return;
		}
		
		//updates the simulation time
		minSimTime = Math.min(minSimTime,simulationTime);
		maxSimTime = Math.max(maxSimTime, simulationTime);
		
		//updates linear indices
		totalWeight += weight;
		weigthedSum1+= sample_x_weight;
		n++;
		
		if (weight != 0) {
			// Weight is OK, so backup previous values
			backupPreviousValues(weight, sample_x_weight);
		} else {
			// Weight 0 means that two jobs ended in the same interval. 
			// So merge it with previous value and redo computation of non-linear indices.
			prevSampleXWeight+=sample_x_weight;
			sample_x_weight = prevSampleXWeight;
			weight = prevWeight;
			
			// Restore backup of previous non-linear quantities. They will be re-computed.
			max = prevMax;
			min = prevMin;
			weigthedSum2 = prevWeigthedSum2;
			weigthedSum3 = prevWeigthedSum3;
			weigthedSum4 = prevWeigthedSum4;
			if (prevIntervalWeights != null) {
				System.arraycopy(prevIntervalWeights, 0, intervalWeights, 0, prevIntervalWeights.length);
				totalIntervalWeights = prevTotalIntervalWeights;
			}
		} 
		double sample = sample_x_weight / weight;
		
		// Formulas are a bit "strange" because we are inverting measure sample by sample.
		max= Math.max(max, sample);
		min = Math.min(min, sample);
		
		// updates the sum of weights multiplied by appropriate power of samples of the sequence
		weigthedSum2+= Math.pow(sample_x_weight,2) / weight;
		weigthedSum3+= Math.pow(sample_x_weight,3) / Math.pow(weight,2);
		weigthedSum4+= Math.pow(sample_x_weight,4) / Math.pow(weight,3);
		
		// Updates intervals if enabled
		if (intervalWeights != null && sample >= intervalMin && sample <= intervalMax) {
			int index = (int) Math.floor((sample - intervalMin)/intervalWidth);

			// The following avoids that due to numerical rounding, some samples exceed last bucket.
			if (index >= intervalWeights.length) {
				index = intervalWeights.length - 1;
			}
			intervalWeights[index] += weight;
			totalIntervalWeights += weight;
		}
				
		//calculates the statistical parameters until that point
		calcStatistics();
	}
	
	
	
	/* (non-Javadoc)
	 * @see jmt.engine.math.SampleStatistics#initializeDistrubution(double, double, int)
	 */
	@Override
	public void initializeDistrubution(double intervalMin, double intervalMax,
			int intervals) {
		super.initializeDistrubution(intervalMin, intervalMax, intervals);
		prevIntervalWeights = new double[intervalWeights.length];
	}



	/**
	 * Backup previous samples and non-linear indices
	 * @param sample the sample to accumulate
	 * @param weight the weight of the sample
	 */
	private void backupPreviousValues(double sample, double weight) {
		prevWeight = sample;
		prevSampleXWeight = weight;
		prevMax = max;
		prevMin = min;
		prevWeigthedSum2 = weigthedSum2;
		prevWeigthedSum3 = weigthedSum3;
		prevWeigthedSum4 = weigthedSum4;
		if (intervalWeights != null) {
			System.arraycopy(intervalWeights, 0, prevIntervalWeights, 0, intervalWeights.length);
			prevTotalIntervalWeights = totalIntervalWeights;
		}
	}


	/* (non-Javadoc)
	 * @see jmt.engine.math.SampleStatistics#finalizeLoading()
	 */
	@Override
	public void finalizeLoading() {
		// Process the last batch
		if (batchSamples > 0) {
			processSamples(currentWeight, currentSampleXWeight, currentSimulationTime);
		}	
		super.finalizeLoading();
	}	
	
	
}
