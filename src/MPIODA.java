import java.io.File;
import java.util.Vector;

import util.KullBackLeibler;

import mpi.MPI;
import mpi.MPIException;
import incrementallda.IncrEstimator;
import incrementallda.MixedDataset;
import jgibblda.Document;
import jgibblda.LDACmdOption;


public class MPIODA {

	static int phase;
	static int[] nw, nwsum, nw_p, nwsum_p;
	static int[][][] nd_p; // phases * batch * nbr_wor_in_doc
	static int[][] ndsum_p;
	static Vector<Integer>[][] z_p; // z[i][d] : assignment vector for the d^th document in i^th phase
	static Document[][] data_p;
	static int K = 10;
	static int V, M;
	static int basis_size = 1000;
	static int batch_size = 100;
	static double alpha = 50.0 / K;
	static double beta = 0.1;
	static double[] p;
	static int[] indices; // indices of the current document for each process
	static int rank;
	
	public static void main(String[] args) {
		MPI.Init(args);
		rank = MPI.COMM_WORLD.Rank();
		int size = MPI.COMM_WORLD.Size();
		int root = 0;
		MixedDataset dataset = null;

		int[] parameters = new int[2];
		int[] nd = null;
		int[] ndsum = null;
		int niters = 10;
		Object[] data = null;
		double[] theta_all = null;
		int[] indices_all = null;
		if (rank == root) {
			String dir = "/Users/edouardgodfrey/work/Online-Document-Aligner/corpus/lda";
			String dfile = "en_2005_02.bag";
			String dfile2 = "ensy_2005_02.bag";
			dataset = MixedDataset.readDataSet(dir + File.separator + dfile, dir + File.separator + dfile2);
			parameters[0] = dataset.V;
			parameters[1] = dataset.M;
			data = dataset.docs;
			theta_all = new double[basis_size*K];
			indices_all = new int[basis_size];
		}
		// broadcast parameters
		MPI.COMM_WORLD.Bcast(parameters, 0, 2, MPI.INT, root);
		V = parameters[0];
		M = parameters[1];
		
		int num_batch = (M - (basis_size - batch_size)) / batch_size;
		int phase_size = M / basis_size;

		nd_p = new int[phase_size][batch_size][K];
		ndsum_p = new int[phase_size][batch_size];
		z_p = new Vector[phase_size][batch_size];
		data_p = new Document[phase_size][batch_size];
		nw = new int[V*K];
		nwsum = new int[K];
		p = new double[K];
		
		// scatter data
		for (int phase = 0; phase < phase_size; phase++) {
			Object[] data_p_local = new Object[batch_size];
			MPI.COMM_WORLD.Scatter(data, phase*basis_size, batch_size, MPI.OBJECT, data_p_local, 0, batch_size, MPI.OBJECT, root);
			for (int i = 0; i < batch_size; i++)
				data_p[phase][i] = (Document) data_p_local[i];
		}
		
		// initialize
		phase = 0;
		nw_p = new int[V*K];
		nwsum_p = new int[K];
		initialSample(true);
		indices = computeIndices();
		MPI.COMM_WORLD.Allreduce(nw_p, 0, nw, 0, V*K, MPI.INT, MPI.SUM);
		MPI.COMM_WORLD.Allreduce(nwsum_p, 0, nwsum, 0, K, MPI.INT, MPI.SUM);
		
		// start estimating
		boolean start = true;
		// process taking care of the last batch of data
		int current_last = size-1;
		for (int batch = 0; batch < num_batch; batch++) {
			if (rank == root)
				System.out.println("Processing batch " + batch);
			
			// sample
			
			for (int iter = 0; iter < (start ? niters*10 : niters); iter++) {
				//System.out.println("hello from " + rank);
				if (rank == root && iter % 10 == 0)
					System.out.print(".");
				nw_p = new int[V*K];
				nwsum_p = new int[K];
				//if (rank == root)
					//System.out.println("Sampling");
				for (int m = 0; m < batch_size; m++){				
					for (int n = 0; n < z_p[phase][m].size(); n++){
						// z_i = z_p[phase][m][n]
						// sample from p(z_i|z_-i, w)
						int topic = sample(m,n);
						z_p[phase][m].set(n, topic);
					}// end for each word
				}// end for each document
				
				// if it is the last iteration and then only moving process gets
				// nw and nwsum
				if (iter % 1 == 0 || iter == (start ? niters*10 : niters) - 1) {
					//if (rank == root)
						//System.out.println("Reducing");
					if (iter == (start ? niters*10 : niters) - 1) {
						if (batch % size == rank) {
							nw_p = new int[V*K];
							nwsum_p = new int[K];
						}
						MPI.COMM_WORLD.Reduce(nw_p, 0, nw, 0, V*K, MPI.INT, MPI.SUM, batch%size);
						MPI.COMM_WORLD.Reduce(nwsum_p, 0, nwsum, 0, K, MPI.INT, MPI.SUM, batch%size);
					} else {
						// update nw, nwsum
						MPI.COMM_WORLD.Allreduce(nw_p, 0, nw, 0, V*K, MPI.INT, MPI.SUM);
						MPI.COMM_WORLD.Allreduce(nwsum_p, 0, nwsum, 0, K, MPI.INT, MPI.SUM);
					}
				}
			}
			
			// compute best matches for the current batch
			double[] theta = computeTheta();
			MPI.COMM_WORLD.Gather(theta, 0, K*batch_size, MPI.DOUBLE, theta_all, 0, K*batch_size, MPI.DOUBLE, root);
			MPI.COMM_WORLD.Gather(indices, 0, batch_size, MPI.INT, indices_all, 0, batch_size, MPI.INT, root);
			if (rank == root) {
				double[][] theta2d = new double[basis_size][K]; // 2d representation of theta_all
				for (int i = 0; i < basis_size; i++) {
					for (int k = 0; k < K; k++) {
						theta2d[i][k] = theta_all[k+i*K];
					}
				}
				int start_q = start ? 0 : batch_size*current_last;
				int end_q = start ? basis_size : batch_size*(current_last+1);
				for (int q = start_q; q < end_q; q++) {
					double min_div = Double.MAX_VALUE;
					int best = -1;
					for (int c = 0; c < basis_size; c++) {
						if (c == q)
							continue;
						if (dataset.type[indices_all[c]] == dataset.type[indices_all[q]])
							continue;
						double js_div = KullBackLeibler.sym_divergence(theta2d[c], theta2d[q]);
						if (js_div < min_div) {
							min_div = js_div;
							best = c;
						}
					}
					if (min_div < 0.005) {
						System.out.println("======================================");
						System.out.println("Score for (" + indices_all[q] + ", " + indices_all[best] + ") = " + min_div);
						System.out.println("--------------------------------------");
						System.out.println(dataset.getRawDoc(indices_all[q]));
						System.out.println("--------------------------------------");
						System.out.println(dataset.getRawDoc(indices_all[best]));
						System.out.println("======================================");
					}
				}
				
			}

			// reassign the oldest process to the new batch
			if (batch % size == rank) {
				phase++;
				initialSample(false);
				indices = computeIndices();
				System.out.println("Process " + rank + " takes the lead");
			}
			MPI.COMM_WORLD.Bcast(nw, 0, V*K, MPI.INT, batch%size);
			
			
			current_last = (current_last + 1) % size;
			start = false;
		}

		MPI.Finalize();
	}

	private static int[] computeIndices() {
		int[] ret = new int[batch_size];
		for (int i = 0; i < batch_size; i++) {
			ret[i] = data_p[phase][i].index;
			//System.out.println("a:" + data_p[phase][i].index + "; phase " + phase + "; " + rank);
		}
		return ret;
	}

	private static void initialSample(boolean start) {
		for (int m = 0; m < batch_size; m++){
			int N = data_p[phase][m].length;
			z_p[phase][m] = new Vector<Integer>();
			
			//initiliaze for z_p
			for (int n = 0; n < N; n++){
				int topic = (int)Math.floor(Math.random() * K);
				z_p[phase][m].add(topic);
				
				// number of instances of word assigned to topic j
				int w = data_p[phase][m].words[n];
				if (start)
					nw_p[topic*V + w] += 1;
				else
					nw[topic*V + w] += 1;
				// number of words in document i assigned to topic j
				nd_p[phase][m][topic] += 1;
				
				if (start)
					nwsum_p[topic] += 1;
				else
					nwsum[topic] += 1;
			}
			// total number of words in document i
			ndsum_p[phase][m] = N;
		}
	}
	

	public static int sample(int m, int n){
		// remove z_i from the count variable
		int topic = z_p[phase][m].get(n);
		int w = data_p[phase][m].words[n];

		nw[w+topic*V] -= 1;
		nd_p[phase][m][topic] -= 1;
		nwsum[topic] -= 1;
		ndsum_p[phase][m] -= 1;
		
		double Vbeta = V * beta;
		double Kalpha = K * alpha;
		
		//do multinominal sampling via cumulative method
		for (int k = 0; k < K; k++) {
			p[k] = (nw[w+k*V] + beta)/(nwsum[k] + Vbeta) *
					(nd_p[phase][m][k] + alpha)/(ndsum_p[phase][m] + Kalpha);
		}
		
		// cumulate multinomial parameters
		for (int k = 1; k < K; k++){
			p[k] += p[k - 1];
		}
		
		// scaled sample because of unnormalized p[]
		double u = Math.random() * p[K - 1];
		
		for (topic = 0; topic < K; topic++){
			if (p[topic] > u) //sample topic w.r.t distribution p
				break;
		}
		
		// add newly estimated z_i to count variables
		nw[w+V*topic] += 1;
		nd_p[phase][m][topic] += 1;
		nwsum[topic] += 1;
		ndsum_p[phase][m] += 1;
		nw_p[w+V*topic] += 1;
		nwsum_p[topic] += 1;
		
 		return topic;
	}

	
	public static double[] computeTheta(){
		double[] ret = new double[batch_size*K];
		for (int m = 0; m < batch_size; m++){
			for (int k = 0; k < K; k++){
				ret[m*K+k] = (nd_p[phase][m][k] + alpha) / (ndsum_p[phase][m] + K * alpha);
			}
		}
		return ret;
	}
	
}


