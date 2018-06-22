package org.mcv.mu;

public class Undefined {

	static Undefined instance = new Undefined();
	
	public static Undefined getInstance() {
		return instance;
	}

	@Override public String toString() {
		return "undefined";
	}
}
