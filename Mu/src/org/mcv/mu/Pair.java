package org.mcv.mu;

import lombok.Data;

@Data
public class Pair<T, U> {

	public Pair(T left, U right, boolean mutable) {
		this.left = left;
		this.right = right;
		this.mutable = mutable;
	}
	
	T left;
	U right;
	final boolean mutable;
	
	public boolean ok() {
		return left != null && right != null;
	}

	public boolean unary() {
		return left != null;
	}

}
