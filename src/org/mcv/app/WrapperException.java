package org.mcv.app;

public class WrapperException extends RuntimeException {
	private static final long serialVersionUID = 862482220065138139L;
	
	public WrapperException(Throwable e) {
		super(e);
	}

	public Throwable unwrap() {
		Throwable ret = this;
		while(ret instanceof WrapperException) {
			ret = ret.getCause();
		}
		return ret;
	}
}
