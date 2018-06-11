package org.mcv.mu;

public class Mixin {
	
	public Mixin(Callee object, ListMap<String> where) {
		this.object = object;
		this.where = where;
	}

	Callee object;
	ListMap<String> where = new ListMap<>();
}
