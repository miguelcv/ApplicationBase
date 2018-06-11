package org.mcv.mu;

public class Result {
	
	Result() {}
	
	public Result(Object value, Type type) {
		this.value = value;
		this.type = type;
	}
	Object value;
	Type type;
	
	@Override public String toString() {
		return value + ":" + type;
	}
}
