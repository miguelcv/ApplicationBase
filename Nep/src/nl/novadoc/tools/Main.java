package nl.novadoc.tools;

import java.net.URL;
import java.security.SecureRandom;
import java.util.Arrays;

import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

import nl.novadoc.utils.ResourceFinder;

public class Main {

	static Lorem lorem = LoremIpsum.getInstance();
	static Telefoonboek tel = new Telefoonboek();
	static int p1 = 0, p2 = 0, p3 = 0;
	static int a1 = 0, a2 = 0, a3 = 0;
	static int nwords = 0;
	static final int MIN = 1000;
	static int currDoc;
	
	public static void main(String... args) {

		int docs = 5;
		for (int i = 0; i < docs; i++) {
			System.out.println("Generating document " + (i+1));
			nwords = 0;
			currDoc = i;
			p1 = p2 = p3 = 0;
			a1 = a2 = a3 = 0;

			StringBuilder text = new StringBuilder();
			text.append("AKTE VAN OPRICHTING\n\n");

			String s;

			while ((s = randomText()) != null) {
				text.append(cap(s));
				text.append(punct());
				++nwords;
			}
			text.append(".\n");
			
			GenPDF.createPDF("NL" + i, Arrays.asList(text.toString().split("\n")));
		}
		System.out.println("Done.");
	}

	static boolean cap = true;
	
	static String cap(String s) {
		if(cap) {
			cap = false;
			return Character.toUpperCase(s.charAt(0)) + s.substring(1);
		} 
		return s;
	}
	
	static String[] punct = {
		". ", ", ", 
		"; ", ": ", 
		".\n",
		" ", " ", 
		" ", " ", 
		" ", 
		" ", " ", " ", " ",
		" "
	};
	
	private static Object punct() {
		String p = punct[new SecureRandom().nextInt(15)];
		if(p.startsWith(".")) cap = true;
		return p;
	}

	static String def() {
		//if (currDoc % 2 == 0) {
			URL url = ResourceFinder.getResource("Nederlands2.txt");
			return new Markov(url).genPlain();
		//} else {
		//	return lorem.getWords(1);
		//}
	}

	private static String randomText() {

		int n = new SecureRandom().nextInt(100);

		switch (n) {
		case 90:
			if (p1 < 4) {
				++p1;
				return "Paspoortnummer " + goodNLPassport();
			}
			return def();
		case 91:
			if (p2 < 4) {
				++p2;
				return "Paspoortnummer " + probablyBadNLPassport();
			}
			return def();
		case 92:
			if (p3 < 4) {
				++p3;
				return goodNLPassport();
			}
			return def();
		case 93:
		case 94:
			if (a1 < 4) {
				++a1;
				return tel.getBusiness();
			}
			return def();
		case 95:
		case 96:
			if (a2 < 4) {
				++a2;
				return tel.getPrivate();
			}
			return def();
		case 97:
		case 98:
			if (a3 < 4) {
				++a3;
				return tel.getUnknown();
			}
			return def();
			
		case 99:
			if (nwords > MIN)
				return null;
			return def();
		default:
			return def();
		}
	}

	static String goodNLPassport() {
		StringBuilder ret = new StringBuilder();
		ret.append(randomA());
		ret.append(randomA());
		ret.append(randomX());
		ret.append(randomX());
		ret.append(randomX());
		ret.append(randomX());
		ret.append(randomX());
		ret.append(randomX());
		ret.append(random9());
		return ret.toString();
	}

	static char randomA() {
		SecureRandom r = new SecureRandom();
		char ret = (char) (r.nextInt(1 + 'Z' - 'A') + 'A');
		if (ret == 'O')
			return randomA();
		return ret;
	}

	static char randomX() {
		SecureRandom r = new SecureRandom();
		if (r.nextInt() % 2 == 0) {
			return randomA();
		} else {
			return random9();
		}
	}

	static char random9() {
		SecureRandom r = new SecureRandom();
		return (char) (r.nextInt(10) + '0');
	}

	static String probablyBadNLPassport() {
		StringBuilder ret = new StringBuilder();
		int len = new SecureRandom().nextInt(5);
		for (int i = 0; i < 7 + len; i++) {
			ret.append(randomX());
		}
		return ret.toString();
	}

}
