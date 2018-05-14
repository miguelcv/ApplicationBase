package org.mcv.mu;

public class TypeInfo {
	
	public TypeInfo(String name, Class<?> trait) {
		this.name = name;
		this.trait = trait;
	}
	
	final String name;
	final Class<?> trait;
}
