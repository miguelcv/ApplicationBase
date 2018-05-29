package org.mcv.mu.stdlib;

public class IThunk extends IType {
	
	@SuppressWarnings("unchecked")
	public static Boolean gt(Object a, Object b) {
		if(a instanceof Comparable && b instanceof Comparable) {
			return ((Comparable<Object>)a).compareTo((Comparable<Object>)b) > 0;
		}
		throw new TypeError("Types are not comparable");
	}

	@SuppressWarnings("unchecked")
	public static Boolean ge(Object a, Object b) {
		if(a instanceof Comparable && b instanceof Comparable) {
			return ((Comparable<Object>)a).compareTo((Comparable<Object>)b) >= 0;
		}
		throw new TypeError("Types are not comparable");		
	}
	
	@SuppressWarnings("unchecked")
	public static Boolean lt(Object a, Object b) {
		if(a instanceof Comparable && b instanceof Comparable) {
			return ((Comparable<Object>)a).compareTo((Comparable<Object>)b) < 0;
		}
		throw new TypeError("Types are not comparable");		
	}
	
	@SuppressWarnings("unchecked")
	public static Boolean le(Object a, Object b) {
		if(a instanceof Comparable && b instanceof Comparable) {
			return ((Comparable<Object>)a).compareTo((Comparable<Object>)b) <= 0;
		}
		throw new TypeError("Types are not comparable");	
	}
	
	public static Boolean eq(Object a, Object b) {
		return a.equals(b);
	}
	
	public static Boolean neq(Object a, Object b) {
		return !eq(a, b);
	}

	public static Class<?> javaType() {
		return Object.class;
	}

	public static Boolean isTrue(Object a) {
		return a != null;
	}
	
}
