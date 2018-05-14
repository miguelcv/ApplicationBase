package nl.novadoc.tools;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.Collections;

public class Main {

	static class Atom {
		final String value;

		public Atom(String value) {
			this.value = value;
		}
	}

	static class DottedPair {
		Object car;
		Object cdr;
	}

	static boolean isNull(Object x) {
		return x == null;
	}

	static boolean atom(Object x) {
		if (isNull(x))
			return true;
		return (x instanceof Atom);
	}

	static boolean eq(Object x, Object y) {
		return x == y;
	}

	static Object car(Object x) {
		if (atom(x))
			return null;
		return ((DottedPair) x).car;
	}

	static Object cdr(Object x) {
		if (atom(x))
			return null;
		return ((DottedPair) x).cdr;
	}

	static DottedPair cons(Object x, Object y) {
		DottedPair dp = new DottedPair();
		dp.car = x;
		dp.cdr = y;
		return dp;
	}

	static void print(Object x) {
		if (isNull(x)) {
			System.out.printf("NIL\n");
		} else if (atom(x)) {
			System.out.printf("%s\n", ((Atom) x).value);
		} else {
			System.out.printf("(");
			printcons((DottedPair) x);
			System.out.printf(")\n");
		}
	}

	static void print2(Object x) {
		if (isNull(x)) {
			System.out.printf("NIL");
		} else if (atom(x)) {
			System.out.printf("%s", ((Atom) x).value);
		} else {
			System.out.printf("(");
			printcons((DottedPair) x);
			System.out.printf(")");
		}
	}

	static void printcons(DottedPair dp) {
		print2(dp.car);
		if (!isNull(dp.cdr) && !atom(dp.cdr)) {
			System.out.printf(" ");
			printcons((DottedPair) dp.cdr);
		} else if (!isNull(dp.cdr)) {
			System.out.printf(" . ");
			print2(dp.cdr);
		}
	}

	static DottedPair list(Object... car) {
		DottedPair dp = null;
		Collections.reverse(Arrays.asList(car));
		for (Object p : car) {
			dp = cons(p, dp);
		}
		return dp;
	}

	public static void main(String[] args) {
		Atom a = new Atom("a");
		Atom b = new Atom("b");
		Atom c = new Atom("c");
		DottedPair lst;
		lst = cons(c, null);
		lst = cons(b, lst);
		lst = cons(a, lst);

		print(a);
		print(b);
		print(c);
		print(lst);

		print(cons(null, null));
		print(cons(null, a));
		print(cons(null, lst));
		print(cons(a, null));
		print(cons(a, b));
		print(cons(a, lst));
		print(cons(lst, null));
		print(cons(lst, a));
		print(cons(lst, lst));

		print(list(a, b, c));

		// normalization
		String s = "schön";
		dump(s);
		if (!Normalizer.isNormalized(s, Form.NFD)) {
			s = Normalizer.normalize(s, Form.NFD);
			dump(s);
		}

		boolean bv = true;
		bv &= getb(false);
		System.out.println("\nValue is " + bv);

		bv = true;
		bv &= getb(true);
		System.out.println("Value is " + bv);

		bv = false;
		bv &= getb(false);
		System.out.println("Value is " + bv);

		bv = false;
		bv &= getb(true);
		System.out.println("Value is " + bv);
	}

	static boolean getb(boolean in) {
		System.out.println("getb called");
		return in;
	}

	private static void dump(CharSequence s) {
		s.codePoints().forEach((p) -> System.out.printf("%c ", p));
	}
}
