package org.mcv.mu;

import lombok.Data;

@Data
public class Pair<T, U> {

	public Pair(T left, U right) {
		this.left = left;
		this.right = right;
	}
	
	T left;
	U right;
}
