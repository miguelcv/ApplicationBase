package org.mcv.mu;

import java.util.HashMap;
import java.util.Map;

class Environment {
	
	final Environment enclosing;
	private final Map<String, Pair<Object, String>> values = new HashMap<>();

	Environment() {
		enclosing = null;
	}

	Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}

	Object get(Token name) {
		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme).left;
		}
		if (enclosing != null)
			return enclosing.get(name);
		return Mu.runtimeError("Undefined variable '%s'.", name.lexeme);
	}

	Object get(String key) {
		if (values.containsKey(key)) {
			return values.get(key).left;
		}
		if (enclosing != null)
			return enclosing.get(key);
		return null;
	}

	Object getType(Token name) {
		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme).right;
		}
		if (enclosing != null)
			return enclosing.get(name);
		return Mu.runtimeError("Undefined variable '%s'.", name.lexeme);
	}

	Object getType(String key) {
		if (values.containsKey(key)) {
			return values.get(key).right;
		}
		if (enclosing != null)
			return enclosing.get(key);
		return null;
	}

	Object assign(Token name, Object value, String type) {
		if (values.containsKey(name.lexeme)) {
			checkType(name.lexeme, values.get(name.lexeme), type);
			values.put(name.lexeme, new Pair<>(value, type));
			return value;
		}
		if (enclosing != null) {
			enclosing.assign(name, value, type);
			return value;
		}
		return Mu.runtimeError("Undefined variable '%s'.", name.lexeme);
	}

	Object define(String name, Object value, String type) {
		if(values.containsKey(name)) {
			checkType(name, values.get(name), type);
		}
		values.put(name, new Pair<>(value, type));
		return value;
	}

	void checkType(String name, Pair<Object, String> value, String type) {
		// UNION/INTERSECTION TYPES!!
		if(!value.right.equals(type)) {
			if(value.right.equals("Any")) {
				value.right = type;
			} else {
				Mu.runtimeError("Type of variable %s (%s) does not match value (%s)", name, value.right, value);
			}
		}
	}
	
	@Override
	public String toString() {
		String result = values.toString();
		if (enclosing != null) {
			result += " -> " + enclosing.toString();
		}
		return result;
	}

}