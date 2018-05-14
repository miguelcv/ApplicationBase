package org.mcv.mu;

import java.util.HashMap;
import java.util.Map;

class MuInstance {
	MuClass klass;
	private final Map<String, Object> fields = new HashMap<>();

	MuInstance(MuClass klass) {
		this.klass = klass;
	}

	Object get(Token name) {
		if (fields.containsKey(name.lexeme)) {
			return fields.get(name.lexeme);
		}
		MuFunction method = klass.findMethod(this, name.lexeme);
		if (method != null)
			return method;

		throw new RuntimeError(name, // [hidden]
				"Undefined property '" + name.lexeme + "'.");
	}

	void set(Token name, Object value) {
		fields.put(name.lexeme, value);
	}

	@Override
	public String toString() {
		return klass.name + " instance";
	}
}