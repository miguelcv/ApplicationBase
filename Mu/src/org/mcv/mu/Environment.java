package org.mcv.mu;

import java.util.HashMap;
import java.util.Map;

class Environment {

	final Environment enclosing;
	private final Map<String, Pair<Object, Type>> values = new HashMap<>();
	private final Map<String, Pair<Object, Type>> publicValues = new HashMap<>();
	private static final String UNDEFINED = "Undefined variable '%s'.";
	
	Environment() {
		enclosing = null;
	}

	Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}

	public Object get(Token name) {
		Object ret = get(name.lexeme);
		if(ret != null) return ret;
		return Mu.runtimeError(UNDEFINED, name.lexeme);
	}

	Object get(String key) {
		if (values.containsKey(key)) {
			return values.get(key).left;
		}
		if (enclosing != null)
			return enclosing.get(key);
		return null;
	}

	public Object getType(Token name) {
		Object ret = getType(name.lexeme);
		if(ret == null) return Mu.runtimeError(UNDEFINED, name.lexeme);
		return ret;
	}

	Object getType(String key) {
		if (values.containsKey(key)) {
			return values.get(key).right;
		}
		if (enclosing != null)
			return enclosing.get(key);
		return null;
	}

	Object getPublic(Token name) {
		Object ret = getPublic(name.lexeme);
		if (ret != null)
			return Mu.runtimeError(UNDEFINED, name.lexeme);
		return ret;
	}

	Object getPublic(String key) {
		if (publicValues.containsKey(key)) {
			return publicValues.get(key).left;
		}
		if (enclosing != null)
			return enclosing.get(key);
		return null;
	}

	Object assign(Token name, Object value, Type type) {
		if (publicValues.containsKey(name.lexeme)) {
			if (publicValues.get(name.lexeme).mutable) {
				checkType(name.lexeme, publicValues.get(name.lexeme), value, type);
				publicValues.put(name.lexeme, new Pair<>(value, type, true));
			}
		}
		if (values.containsKey(name.lexeme)) {
			if (values.get(name.lexeme).mutable) {
				checkType(name.lexeme, values.get(name.lexeme), value, type);
				values.put(name.lexeme, new Pair<>(value, type, true));
				return value;
			} else {
				Mu.runtimeError("variable '%s' is constant", name.lexeme);
				return null;
			}
		}
		if (enclosing != null) {
			enclosing.assign(name, value, type);
			return value;
		}
		return Mu.runtimeError(UNDEFINED, name.lexeme);
	}

	Object define(String name, Object value, Type type, boolean mutable, boolean shared) {
		if (values.containsKey(name)) {
			checkType(name, values.get(name), value, type);
		}
		values.put(name, new Pair<>(value, type, mutable));
		if(shared) publicValues.put(name, new Pair<>(value, type, mutable));
		return value;
	}

	void checkType(String name, Pair<Object, Type> value, Object newVal, Type type) {
		// TODO UNION/INTERSECTION TYPES!!
		if (!value.right.name.equals(type.name)) {
			if (value.right.name.equals("Any")) {
				value.right = type;
			} else {
				Mu.runtimeError("Type of variable %s (%s) does not match value (%s)", name, value.right, newVal);
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