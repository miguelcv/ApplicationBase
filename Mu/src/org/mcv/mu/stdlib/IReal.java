package org.mcv.mu.stdlib;

import org.mcv.uom.Bag;
import org.mcv.uom.Unit;
import org.mcv.uom.UnitValue;

public class IReal extends IType {

	public static Boolean gt(Double a, Double b) {
		return a > b;
	}

	public static Boolean ge(Double a, Double b) {
		return a >= 0;
	}
	
	public static Boolean lt(Double a, Double b) {
		return a < 0;
	}
	
	public static Boolean le(Double a, Double b) {
		return a <= 0;
	}
	
	public static Boolean eq(Double a, Double b) {
		return a == b;
	}
	
	public static Boolean neq(Double a, Double b) {
		return a != b;
	}

	public static Boolean eqeq(Double a, Double b) {
		return a == b;
	}
	
	public static Boolean neqeq(Double a, Double b) {
		return a != b;
	}

	public static Double plus(Double a, Double b) {
		return a + b;
	}

	public static Double minus(Double a, Double b) {
		return a - b;
	}

	public static Double neg(Double a) {
		return -a;
	}

	public static Double inc(Double a) {
		return a + 1;
	}
	
	public static Double dec(Double a) {
		return a - 1;
	}

	public static Double div(Double a, Double b) {
		return a / b;
	}
	
	public static Double rem(Double a, Double b) {
		return a % b;
	}

	public static Double mul(Double a, Double b) {
		return a * b;
	}

	public static Double pow(Double a, Double b) {
		return Math.pow(a, b);
	}

	public static Double abs(Double a) {
		return Math.abs(a);
	}
	
	public static Double max(Double a, Double b) {
		return Math.max(a, b);
	}
	
	public static Double min(Double a, Double b) {
		return Math.min(a, b);
	}
	
	public static Double sqrt(Double a) {
		return Math.sqrt(a);
	}
	
	public static String toString(Double a) {
		return String.valueOf(a);
	}
	
	public static UnitValue toUnit(Double a) {
		Bag units = new Bag();
		units.add(new Unit("1", 1));
		return new UnitValue(a, units);
	}

	public static UnitValue toUnit(UnitValue a) {
		return a;
	}

}
