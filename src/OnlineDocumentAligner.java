import incrementallda.IncrEstimator;
import jgibblda.LDACmdOption;


public class OnlineDocumentAligner {

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: java OnlineDocumentAligner dir en.bag ensy.bag");
			System.exit(-1);
		}
		LDACmdOption option = new LDACmdOption();
		option.est = true;
		option.inf = false;
		option.niters = 100;
		option.K = 10;
		option.dir = args[0];
		option.dfile = args[1];
		option.dfile2 = args[2];
		option.twords = 10;
		int basis_size = 1000;
		int batch_size = 50;
		IncrEstimator estimator = new IncrEstimator();
		estimator.init(option, basis_size, batch_size);
		estimator.estimate();
	}

}