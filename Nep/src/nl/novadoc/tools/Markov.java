package nl.novadoc.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import nl.novadoc.utils.ResourceFinder;
import nl.novadoc.utils.WrapperException;

public class Markov {

	protected static final char MARKER = '/';
	/** Number of times to try generating a word before giving up. */
	protected static final int MAX_ITERATIONS = 2000;
	protected static final int DEFAULT_WIDTH = 4;
	protected static final int DEFAULT_MAXIMUM_LENGTH = 15;
	protected static final boolean DEFAULT_ALLOW_SOURCE_WORDS = true;

	static Set<String> words;
	static List<String> wordsL;

	protected Set<String> excluded;

	protected int[] counts;
	protected int[] sums;
	protected int width;
	protected int maxLen;
	protected boolean allowSourceWords;

	/**
	 * Constructs a Markov object capable of generating random words.
	 * 
	 * @param source
	 *            Source of random vocabulary.
	 */
	public Markov(URL source) {
		this(source, null, DEFAULT_WIDTH);
	}

	public Markov(String source) {
		this(ResourceFinder.getResource(source));
	}
	/**
	 * Constructs a Markov object capable of generating random words.
	 * 
	 * @param source
	 *            Construct words based on the words from the given source.
	 * @param exclude
	 *            Words to disallow from the output (may be null).
	 * @param width
	 *            Controls how similar to the existing words the generated words
	 *            are&mdash;4/5 is (very) similar, 2 is deviant, and 3 is in
	 *            between.
	 */
	public Markov(URL source, URL exclude, int width) {
		try {
			if (words == null) {
				words = new HashSet<>();
				
				excluded = new HashSet<>();

				parseWords(source, words);
				if (exclude != null)
					parseWords(exclude, excluded);

				if (words.isEmpty())
					throw new IOException("No valid words.");
				wordsL = new ArrayList<>(words);
			}
			setWidth(width);
			setMaximumLength(DEFAULT_MAXIMUM_LENGTH);
			setAllowSourceWords(DEFAULT_ALLOW_SOURCE_WORDS);
		} catch (Exception e) {
			throw new WrapperException(e);
		}

	}

	public String[] genPlain(int num) {
		String[] words1 = new String[num];
		for (int n = 0; n < num; n++) {
			String word = genPlain();
			if (word == null)
				break;
			words1[n] = word;
		}
		return words1;
	}

	SecureRandom rnd = new SecureRandom();

	public String genPlain() {
		return wordsL.get(rnd.nextInt(words.size()));
	}

	/** Generates the given number of random words. */
	public String[] generate(int num) {
		String[] words1 = new String[num];
		for (int n = 0; n < num; n++) {
			String word = generate();
			if (word == null)
				break;
			words1[n] = word;
		}
		return words1;
	}

	/** Generates one random word. */
	public String generate() {
		StringBuffer sb = new StringBuffer();
		for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
			sb.setLength(0);
			for (int i = 0; i < width - 1; i++)
				sb.append(MARKER);
			boolean success = false;
			while (sb.length() < maxLen + width) {
				// tuple for N-1 letters
				String tuple = sb.substring(sb.length() - width + 1);
				int sum = sums[getIndex(tuple)];
				int ci = (int) (sum * Math.random());

				char c = 'a' - 1;
				while (c <= 'z') {
					c++;
					ci -= counts[getIndex(tuple + c)];
					if (ci < 0)
						break;
				}
				if (c > 'z') {
					// successfully reached a terminating tuple
					success = true;
					break;
				}
				sb.append(c);
			}
			if (success) {
				String word = sb.substring(width - 1);
				if (!allowSourceWords && words.contains(word)) {
					continue;
				}
				if (excluded.contains(word)) {
					continue;
				}
				return word;
			}
			// word too long; try again...
		}
		// not enough data; give up
		return null;
	}

	/** Gets the number of words used for input. */
	public int getSourceWordCount() {
		return words.size();
	}

	/** Gets the number of words on the exclusion list. */
	public int getExcludeWordCount() {
		return excluded.size();
	}

	/** Sets tuple width for generation algorithm. */
	public void setWidth(int width) {
		this.width = width;
		if (width < 2 || width > 5) {
			throw new IllegalArgumentException("Invalid width: " + width + " (expected 2 <= width <= 5)");
		}

		// rebuild counts and sums structures
		int size = 1;
		for (int i = 0; i < width; i++)
			size *= 27;
		counts = new int[size];
		sums = new int[size / 27];

		String prefix = "";
		for (int i = 0; i < width - 1; i++)
			prefix += MARKER;

		Iterator<String> iter = words.iterator();
		while (iter.hasNext()) {
			String word = iter.next();

			// prepend N-1 markers to the front, and append one marker to the end
			word = prefix + word + MARKER;

			// compile tuples from word
			int len = word.length();
			for (int i = width; i <= len; i++) {
				String s = word.substring(i - width, i);
				// increment count for this N-tuple
				counts[getIndex(s)]++;
				// increment sum for N-tuples starting with these N-1 letters
				sums[getIndex(s.substring(0, s.length() - 1))]++;
			}
		}
	}

	/** Gets tuple width for generation algorithm. */
	public int getWidth() {
		return width;
	}

	/** Sets the maximum length of each generated word. */
	public void setMaximumLength(int maxLen) {
		this.maxLen = maxLen;
	}

	/** Gets the maximum length of each generated word. */
	public int getMaximumLength() {
		return maxLen;
	}

	/** Sets whether to allow generated words to match a source word. */
	public void setAllowSourceWords(boolean allow) {
		allowSourceWords = allow;
	}

	/** Gets whether to allow generated words to match a source word. */
	public boolean isAllowSourceWords() {
		return allowSourceWords;
	}

	/**
	 * Reads the list of words from the given source into the specified set.
	 */
	protected void parseWords(URL source, Set<String> words2) {
		try {
			// read words from source
			BufferedReader fin = new BufferedReader(new InputStreamReader(source.openStream()));
			String line;
			while (true) {
				// read word entry
				line = fin.readLine();
				if (line == null)
					break;
				StringTokenizer st = new StringTokenizer(line);
				String token = st.nextToken().toLowerCase();

				// ignore entries with invalid characters
				int len = token.length();
				boolean valid = true;
				for (int i = 0; i < len; i++) {
					char c = token.charAt(i);
					if (c < 'a' || c > 'z') {
						valid = false;
						break;
					}
				}
				if (!valid)
					continue;

				words2.add(token);
			}
			fin.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	/** Converts an N-tuple to an index. */
	protected int getIndex(String s) {
		int ndx = 0;
		for (int j = 0; j < s.length(); j++) {
			ndx *= 27;
			char c = s.charAt(j);
			int q = c == MARKER ? 26 : c - 'a';
			ndx += q;
		}
		return ndx;
	}

	public static void main(String[] args) {
		String[] strings = new Markov("Nederlands2.txt").generate(100);
		for(String s : strings) System.out.println(s);
		System.out.println("***************************");
		strings = new Markov("Nederlands2.txt").genPlain(100);
		for(String s : strings) System.out.println(s);
	}
}
