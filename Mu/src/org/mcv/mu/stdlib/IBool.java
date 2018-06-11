package org.mcv.mu.stdlib;

public class IBool extends IType {

	public static Boolean eq(Boolean a, Boolean b) {
		return a == b;	
	}

	public static Boolean eqeq(Boolean a, Boolean b) {
		return a == b;	
	}

	public static Boolean neq(Boolean a, Boolean b) {
		return !a.equals(b);	
	}

	public static Boolean neqeq(Boolean a, Boolean b) {
		return !a.equals(b);	
	}
	
	public static Boolean inc(Boolean b) {
		return true;
	}

	public static Boolean dec(Boolean b) {
		return false;
	}

	public static Boolean not(Boolean b) {
		return !b;
	}
	
	public static Boolean and(Boolean a, Boolean b) {
		return a && b; 
	}

	public static Boolean or(Boolean a, Boolean b) {
		return a || b;
	}

	public static Boolean xor(Boolean a, Boolean b) {
		return a ^ b;
	}

	public static Boolean gt(Boolean a, Boolean b) {
		return a ? !b : false;
	}
	
	public static Boolean ge(Boolean a, Boolean b) {
		return a ? true : !b;
	}
	
	public static Boolean lt(Boolean a, Boolean b) {
		return a ? false : b;
	}
	
	public static Boolean le(Boolean a, Boolean b) {
		return a ? b : true;
	}

	public static Boolean max(Boolean a, Boolean b) {
		return a ? a : b;
	}

	public static Boolean min(Boolean a, Boolean b) {
		return a ? b : a;
	}

	public static String toString(Boolean a) {
		return String.valueOf(a);
	}
}
