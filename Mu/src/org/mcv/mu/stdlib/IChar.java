package org.mcv.mu.stdlib;

public class IChar extends IType {
	
	public static Boolean gt(Integer a, Integer b) {
		return a > b;
	}

	public static Boolean ge(Integer a, Integer b) {
		return a >= b;
	}
	
	public static Boolean lt(Integer a, Integer b) {
		return a < b;
	}
	
	public static Boolean le(Integer a, Integer b) {
		return a <= b;
	}
	
	public static Boolean eq(Integer a, Integer b) {
		return a == b;
	}
	
	public static Boolean neq(Integer a, Integer b) {
		return a != b;
	}

	public static Boolean eqeq(Integer a, Integer b) {
		return a == b;
	}
	
	public static Boolean neqeq(Integer a, Integer b) {
		return a != b;
	}

	public static Integer plus(Integer a, Integer b) {
		return a + b;
	}

	public static Integer minus(Integer a, Integer b) {
		return a - b;
	}
	
	public static Integer inc(Integer a) {
		return ++a;
	}
	
	public static Integer dec(Integer a) {
		return --a;
	}
	
	public static Integer max(Integer a, Integer b) {
		return a >= b ? a : b;
	}

	public static Integer min(Integer a, Integer b) {
		return a < b ? a : b;		
	}

	public static Integer id(Integer a) {
		return a;
	}

	public static String toString(Integer a) {
		return new String(Character.toChars(a));
	}
}
