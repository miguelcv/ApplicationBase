package org.mcv.app;

import lombok.NonNull;

public class WrapperException extends RuntimeException {
	private static final long serialVersionUID = 862482220065138139L;
	
	public WrapperException(Throwable e) {
		super(e);
	}

	public static @NonNull Throwable unwrap(Throwable e) {
		if(e == null) throw new NullPointerException();
		Throwable ret = e;
		while(ret instanceof WrapperException) {
			if(ret.getCause() == null) {
				return ret;
			}
			ret = ret.getCause();
		}
		return ret;
	}
	
	@Override
	public String getMessage() {
		return unwrap(this).getMessage();
	}

	@Override
	public StackTraceElement[] getStackTrace() {
		return unwrap(this).getStackTrace();
	}

}
