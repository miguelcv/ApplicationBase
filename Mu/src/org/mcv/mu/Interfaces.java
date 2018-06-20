package org.mcv.mu;

import java.util.HashMap;
import java.util.Map;

public class Interfaces {

	Map<String, Map<Signature, Object>> interfaces = new HashMap<>();

	private String shortname(String name) {
		int index = name.indexOf('[');
		if(index >= 0) {
			return name.substring(0, index);
		}
		return name;
	}
	
	public Map<Signature, Object> get(String name) {
		return interfaces.get(shortname(name));
	}

	public void put(String name, Map<Signature, Object> intface) {
		interfaces.put(shortname(name),intface);
	}

}
