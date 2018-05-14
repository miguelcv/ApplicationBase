package org.mcv.mu.stdlib;

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

	public static Class<?> javaType() {
		return String.class;
	}

	public static  String and(String a, String b) {
		return a + b;
	}
	
	public static Boolean isTrue(String a) {
		return a != null && a.length() > 0;
	}

}
