import mpi.MPI;
import incrementallda.IncrEstimator;
import jgibblda.LDACmdOption;


public class MPIODA {

	public static void main(String[] args) {
		MPI.Init(args);
		int rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		int root = 0;
		System.out.println("Hello from process " + rank + "/" + size);
		IncrEstimator estimator;
		int[][] nd_p;
		int[][] nw;
		int[] nwsum;
		int[] ndsum_p;
		Object[] recvObj = new Object[2];
		if (rank == root) {
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
			estimator = new IncrEstimator();
			estimator.init(option, basis_size, batch_size);
			nd_p = estimator.trnModel.getInitNd();
			nw = estimator.trnModel.nw;
			ndsum_p = estimator.trnModel.getInitNdSum();
			nwsum = estimator.trnModel.nwsum;
			recvObj[0] = nw;
			recvObj[1] = nwsum;
		}
		MPI.COMM_WORLD.Bcast(recvObj, 0, 1, MPI.OBJECT, root);
		nw = (int[][]) recvObj[0];
		nwsum = (int[]) recvObj[1];
		MPI.Finalize();
	}

}


