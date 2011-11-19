package preprocess;

public class Couple implements Comparable<Couple> {
	String s;
	int i;
	public Couple(String _s, int _i) {
		s = _s;
		i = _i;
	}
	public int compareTo(Couple arg0) {
		return i - arg0.i;
	}
	public String toString() {
		return s;
	}
}
