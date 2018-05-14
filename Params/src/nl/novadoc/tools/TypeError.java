package nl.novadoc.tools;

public class TypeError extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	TypeError(String message, Object...args) {
		super(String.format(message, args));
	}
}
