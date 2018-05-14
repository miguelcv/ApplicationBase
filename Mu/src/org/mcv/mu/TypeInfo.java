package org.mcv.mu;

public class TypeInfo {
	
	public TypeInfo(Types name, Class<?> trait) {
		this.name = name;
		this.trait = trait;
	}
	
	final Types name;
	final Class<?> trait;
}
