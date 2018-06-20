package org.mcv.mu.stdlib;

import org.mcv.mu.Interpreter;

public class IAny extends IType {
	
	private static final String TYPES_ARE_NOT_COMPARABLE = "Types are not comparable";

	@SuppressWarnings("unchecked")
	public static Boolean gt(Object a, Object b) {
		if(a instanceof Comparable && b instanceof Comparable) {
			return ((Comparable<Object>)a).compareTo((Comparable<Object>)b) > 0;
		}
		throw new Interpreter.InterpreterError(TYPES_ARE_NOT_COMPARABLE);
	}

	@SuppressWarnings("unchecked")
	public static Boolean ge(Object a, Object b) {
		if(a instanceof Comparable && b instanceof Comparable) {
			return ((Comparable<Object>)a).compareTo((Comparable<Object>)b) >= 0;
		}
		throw new Interpreter.InterpreterError(TYPES_ARE_NOT_COMPARABLE);		
	}
	
	@SuppressWarnings("unchecked")
	public static Boolean lt(Object a, Object b) {
		if(a instanceof Comparable && b instanceof Comparable) {
			return ((Comparable<Object>)a).compareTo((Comparable<Object>)b) < 0;
		}
		throw new Interpreter.InterpreterError(TYPES_ARE_NOT_COMPARABLE);		
	}
	
	@SuppressWarnings("unchecked")
	public static Boolean le(Object a, Object b) {
		if(a instanceof Comparable && b instanceof Comparable) {
			return ((Comparable<Object>)a).compareTo((Comparable<Object>)b) <= 0;
		}
		throw new Interpreter.InterpreterError(TYPES_ARE_NOT_COMPARABLE);	
	}
	
	public static Boolean eq(Object a, Object b) {
		return a.equals(b);
	}

	public static Boolean eqeq(Object a, Object b) {
		return a == b;
	}

	public static Boolean neq(Object a, Object b) {
		return !eq(a, b);
	}

	public static Boolean neqeq(Object a, Object b) {
		return !eqeq(a, b);
	}

	public static Object id(Object a) {
		return a;
	}	

	public static String toString(Object a) {
		return String.valueOf(a);
	}	
}
