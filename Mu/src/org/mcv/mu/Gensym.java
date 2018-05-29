package org.mcv.mu;

public class Gensym {

	final String prefix;
	private long id;
	private String fmt;

	public Gensym() {
		this("ANON_");
	}

	public Gensym(String pref) {
		prefix = pref;
		id = (long) 0;
		setDigits(9);
	}

	public void setDigits(int d) {
		fmt = String.format("%%s%%0%dd", d);
	}

	public String nextSymbol() {
		return String.format(fmt, prefix, id++);
	}
}
