package preprocess;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Scanner;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class DocumentSeparator {

	public static void main(String[] args) throws JDOMException, IOException {
		if (args.length < 2) {
			System.err.println("Usage: java DocumentSeparator fileLg1 fileFormat (fileLg2)");
			System.exit(-1);
		}
		SAXBuilder builder = new SAXBuilder();
		FileInputStream fis = new FileInputStream(args[1]);
		InputStreamReader isr = new InputStreamReader(fis, "UTF8");
		Document document = builder.build(isr);
		boolean thirdFile = (args.length > 2);
		Scanner in2 = null;
		if (thirdFile)
			in2 = new Scanner(new File(args[2]), "UTF8");

		Element root = document.getRootElement();
		Scanner in = new Scanner(new File(args[0]), "UTF8");
		System.out.println("<root>");
		List docs = root.getChildren("DOC");
		for (int i = 0; i < docs.size(); i++) {
			String doc_id = ((Element) docs.get(i)).getAttribute("id").getValue();
			if (! ((Element) docs.get(i)).getAttribute("type").getValue().equals("story")) {
				//System.err.println(((Element) docs.get(i)).getAttribute("type").getValue());
				continue;
			}
			//System.err.println(((Element) docs.get(i)).getAttribute("type").getValue());
			Element doc_text = ((Element) docs.get(i)).getChild("TEXT");
			List paragraphs = doc_text.getChildren("P");
			StringBuffer txt2 = new StringBuffer();
			for (int j = 0; j < paragraphs.size(); j++) {
				txt2.append(((Element) paragraphs.get(j)).getText() + " ");
			}
			String txt = doc_text.getText();
			if (txt2.length() > txt.length())
				txt = txt2.toString();
			if (txt.trim().length() == 0)
				continue;
			txt = txt.trim();
			txt = txt.replace('\n', ' ');
			txt = txt.replaceAll(" [ ]*", " ");
			//if (txt.substring(txt.length()-9).equals("2829 6281"))
				//System.err.println("erreur");
			int k = 0;
			System.out.println("<DOC id=\"" + doc_id + "\">");
			while (in.hasNextLine()) {
				k++;
				String l = in.nextLine().replaceAll(" [ ]*", " ");
				if (thirdFile)
					System.out.println(in2.nextLine().replaceAll(" [ ]*", " "));
				else
					System.out.println(l);
				//if (l.startsWith("He waits for the proper moment"))
					//System.err.println("error");
				int n = l.length();
				if (n < 4)
					continue;
				int start = Math.max(txt.length()-n, 0);
				String query = txt.substring(start);
				if (k > 200) {
					
					System.err.println("--------------------------------------");
					System.err.println("Problem: can't match query " + query);
					System.err.println("--------------------------------------");
					
				}
				/*
				System.err.println("=================================");
				System.err.println(query);
				System.err.println("---------------------------------");
				System.err.println(l);
				System.err.println("---------------------------------");
				System.err.println(EditDistance.normDist(query, l));
				System.err.println("=================================");
				*/
				if (EditDistance.normDist(query, l) <= 0.4) {
					//System.err.println("Match");
					break;
				}		
			}
			System.out.println("</DOC>");
		}
		System.out.println("</root>");
	}

	private static boolean match(String cand, String query) {
		System.err.println("cand: " + cand);
		if (cand.length() != query.length())
			return false;
		if (cand.equals(query))
			return true;
		char[] cand_c = cand.toCharArray();
		char c = cand_c[cand_c.length-1];
		cand_c[cand_c.length-1] = cand_c[cand_c.length-2];
		cand_c[cand_c.length-2] = c;
		if (new String(cand_c).equals(query))
			return true;
		System.err.println(EditDistance.normDist(cand, query));
		if (EditDistance.normDist(cand, query)  <= 0.4)
			return true;
		return false;
	}

}