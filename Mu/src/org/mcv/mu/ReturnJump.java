package org.mcv.mu;

class ReturnJump extends RuntimeException {

	private static final long serialVersionUID = 1L;
	transient final Result value;

	ReturnJump(Result value) {
		super(null, null, false, false);
		this.value = value;
	}
}