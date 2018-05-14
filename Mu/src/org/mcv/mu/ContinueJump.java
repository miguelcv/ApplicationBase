package org.mcv.mu;

class ContinueJump extends RuntimeException {
	private static final long serialVersionUID = 1L;

	ContinueJump() {
        super("continue jump");
    }
}