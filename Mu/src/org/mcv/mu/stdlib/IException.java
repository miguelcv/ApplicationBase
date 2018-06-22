package org.mcv.mu.stdlib;

import org.mcv.mu.MuException;

public class IException {
	
	public static Boolean eq(MuException a, MuException b) {
		return a.equals(b);
	}
	
	public static Boolean neq(MuException a, MuException b) {
		return !eq(a, b);
	}

	public static Boolean eqeq(MuException a, MuException b) {
		return a.equals(b);
	}
	
	public static Boolean neqeq(MuException a, MuException b) {
		return !eq(a, b);
	}

	public static MuException id(MuException a) {
		return a;
	}

	public static Object deref(MuException a) {
		return a.getValue();
	}

	public static String toString(MuException a) {
		return String.valueOf(a);
	}

	public static Boolean not(MuException w) {
		return true;
	}
}
