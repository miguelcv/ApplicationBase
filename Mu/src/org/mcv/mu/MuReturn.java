package org.mcv.mu;

class MuReturn extends RuntimeException {

	private static final long serialVersionUID = 1L;
	transient final Object value;

	MuReturn(Object value) {
		super(null, null, false, false);
		this.value = value;
	}
}