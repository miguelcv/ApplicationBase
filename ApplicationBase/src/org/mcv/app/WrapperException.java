package org.mcv.app;

public class WrapperException extends RuntimeException {
	private static final long serialVersionUID = 862482220065138139L;
	
	public WrapperException(Throwable e) {
		super(e);
	}

	public static Throwable unwrap(Throwable e) {
		if(e == null) return new Exception("null");
		Throwable ret = e;
		while(ret instanceof WrapperException) {
			ret = ret.getCause();
		}
		return ret;
	}
}
