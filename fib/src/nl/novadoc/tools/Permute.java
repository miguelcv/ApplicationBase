package nl.novadoc.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Permute {

	private static Map<Character, List<String>> permutations = new HashMap<>();

	public static void main(String[] args) {
		/*
        List<String> entries = Arrays.asList(FileUtils.readLines(findOCRMistakes()));
        for(String entry : entries) 
        {
            permutations.put(entry.charAt(0), Arrays.asList(entry.substring(entry.indexOf(':') + 1).split(", ")));
            System.out.println("OCR Mistake: " + permutations.get(entry.charAt(0)));
        }
        */
		String input = "wonend";
		//List<String> list = permute(input);
		//System.out.println("permutations: " + list.size());
		
		//System.out.println(transpose(input));
		System.out.println(insertions(input));
		System.out.println(delsubst(input));
	}
	
	private static List<String> delsubst(String input) {
		int length = input.length();
		List<String> ret = new ArrayList<>();
		for(int i=0; i < length; i++) {
			String pre = input.substring(0, i);
			String post = input.substring(i+1);
			ret.add(pre + ".?.?" + post);
		}
		return ret;		
	}

	private static List<String> insertions(String input) {
		int length = input.length();
		List<String> ret = new ArrayList<>();
		for(int i=0; i < length+1; i++) {
			String pre = input.substring(0, i);
			String post = input.substring(i);
			ret.add(pre + "." + post);
		}
		return ret;		
	}

	private static List<String> transpose(String input) {
		int length = input.length();
		List<String> ret = new ArrayList<>();
		for(int i=0; i < length-1; i++) {
			char c1 = input.charAt(i);
			String pre = input.substring(0, i);
			String post = input.substring(i+2);
			char c2 = input.charAt(i+1);
			ret.add(pre + c2 + c1 + post);
		}
		return ret;
	}

	private static File findOCRMistakes() {
		File file = new File("OCR_mistakes.txt");
		if (file.exists()) {
			return file;
		}
		System.out.println("File not found");throw new RuntimeException();
	}

	static List<String> permute(String input) {
		List<StringBuilder> acc = new ArrayList<>();
		acc.add(new StringBuilder());
		for(int i=0; i < input.length(); i++) {
			char c = input.charAt(i);
			List<StringBuilder> additions = new ArrayList<>();
			for (StringBuilder sb : acc) {
				StringBuilder copy = new StringBuilder(sb.toString());
				sb.append(c);
				if(Character.isLetterOrDigit(c)) {
					additions.add(copyAndAppend(copy, c + " "));
				}
				List<String> list = permutations.get(c);
				if (list != null) {
					for (String s : list) {
						additions.add(copyAndAppend(copy, s));
					}
				}
			}
			acc.addAll(additions);
		}
		List<String> ret = new ArrayList<>();
		for (StringBuilder sb : acc) {
			ret.add(sb.toString());
		}
		return ret;
	}

	private static StringBuilder copyAndAppend(StringBuilder sb, String s) {
		StringBuilder ret = new StringBuilder(sb.toString());
		ret.append(s);
		return ret;
	}
}
