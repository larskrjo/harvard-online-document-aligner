package preprocess;

import java.util.Scanner;

public class EditDistance {
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		while (in.hasNextLine()) {
			String[] l = in.nextLine().split("\t");
			int ed = edit_distance(l[0].trim().toCharArray(), l[1].trim().toCharArray());
			System.out.println(l[0] + "\t" + l[1] + "\t" + 2.0*ed / (double) (l[0].length() + l[1].length()));
		}
	}
    private static int minimum(int a, int b, int c) {
            return Math.min(Math.min(a, b), c);
    }

    public static int edit_distance(char[] str1,
    		char[] str2) {
    	int[][] distance = new int[str1.length + 1][str2.length + 1];

    	for (int i = 0; i <= str1.length; i++)
    		distance[i][0] = i;
    	for (int j = 0; j <= str2.length; j++)
    		distance[0][j] = j;

    	for (int i = 1; i <= str1.length; i++)
    		for (int j = 1; j <= str2.length; j++)
    			distance[i][j] = minimum(
    					distance[i - 1][j] + 1,
    					distance[i][j - 1] + 1,
    					distance[i - 1][j - 1]
    					                + ((str1[i - 1] == str2[j - 1]) ? 0
    					                		: 1));
    	return distance[str1.length][str2.length];
    }
	public static double normDist(String s, String s2) {
		int ed = edit_distance(s.trim().toCharArray(), s2.trim().toCharArray());
		return (2.0*ed / (double) (s.length() + s2.length()));
	}
}