package org.mcv.mu.stdlib;

import java.util.List;
import java.util.Set;

public class ISet {
	
	public static Boolean in(Set<Object>b, Object a) {
		return b.contains(a);
	}

	public static Boolean not(List<Object>a) {
		return !a.isEmpty();
	}

}
