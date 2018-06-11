package org.mcv.mu.stdlib;

import org.mcv.math.BigInteger;

public class IInt extends IType {
	
	// ORD
	public static Boolean gt(BigInteger a, BigInteger b) {
		if(a.isNaN() || b.isNaN()) {
			return false;
		}
		return a.compareTo(b) > 0;
	}

	public static Boolean ge(BigInteger a, BigInteger b) {
		if(a.isNaN() || b.isNaN()) {
			return false;
		}
		return a.compareTo(b) >= 0;
	}
	
	public static Boolean lt(BigInteger a, BigInteger b) {
		if(a.isNaN() || b.isNaN()) {
			return false;
		}
		return a.compareTo(b) < 0;
	}
	
	public static Boolean le(BigInteger a, BigInteger b) {
		if(a.isNaN() || b.isNaN()) {
			return false;
		}
		return a.compareTo(b) <= 0;
	}

	// EQ
	public static Boolean eq(BigInteger a, BigInteger b) {
		return a.equals(b);
	}
	
	public static Boolean neq(BigInteger a, BigInteger b) {
		return !eq(a, b);
	}

	public static Boolean eqeq(BigInteger a, BigInteger b) {
		return a.equals(b);
	}
	
	public static Boolean neqeq(BigInteger a, BigInteger b) {
		return !eq(a, b);
	}

	// PLUS/MINUS
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

	// MULT/DIV
	/* Euclidean division */
	public static BigInteger div(BigInteger a, BigInteger b)
	{
		BigInteger q = a.divide(b);
		BigInteger r = a.remainder(b);
		if (r.compareTo(BigInteger.ZERO) < 0) {
			if (b.compareTo(BigInteger.ZERO) > 0) {
				q = q.subtract(BigInteger.ONE);
			} else {
				q = q.add(BigInteger.ONE);
			}
		}
		return q;
	}
	
	public static BigInteger rem(BigInteger a, BigInteger b)
	{
		BigInteger r = a.remainder(b);
		if (r.compareTo(BigInteger.ZERO) < 0) {
			if (b.compareTo(BigInteger.ZERO) > 0) {
				r = r.add(b);
			} else {
				r = r.subtract(b);
			}
		}
		return r;
	}
	
	public static BigInteger mul(BigInteger a, BigInteger b) {
		return a.multiply(b);
	}

	// POW / SQRT
	public static BigInteger pow(BigInteger a, BigInteger b) {
		return a.pow(b);
	}

	public static BigInteger sqrt(BigInteger a) {
		return a.sqrt();
	}
	
	// BITWISE OPERATORS
	public static BigInteger and(BigInteger a, BigInteger b) {
		return a.and(b);	
	}

	public static BigInteger or(BigInteger a, BigInteger b) {
		return a.or(b);	
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

	public static BigInteger xor(BigInteger a, BigInteger b) {
		return a.xor(b);
	}
	
	// OTHER
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

	public static String toString(BigInteger a) {
		return String.valueOf(a);
	}
	
	public static Double toReal(BigInteger a) {
		return a.doubleValue();
	}
}
