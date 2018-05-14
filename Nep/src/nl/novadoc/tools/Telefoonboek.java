package nl.novadoc.tools;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import nl.novadoc.utils.WrapperException;

public class Telefoonboek {

	static String urlTmpl = "https://www.detelefoongids.nl/%s/%s/%s/?page=%d";
	private static final String PRIVATE = "7-1";
	private static final String BUSINESS = "3-1";
	Stack<String> businessStk = new Stack<>();
	Stack<String> privateStk = new Stack<>();
	Stack<String> unknownStk = new Stack<>();

	String places[] = { "Amsterdam", "Rotterdam", "Den Haag", "Utrecht", "Den Bosch", "Maastricht", "Groningen",
			"Arnhem", "Nijmegen", "Almere" };

	String lastnames[] = { "Jansen", "de Vries", "de Jong", "van den Berg", "van Dijk", "Bakker", "Visser", "Smit",
			"Meijer", "de Boer" };

	String branches[] = { "Apotheek", "Aannemer", "Bloemist", "Bakker", "Huisarts", "Fietsenmaker", "Kapper",
			"Loodgieter", "Restaurant", "Notaris" };

	private String randomPlace() {
		return places[r()];
	}

	private String randomName() {
		return lastnames[r()];
	}

	private String randomBranch() {
		return branches[r()];
	}

	int r() {
		return new SecureRandom().nextInt(10);
	}

	public String getBusiness() {
		if (businessStk.isEmpty()) {
			String branch = randomBranch();
			String place = randomPlace();
			String url = String.format(urlTmpl, branch, place, BUSINESS, 1);
			businessStk = extract(url, "gevestigd te ");
		}
		return businessStk.pop();
	}

	public String getPrivate() {
		if (privateStk.isEmpty()) {
			String name = randomName();
			String place = randomPlace();
			String url = String.format(urlTmpl, name, place, PRIVATE, 1);
			privateStk = extract(url, "wonende te ");
		}
		return privateStk.pop();
	}

	public String getUnknown() {
		if (unknownStk.isEmpty()) {
			String name = randomName();
			String place = randomPlace();
			String url = String.format(urlTmpl, name, place, PRIVATE, 1);
			unknownStk = extract(url, "");
		}
		return unknownStk.pop();
	}

	private Stack<String> extract(String url, String injectedText) {
		
		Document doc = connect(url);
		
		List<Element> names = doc.select("span[itemprop='name']");
		Elements addrs = doc.select("span[itemprop='streetAddress']");
		Elements zips = doc.select("span[itemprop='postalCode']");
		Elements cities = doc.select("span[itemprop='addressLocality']");

		// too many names [breadcrumb], reverse: doesn't matter anyhow
		Collections.reverse(names);
		
		Stack<String> entries = new Stack<>();

		for (int i = 0; i < addrs.size(); i++) {

			StringBuilder sb = new StringBuilder();

			Element elt = names.get(i);
			sb.append(elt.text()).append(" ");

			sb.append(injectedText);

			elt = addrs.get(i);
			sb.append(elt.text()).append(" ");

			elt = zips.get(i);
			sb.append(elt.text()).append(" ");

			elt = cities.get(i);
			sb.append(elt.text());
			
			entries.add(sb.toString());
		}

		return entries;
	}

	Document connect(String url) {
		try {
			return Jsoup.connect(url).get();
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}
}
