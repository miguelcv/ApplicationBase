package org.mcv.mu.stdlib;

import java.util.List;

public class IList {
	
	public static String toString(List<?> a) {
		return a.toString();
	}
	
	public static Boolean in(List<Object>b, Object a) {
		return b.contains(a);
	}

	public static Boolean not(List<Object>a) {
		return !a.isEmpty();
	}

}
