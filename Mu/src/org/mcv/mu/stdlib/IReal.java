package org.mcv.mu.stdlib;

import java.util.Arrays;

public class IReal extends IType {

	public static Boolean gt(double a, double b) {
		return a > b;
	}

	public static Boolean ge(double a, double b) {
		return a >= 0;
	}
	
	public static Boolean lt(double a, double b) {
		return a < 0;
	}
	
	public static Boolean le(double a, double b) {
		return a <= 0;
	}
	
	public static Boolean eq(double a, double b) {
		return a == b;
	}
	
	public static Boolean neq(double a, double b) {
		return a != b;
	}

	public static Boolean eqeq(double a, double b) {
		return a == b;
	}
	
	public static Boolean neqeq(double a, double b) {
		return a != b;
	}

	public static Class<?> javaType() {
		return Double.class;
	}

	public static double plus(double a, double b) {
		return a + b;
	}

	public static double minus(double a, double b) {
		return a - b;
	}

	public static double neg(double a) {
		return -a;
	}

	public static double inc(double a) {
		return a + 1;
	}
	
	public static double dec(double a) {
		return a - 1;
	}

	public static double div(double a, double b) {
		return a / b;
	}
	
	public static double rem(double a, double b) {
		return a % b;
	}

	public static double mul(double a, double b) {
		return a * b;
	}

	public static double pow(double a, double b) {
		return Math.pow(a, b);
	}

	public static double abs(double a) {
		return Math.abs(a);
	}
	
	public static double max(double a, double b) {
		return Math.max(a, b);
	}
	
	public static double min(double a, double b) {
		return Math.min(a, b);
	}
	
	public static double sqrt(double a) {
		return Math.sqrt(a);
	}
	
	public static String toString(Object a) {
		return String.valueOf(a);
	}
	
	public static void doesNotUnderstand(String op, Object o, Object...args) {
		System.err.println(o.toString() + " does not understand mathod " + op + " with args " + Arrays.deepToString(args));
	}

}
