package nl.novadoc.tools;

public class Type {
	String name;
	
	/* data representation */
	byte[] rep;
	int size;
	
	/* interface */
	Signature[] iface;

	public boolean check(Value v) {
		// TODO
		return false;
	}
}
