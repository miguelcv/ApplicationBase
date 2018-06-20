package org.mcv.mu.stdlib;

import org.mcv.mu.Type;

public class IType {

	public static Boolean eq(Type a, Type b) {
		return a.matches(b);
	}

	public static Boolean eqeq(Type a, Type b) {
		return a.equals(b);
	}

	public static Boolean neq(Type a, Type b) {
		return !eq(a, b);
	}

	public static Boolean neqeq(Type a, Type b) {
		return !eqeq(a, b);
	}

	public static Object id(Type a) {
		return a;
	}	

	public static String toString(Type a) {
		return String.valueOf(a);
	}	

	public static Type and(Type a, Type b) {
		return new Type.IntersectionType(a, b);
	}

	public static Type or(Type a, Type b) {
		return new Type.UnionType(a, b);
	}
}
