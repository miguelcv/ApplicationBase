package org.mcv.mu;

public class MuException extends RuntimeException {

	final transient Result value;
	transient int line;
	transient Expr expr;
	
	public MuException(Result value) {
		this.value = value;
	}

	private static final long serialVersionUID = 1L;

}
