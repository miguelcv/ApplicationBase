package nl.novadoc.tools;

public class Gensym {
	
	private String prefix;
	private long id;
	private int digits;
	private String fmt;

	public Gensym() {
		this("G_");
	}
	
	public Gensym(String pref) {
		prefix = pref;
		id = (long) 0;
		setDigits(9);
	}

	public void setDigits(int d) {
		digits = d;
		fmt = String.format("%%s%%0%dd", digits);
	}

	public String nextSymbol() {
		return String.format(fmt, prefix, id++);
	}

}
