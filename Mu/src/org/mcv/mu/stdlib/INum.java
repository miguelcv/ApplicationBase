package org.mcv.mu.stdlib;

import java.math.BigDecimal;
import java.math.BigInteger;

public class INum  extends IReal {
	
	public static BigDecimal convert(Object o) {
		if(o instanceof BigDecimal) return (BigDecimal)o;
		if(o instanceof BigInteger)	return new BigDecimal((BigInteger)o);
		return null; // or throw exception?
	}
		
}
