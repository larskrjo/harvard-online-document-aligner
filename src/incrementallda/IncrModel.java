package incrementallda;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import jgibblda.LDACmdOption;
import jgibblda.LDADataset;
import jgibblda.Model;
import jgibblda.Pair;

public class IncrModel extends Model {
	int basis_size; // basis size
	int batch_size; // batch size
	int inf;
	int sup;
	public MixedDataset data;
	
	public IncrModel(int _N, int _m) {
		super();
		setBasisSize(_N);
		setBatchSize(_m);
		sup = basis_size-1-batch_size;
	}
	
	public void setBasisSize(int _N) {
		basis_size = _N;
	}
	public void setBatchSize(int _m) {
		batch_size = _m;
	}
	public boolean hasNextBatch() {
		return sup < M-1;
	}
	public void nextBatch() {
		int inf_old = inf;
		int sup_old = sup;
		sup = sup + batch_size;
		if (sup > M-1)
			sup = M-1;
		inf = sup - basis_size + 1;
		// suppress counts in beginning of basis
		for (int m = inf_old; m < inf; m++) {
			for (int n = 0; n < data.docs[m].length; n++) {
				int topic = z[m].get(n);
				int w = data.docs[m].words[n];
				nd[m][topic] -= 1;
				nw[w][topic] -= 1;
				nwsum[topic] -= 1;
			}
			ndsum[m] -= data.docs[m].length;
		}
		// add new counts at end of basis
		for (int m = sup_old + 1; m <= sup; m++) {
			int N = data.docs[m].length;
			z[m] = new Vector<Integer>();

			for (int n = 0; n < N; n++){
				int topic = (int)Math.floor(Math.random() * K);
				z[m].add(topic);
				// number of instances of word assigned to topic j
				nw[data.docs[m].words[n]][topic] += 1;
				// number of words in document i assigned to topic j
				nd[m][topic] += 1;		
				nwsum[topic] += 1;
			}
			// total number of words in document i
			ndsum[m] = N;
		}
	}
	
	/**
	 * Init parameters for estimation
	 */
	public boolean initNewModel(LDACmdOption option){
		if (!init(option))
			return false;
		
		int m, n, w, k;		
		p = new double[K];		
		
		data = MixedDataset.readDataSet(dir + File.separator + dfile, dir + File.separator + option.dfile2);
		super.data = data;
		if (data == null){
			System.out.println("Fail to read training data!\n");
			return false;
		}
		
		//+ allocate memory and assign values for variables		
		M = data.M;
		V = data.V;
		dir = option.dir;
		savestep = option.savestep;
		
		// K: from command line or default value
	    // alpha, beta: from command line or default values
	    // niters, savestep: from command line or default values

		nw = new int[V][K];
		for (w = 0; w < V; w++){
			for (k = 0; k < K; k++){
				nw[w][k] = 0;
			}
		}
		
		nd = new int[M][K];
		for (m = 0; m < M; m++){
			for (k = 0; k < K; k++){
				nd[m][k] = 0;
			}
		}
		
		nwsum = new int[K];
		for (k = 0; k < K; k++){
			nwsum[k] = 0;
		}
		
		ndsum = new int[M];
		for (m = 0; m < M; m++){
			ndsum[m] = 0;
		}
		
		z = new Vector[M];
		for (m = 0; m < basis_size - batch_size; m++){
			int N = data.docs[m].length;
			z[m] = new Vector<Integer>();
			
			//initilize for z
			for (n = 0; n < N; n++){
				int topic = (int)Math.floor(Math.random() * K);
				z[m].add(topic);
				
				// number of instances of word assigned to topic j
				nw[data.docs[m].words[n]][topic] += 1;
				// number of words in document i assigned to topic j
				nd[m][topic] += 1;
				
				nwsum[topic] += 1;
			}
			// total number of words in document i
			ndsum[m] = N;
		}
		
		theta = new double[M][K];		
		phi = new double[K][V];
		
		return true;
	}
	
	/**
	 * Save model the most likely words for each topic
	 */
	public boolean saveModelTwords(String filename){
		try{
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename), "UTF-8"));
			
			if (twords > V){
				twords = V;
			}
			
			for (int k = 0; k < K; k++){
				List<Pair> wordsProbsList = new ArrayList<Pair>(); 
				for (int w = 0; w < V; w++){
					Pair p = new Pair(w, phi[k][w], false);
					
					wordsProbsList.add(p);
				}//end foreach word
				
				//print topic				
				writer.write("Topic " + k + "th:\n");
				Collections.sort(wordsProbsList);
				
				for (int i = 0; i < twords; i++){
					if (data.localDict.contains((Integer)wordsProbsList.get(i).first)){
						String word = data.localDict.getWord((Integer)wordsProbsList.get(i).first);
						
						writer.write("\t" + word + " " + wordsProbsList.get(i).second + "\n");
					}
				}
			} //end foreach topic			
						
			writer.close();
		}
		catch(Exception e){
			System.out.println("Error while saving model twords: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Save model
	 */
	public boolean saveModel(String modelName){
		if (!saveModelTwords(dir + File.separator + modelName + twordsSuffix))
				return false;
		return true;
	}

	public int[] getFlatNw() {
		int[] ret = new int[V*K];
		int k = 0;
		for (int i = 0; i < V; i++)
			for (int j = 0; j < K; j++)
				ret[k++] = nw[i][j];
		return ret;
	}

	public int[] getNwSum() {
		return nwsum;
	}

	public int[] getFlatNd() {
		int[] ret = new int[M*K];
		int k = 0;
		for (int i = 0; i < M; i++)
			for (int j = 0; j < K; j++)
				ret[k++] = nd[i][j];
		return ret;
	}

	public int[] getNdSum() {
		return ndsum;
	}

	public Object[] getZ() {
		Object[] ret = new Object[M];
		for (int i = 0; i < M; i++)
			ret[i] = (Object) z[i];
		return ret;
	}
	
	
}
