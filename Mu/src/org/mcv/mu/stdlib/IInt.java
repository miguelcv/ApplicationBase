package org.mcv.mu.stdlib;

import java.math.BigInteger;

public class IInt extends IType {
	
	public static Boolean gt(BigInteger a, BigInteger b) {
		return a.compareTo(b) > 0;
	}

	public static Boolean ge(BigInteger a, BigInteger b) {
		return a.compareTo(b) >= 0;
	}
	
	public static Boolean lt(BigInteger a, BigInteger b) {
		return a.compareTo(b) < 0;
	}
	
	public static Boolean le(BigInteger a, BigInteger b) {
		return a.compareTo(b) <= 0;
	}
	
	public static Boolean eq(BigInteger a, BigInteger b) {
		return a.equals(b);
	}
	
	public static Boolean neq(BigInteger a, BigInteger b) {
		return !eq(a, b);
	}

	public static Class<?> javaType() {
		return BigInteger.class;
	}

	public static BigInteger plus(BigInteger a, BigInteger b) {
		return a.add(b);
	}

	public static BigInteger minus(BigInteger a, BigInteger b) {
		return a.subtract(b);
	}

	public static BigInteger neg(BigInteger a) {
		return a.negate();
	}

	public static BigInteger inc(BigInteger a) {
		return a.add(BigInteger.ONE);
	}
	
	public static BigInteger dec(BigInteger a) {
		return a.subtract(BigInteger.ONE);
	}

	public static BigInteger div(BigInteger a, BigInteger b) {
		return a.divide(b);
	}
	
	public static BigInteger rem(BigInteger a, BigInteger b) {
		return a.remainder(b);
	}

	public static BigInteger mul(BigInteger a, BigInteger b) {
		return a.multiply(b);
	}

	public static BigInteger and(BigInteger a, BigInteger b) {
		return a.and(b);	
	}

	public static BigInteger or(BigInteger a, BigInteger b) {
		return a.or(b);	
	}

	public static BigInteger pow(BigInteger a, BigInteger b) {
		return a.pow(b.intValue());
	}

	public static BigInteger fac(BigInteger a) {
		BigInteger result = BigInteger.ONE;
		while (!a.equals(BigInteger.ZERO)) {
			result = result.multiply(a);
			a = a.subtract(BigInteger.ONE);
		}
		return result;
	}
	
	public static BigInteger abs(BigInteger a) {
		return a.abs();
	}
	
	public static BigInteger gcd(BigInteger a, BigInteger b) {
		return a.gcd(b);
	}
	
	public static BigInteger max(BigInteger a, BigInteger b) {
		return a.max(b);
	}
	
	public static BigInteger min(BigInteger a, BigInteger b) {
		return a.min(b);
	}
	
	public static BigInteger not(BigInteger a) {
		return a.not();
	}
	
	public static BigInteger lsh(BigInteger a, BigInteger b) {
		return a.shiftLeft(b.intValue());
	}
	
	public static BigInteger rsh(BigInteger a, BigInteger b) {
		return a.shiftRight(b.intValue());
	}
	
	public static BigInteger sqrt(BigInteger a) {
		return a.sqrt();
	}
	
	public static BigInteger xor(BigInteger a, BigInteger b) {
		return a.xor(b);
	}
	
	public static Boolean isTrue(Boolean a) {
		return true;
	}

}
