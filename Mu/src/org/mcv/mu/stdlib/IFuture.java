package org.mcv.mu.stdlib;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.mcv.math.BigInteger;
import org.mcv.mu.Result;
import org.mcv.mu.Type;

public class IFuture {
	
	public static Boolean eq(Future<Result> a, Future<Result> b) {
		return a.equals(b);
	}
	
	public static Boolean neq(Future<Result> a, Future<Result> b) {
		return !eq(a, b);
	}

	public static Boolean eqeq(Future<Result> a, Future<Result> b) {
		return a.equals(b);
	}
	
	public static Boolean neqeq(Future<Result> a, Future<Result> b) {
		return !eq(a, b);
	}

	public static Future<Result> id(Future<Result> a) {
		return a;
	}

	public static Object deref(Future<Result> a) {
		try {
			return a.get().value;
		} catch(Exception e) {
			return new Result(e, Type.Exception);
		}
	}

	public static String toString(Future<Result> a) {
		return String.valueOf(a);
	}

	public static boolean isResolved(Future<Result> a) {
		return a.isDone();
	}

	public static Boolean not(Future<Result> a) {
		if (a.isDone()) return true;
		return false;
	}

	public static Object await(Future<Result> a, BigInteger timeout) {
		try {
			return a.get(timeout.longValue(), TimeUnit.MILLISECONDS).value;
		} catch(TimeoutException e) {
			return new Result(null, Type.Void);
		} catch(Exception e) {
			return new Result(e, Type.Exception);			
		}
	}

}
