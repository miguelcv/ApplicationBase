package org.mcv.mu.stdlib;

import org.mcv.mu.MuException;

public class IException {
	
	public static String toString(MuException e) {
		return String.valueOf(e.getValue());
	}
}
