package org.mcv.app;

public class ApplicationException extends RuntimeException {
	private static final long serialVersionUID = 862482220065138139L;
	
	public ApplicationException(String s) {
		super(s);
	}

	public ApplicationException(String fmt, Object... args) {
		super(String.format(fmt, args));
	}

}
