package org.mcv.mu.stdlib;

import org.mcv.math.BigInteger;

public class INum  extends IReal {
	
	public static Double convert(Object o) {
		if(o instanceof Double) return (Double)o;
		if(o instanceof BigInteger) return ((BigInteger)o).doubleValue();
		return null; // or throw exception?
	}
		
}
