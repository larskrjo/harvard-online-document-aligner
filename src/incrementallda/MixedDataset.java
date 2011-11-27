package incrementallda;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;

import jgibblda.Document;
import jgibblda.LDADataset;

import javax.print.Doc;

public class MixedDataset extends LDADataset{
	public Doctype[] type;
	String[] rawdata;
	
	public MixedDataset(int i) {
		super(i);
		type = new Doctype[i];
		rawdata = new String[i];
	}
	
	public String getRawDoc(int idx) {
		return rawdata[idx];
	}
	
	/**
	 * set the document at the index idx if idx is greater than 0 and less than M
	 * @param str string contains doc
	 * @param idx index in the document array
	 */
	public void setDoc(String str, int idx){
		rawdata[idx] = str;
		if (0 <= idx && idx < M){
			String [] words = str.split("[ \\t\\n]");
			
			Vector<Integer> ids = new Vector<Integer>();
			
			for (String word : words){
				int _id = localDict.word2id.size();
				
				if (localDict.contains(word))		
					_id = localDict.getID(word);
								
				if (globalDict != null){
					//get the global id					
					Integer id = globalDict.getID(word);
					//System.out.println(id);
					
					if (id != null){
						localDict.addWord(word);
						
						lid2gid.put(_id, id);
						ids.add(_id);
					}
					else { //not in global dictionary
						//do nothing currently
					}
				}
				else {
					localDict.addWord(word);
					ids.add(_id);
				}
			}
			
			Document doc = new Document(ids, str);
			docs[idx] = doc;
			V = localDict.word2id.size();			
		}
	}

	public static MixedDataset readDataSet(String f1, String f2) {
		try {
			BufferedReader reader1 = new BufferedReader(new InputStreamReader(
					new FileInputStream(f1), "UTF-8"));
			BufferedReader reader2 = new BufferedReader(new InputStreamReader(
					new FileInputStream(f2), "UTF-8"));

			//read number of document
			String line1 = reader1.readLine();
			String line2 = reader2.readLine();
			int M1 = Integer.parseInt(line1);
			int M2 = Integer.parseInt(line2);
			MixedDataset data = new MixedDataset(M1+M2);
			for (int i = 0; i < M1; ++i){
				line1 = reader1.readLine();
				data.setDoc(line1, i);
			}
			for (int i = 0; i < M2; ++i){
				line1 = reader2.readLine();
				data.setDoc(line1, M1+i);
			}
			// shuffle the data probabilistically
			double p1 = M1 / (double) (M1+M2);
			int i = 0, j = 0, k = 0;
			Document[] temp = new Document[M1+M2];
			String[] raw = new String[M1+M2];
			while (i < M1 && j < M2) {
				if (Math.random() < p1) {
					data.type[k] = Doctype.EN;
					raw[k] = data.rawdata[i];
					temp[k++] = data.docs[i++];
				} else {
					data.type[k] = Doctype.ENSY;
					raw[k] = data.rawdata[M1+j];
					temp[k++] = data.docs[M1+(j++)];
				}
			}
			while (i < M1)
				temp[k++] = data.docs[i++];
			while (j < M2)
				temp[k++] = data.docs[M1+j++];
			for (int d = 0; d < M1+M2; d++)
				temp[d].index = d;
			data.docs = temp;
			data.rawdata = raw;
			reader1.close();
			reader2.close();

			return data;
		}
		catch (Exception e){
			System.out.println("Read Dataset Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
}
