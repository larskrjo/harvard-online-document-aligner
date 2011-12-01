package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Util {
	public static String[] loadIndexFile(String filename) throws FileNotFoundException {
		Scanner in = new Scanner(new File(filename));
		int n = Integer.parseInt(in.nextLine());
		String[] ret = new String[n];
		for (int i = 0; i < n; i++)
			ret[i] = in.nextLine();
		return ret;
	}
}
