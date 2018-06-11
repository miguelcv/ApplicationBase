package org.mcv.mu.stdlib;

import org.mcv.mu.Type;

public class IType {
	public static Type and(Type a, Type b) {
		return new Type.IntersectionType(a, b);
	}

	public static Type or(Type a, Type b) {
		return new Type.UnionType(a, b);
	}
}
