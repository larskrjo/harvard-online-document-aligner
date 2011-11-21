package util;

public class KullBackLeibler {
	public static double divergence(double[] p, double[] q) {
		double ret = 0;
		for (int i = 0; i < p.length; i++) {
			if (p[i] > 0.0) {
				double q_i = q[i];
				if (q_i < 10e-5)
					q_i = 10e-5;
				ret += p[i] * (Math.log(p[i]) - Math.log(q[i]));
			}
		}
		return ret;
	}
	public static double sym_divergence(double[] p, double[] q) {
		double[] m = new double[p.length];
		for (int i = 0; i < p.length; i++)
			m[i] = (p[i] + q[i]) / 2.0;
		return 0.5*divergence(p, m) + 0.5*divergence(q, m);
	}
	public static void main(String[] args) {
		double[] p = {0.3, 0.5, 0.2};
		double[] q = {0.5, 0.1, 0.4};
		System.out.println(sym_divergence(p,q));
	}
}
