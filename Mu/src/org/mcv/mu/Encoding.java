package org.mcv.mu;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.tomtung.latex2unicode.LaTeX2Unicode;

import nl.novadoc.utils.FileUtils;

public class Encoding {

	static Map<String, Integer> unicodeNames = new LinkedHashMap<>();

	static {
		File f = new File("UniData.txt");
		String[] lines = FileUtils.readLines(f);
		for (String line : lines) {
			String[] vk = line.split(": ");
			String[] keys = vk[1].split(",");
			for(String key : keys) {
				unicodeNames.put(key, fromHex(vk[0]));
			}
		}
	}

	public static int fromUnicodeName(String name) {
		return unicodeNames.getOrDefault(name, -1);
	}

	public static int fromHex(String hex) {
		try {
			return Integer.parseInt(hex, 16);
		} catch (Exception e) {
			return -1;
		}
	}

	public static int fromEntity(String entity) {
		try {
			return org.jsoup.parser.Parser.unescapeEntities(entity + ";", false).codePointAt(0);
		} catch (Exception e) {
			return -1;
		}
	}

	public static int fromLaTeX(String escape) {
		try {
			return LaTeX2Unicode.convert(escape).codePointAt(0);
		} catch (Exception e) {
			return -1;
		}
	}

	public static int decode(String name) {
		if (name.startsWith("&"))
			return fromEntity(name);
		if (name.startsWith("\\"))
			return fromLaTeX(name);
		int cp = fromUnicodeName(name);
		if (cp == -1)
			return fromHex(name);
		return cp;
	}
}
