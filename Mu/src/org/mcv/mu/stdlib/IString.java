package org.mcv.mu.stdlib;

import java.util.Arrays;

public class IString extends IType {
	
	public static Boolean gt(String a, String b) {
		return a.compareTo(b) > 0;
	}

	public static Boolean ge(String a, String b) {
		return a.compareTo(b) >= 0;
	}
	
	public static Boolean lt(String a, String b) {
		return a.compareTo(b) < 0;
	}
	
	public static Boolean le(String a, String b) {
		return a.compareTo(b) <= 0;
	}
	
	public static Boolean eq(String a, String b) {
		return a.equals(b);
	}
	
	public static Boolean neq(String a, String b) {
		return !eq(a, b);
	}

	public static Boolean eqeq(String a, String b) {
		return a == b;
	}
	
	public static Boolean neqeq(String a, String b) {
		return a != b;
	}

	public static Class<?> javaType() {
		return String.class;
	}

	public static String and(String a, String b) {
		return a + b;
	}

	public static String max(String a, String b) {
		return a.compareTo(b) >= 0 ? a : b;
	}

	public static String min(String a, String b) {
		return a.compareTo(b) < 0 ? a : b;		
	}

	public static String toString(String a) {
		return String.valueOf(a);
	}
	
	public static void doesNotUnderstand(String op, Object o, Object...args) {
		System.err.println(o.toString() + " does not understand mathod " + op + " with args " + Arrays.deepToString(args));
	}

}
