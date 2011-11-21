package incrementallda;

import java.io.File;

import util.KullBackLeibler;

import jgibblda.Conversion;
import jgibblda.Estimator;
import jgibblda.LDACmdOption;

public class IncrEstimator extends Estimator {
	public IncrModel trnModel;
	public boolean init(LDACmdOption option, int basis, int batch) {
		this.option = option;
		trnModel = new IncrModel(basis, batch);
		super.trnModel = trnModel;
		if (option.est){
			if (!trnModel.initNewModel(option))
				return false;
			trnModel.data.localDict.writeWordMap(option.dir + File.separator + option.wordMapFileName);
		}
		else if (option.estc){
			if (!trnModel.initEstimatedModel(option))
				return false;
		}

		return true;
	}

	public void estimate(){
		int i = 0;
		boolean start = true;
		int n_iter, start_lookup;
		while (trnModel.hasNextBatch()) {
			trnModel.nextBatch();
			System.out.println("Processing batch " + i);
			if (start) {
				n_iter = 10*trnModel.niters;
				start_lookup = 0;
				start = false;
			} else {
				n_iter = trnModel.niters;
				start_lookup = trnModel.sup - trnModel.batch_size + 1;
			}
			for (int iter = 0; iter < n_iter; iter++){
				System.out.print(".");

				// for all z_i
				for (int m = trnModel.inf; m <= trnModel.sup; m++){				
					for (int n = 0; n < trnModel.data.docs[m].length; n++){
						// z_i = z[m][n]
						// sample from p(z_i|z_-i, w)
						int topic = sampling(m, n);
						trnModel.z[m].set(n, topic);
					}// end for each word
				}// end for each document
			}
			System.out.print("\n");

			computeTheta();
			// compute the best matches
			for (int m = start_lookup; m <= trnModel.sup; m++) {
				double min_div = Double.MAX_VALUE;
				int best = -1;
				for (int m2 = trnModel.inf; m2 <= trnModel.sup; m2++) {
					if (m == m2)
						continue;
					if (trnModel.data.type[m] == trnModel.data.type[m2])
						continue;
					double js_div = KullBackLeibler.sym_divergence(trnModel.theta[m], trnModel.theta[m2]);
					if (js_div < min_div) {
						min_div = js_div;
						best = m2;
					}
				}
				if (min_div < 0.005) {
					System.out.println("======================================");
					System.out.println("Score for (" + m + ", " + best + ") = " + min_div);
					System.out.println("--------------------------------------");
					System.out.println(trnModel.data.getRawDoc(m));
					System.out.println("--------------------------------------");
					System.out.println(trnModel.data.getRawDoc(best));
					System.out.println("======================================");
				}
			}

			computePhi();
			//System.out.println("Saving the model at iteration " + trnModel.liter + " ...");
			trnModel.saveModel("model-" + Conversion.ZeroPad(i, 5));

			i++;
		}// end iterations		
		computeTheta();
		computePhi();
		trnModel.saveModel("model-final");
	}
	
	public void computeTheta(){
		for (int m = trnModel.inf; m <= trnModel.sup; m++){
			for (int k = 0; k < trnModel.K; k++){
				trnModel.theta[m][k] = (trnModel.nd[m][k] + trnModel.alpha) / (trnModel.ndsum[m] + trnModel.K * trnModel.alpha);
			}
		}
	}
}
