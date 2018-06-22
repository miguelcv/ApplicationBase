package org.mcv.mu.stdlib;

import java.util.List;
import java.util.Map;

public class IMap {

	public static Boolean in(Map<String, Object>b, String a) {
		return b.containsKey(a);
	}
	
	public static Boolean not(List<Object>a) {
		return !a.isEmpty();
	}

}
