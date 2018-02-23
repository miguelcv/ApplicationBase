package org.mcv.git;

public class GitException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public GitException(String string) {
		super(string);
	}

	public GitException(Exception e) {
		super(e);
	}

}
