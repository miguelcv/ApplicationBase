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
	
	//public boolean ok() {
	//	return left != null && right != null;
	//}

	//public boolean unary() {
	//	return left != null;
	//}

}
