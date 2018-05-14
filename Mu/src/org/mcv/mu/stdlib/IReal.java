package org.mcv.mu.stdlib;

import java.math.BigDecimal;
import java.math.MathContext;

public class IReal extends IType {

	public static Boolean gt(BigDecimal a, BigDecimal b) {
		return a.compareTo(b) > 0;
	}

	public static Boolean ge(BigDecimal a, BigDecimal b) {
		return a.compareTo(b) >= 0;
	}
	
	public static Boolean lt(BigDecimal a, BigDecimal b) {
		return a.compareTo(b) < 0;
	}
	
	public static Boolean le(BigDecimal a, BigDecimal b) {
		return a.compareTo(b) <= 0;
	}
	
	public static Boolean eq(BigDecimal a, BigDecimal b) {
		return a.equals(b);
	}
	
	public static Boolean neq(BigDecimal a, BigDecimal b) {
		return !eq(a, b);
	}

	public static Class<?> javaType() {
		return BigDecimal.class;
	}

	public static BigDecimal plus(BigDecimal a, BigDecimal b) {
		return a.add(b);
	}

	public static BigDecimal minus(BigDecimal a, BigDecimal b) {
		return a.subtract(b);
	}

	public static BigDecimal neg(BigDecimal a) {
		return a.negate();
	}

	public static BigDecimal inc(BigDecimal a) {
		return a.add(BigDecimal.ONE);
	}
	
	public static BigDecimal dec(BigDecimal a) {
		return a.subtract(BigDecimal.ONE);
	}

	public static BigDecimal div(BigDecimal a, BigDecimal b) {
		return a.divide(b, MathContext.DECIMAL128);
	}
	
	public static BigDecimal rem(BigDecimal a, BigDecimal b) {
		return a.remainder(b, MathContext.DECIMAL128);
	}

	public static BigDecimal mul(BigDecimal a, BigDecimal b) {
		return a.multiply(b, MathContext.DECIMAL128);
	}

	public static BigDecimal pow(BigDecimal a, BigDecimal b) {
		return a.pow(b.intValue(), MathContext.DECIMAL128);
	}

	public static BigDecimal abs(BigDecimal a) {
		return a.abs();
	}
	
	public static BigDecimal max(BigDecimal a, BigDecimal b) {
		return a.max(b);
	}
	
	public static BigDecimal min(BigDecimal a, BigDecimal b) {
		return a.min(b);
	}
	
	public static BigDecimal sqrt(BigDecimal a) {
		return a.sqrt(MathContext.DECIMAL128);
	}
	
	public static Boolean isTrue(BigDecimal a) {
		return true;
	}

}
