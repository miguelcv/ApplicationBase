package org.mcv.uom;

public class UoMError extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UoMError(String fmt, Object... args) {
		super(String.format(fmt, args));
	}
}
