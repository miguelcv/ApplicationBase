package org.mcv.mu;

public class MuException extends RuntimeException {

	final transient Result value;
	transient int line;
	transient Expr expr;
	
	public MuException(Result value) {
		this.value = value;
	}

	public Object getValue() {
		return value.value;
	}

	private static final long serialVersionUID = 1L;
	
	@Override public String toString() {
		return String.valueOf(value.value);
	}

}
