package preprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

public class Counts {
	HashMap<String, Integer> counts;
	public static void main(String[] args) {
		if (args.length != 0) {
			System.err.println("Usage: java Counts");
			System.exit(-1);
		}
		Scanner in = new Scanner(System.in);
		Counts c = new Counts();
		while (in.hasNextLine()) {
			String[] l = in.nextLine().split(" ");
			for (String s : l)
				c.add(s);
		}
		for (Couple e : c.orderedList()) {
			System.out.println(e);
		}
	}
	public Counts() {
		counts = new HashMap<String, Integer>();
	}
	public void add(String s) {
		if (! counts.containsKey(s))
			counts.put(s, 0);
		counts.put(s, counts.get(s)+1);
	}
	public List<Couple> orderedList() {
		ArrayList<Couple> ret = new ArrayList<Couple>();
		for (Entry<String, Integer> e : counts.entrySet())
			ret.add(new Couple(e.getKey(), e.getValue()));
		Collections.sort(ret);
		return ret;
	}
}
