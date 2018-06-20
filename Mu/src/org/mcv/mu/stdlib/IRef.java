package org.mcv.mu.stdlib;

import org.mcv.mu.Pointer;

public class IRef extends IType {
	
	public static Boolean gt(Pointer a, Pointer b) {
		return a.compareTo(b) > 0;
	}

	public static Boolean ge(Pointer a, Pointer b) {
		return a.compareTo(b) >= 0;
	}
	
	public static Boolean lt(Pointer a, Pointer b) {
		return a.compareTo(b) < 0;
	}
	
	public static Boolean le(Pointer a, Pointer b) {
		return a.compareTo(b) <= 0;
	}
	
	public static Boolean eq(Pointer a, Pointer b) {
		return a.equals(b);
	}
	
	public static Boolean neq(Pointer a, Pointer b) {
		return !eq(a, b);
	}

	public static Boolean eqeq(Pointer a, Pointer b) {
		return a.equals(b);
	}
	
	public static Boolean neqeq(Pointer a, Pointer b) {
		return !eq(a, b);
	}

	public static Pointer ref(Object a) {
		return new Pointer(a);
	}

	public static Pointer id(Pointer a) {
		return a;
	}

	public static Object deref(Pointer a) {
		return a.getRef();
	}

	public static String toString(Pointer a) {
		return String.valueOf(a);
	}
}
