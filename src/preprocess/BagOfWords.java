package preprocess;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;



public class BagOfWords {
	public static void main(String[] args) throws JDOMException, IOException {
		if (args.length < 2) {
			System.err.println("Usage: java BagOfWords input_en stopwords");
			System.exit(-1);
		}
		SAXBuilder builder = new SAXBuilder();
		FileInputStream fis = new FileInputStream(args[0]);
		InputStreamReader isr = new InputStreamReader(fis, "UTF8");
		Document document = builder.build(isr);
		HashSet<String> stopWords = stopWords(new File(args[1]));
		Element root = document.getRootElement();
		List docs = root.getChildren("DOC");
		for (int i = 0; i < docs.size(); i++) {
			Element doc = (Element) docs.get(i);
			String id = doc.getAttribute("id").getValue();
			String txt = doc.getText();
			txt = (txt.replace('\n', ' ')).replaceAll(" [ ]*", " ");
			String[] toks = txt.split(" ");
			for (String tok : toks) {
				if (stopWords.contains(tok))
					continue;
				if (tok.trim().length() == 0)
					continue;
				System.out.print(tok.trim() + " ");
			}
			System.out.print("\n");
			System.err.println(id);
		}
	}

	private static HashSet<String> stopWords(File file) throws FileNotFoundException {
		HashSet<String> ret = new HashSet<String>();
		Scanner in = new Scanner(file);
		while (in.hasNextLine())
			ret.add(in.nextLine());
		return ret;
	}
}
