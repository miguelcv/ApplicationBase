package org.mcv.mu;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.mcv.mu.Interpreter.InterpreterError;

class Environment {

	static class TableEntry {

		public TableEntry(Object value, Type type, boolean mutable, boolean publiq) {
			this.value = value;
			this.type = type;
			this.mutable = mutable;
			this.publiq = publiq;
		}

		Object value;
		Type type;
		final boolean mutable;
		final boolean publiq;

		public Result result() {
			return new Result(value, type);
		}
		@Override public String toString() {
			return String.valueOf(this.value);
		}
	}

	final Environment enclosing;
	String name;
	final Map<String, TableEntry> values = new HashMap<>();
	private static final String UNDEFINED = "Undefined variable '%s' in environment %s.";

	Environment(String name) {
		this.name = name;
		enclosing = null;
	}

	Environment(String name, Environment enclosing) {
		this.name = name;
		this.enclosing = enclosing;
	}

	String printenv() {
		return this.name + (enclosing == null ? "" : "=>" + enclosing.printenv());
	}

	public Result get(Token name) {
		Result ret = get(name.lexeme);
		if (ret != null)
			return ret;
		throw new InterpreterError(UNDEFINED, name.lexeme, printenv());
	}

	Result get(String key) {
		if (values.containsKey(key)) {
			return values.get(key).result();
		}
		if (enclosing != null)
			return enclosing.get(key);
		return null;
	}

	Result assign(Token name, Result result) {
		String key = name.lexeme;
		return assign(key, result);
	}
	
	Result assign(String key, Result result) {
		if (values.containsKey(key)) {
			TableEntry entry = values.get(key);
			if (entry.mutable) {
				Type type = checkType(key, entry.value, entry.type, result.type, result.value);
				entry.type = type;
				entry.value = result.value;
				return entry.result();
			} else {
				throw new InterpreterError("Variable '%s' is constant", key);
			}
		}
		if (enclosing != null) {
			enclosing.assign(key, result);
			return result;
		}
		throw new InterpreterError(UNDEFINED, key, printenv());
	}

	private boolean isUndefined(Object value) {
		return value != null && value.equals(Type.UNDEFINED);
	}

	Result define(String name, Result result, boolean mutable, boolean pub) {
		if (values.containsKey(name)) {
			TableEntry e = values.get(name);
			Type type = checkType(name, e.value, e.type, result.type, result.value);
			values.get(name).value = result.value;
			values.get(name).type = type;
		} else {
			if (result.type.name.equals("None")) {
				throw new InterpreterError("Cannot assign type None to variable %s", name);
			}
			values.put(name, new TableEntry(result.value, result.type, mutable, pub));
		}
		return values.get(name).result();
	}

	Type checkType(String name, Object value, Type current, Type newType, Object newValue) {
		if (newType.equals(Type.None)) {
			throw new InterpreterError("Cannot assign type None to variable %s", name);
		}
		if (current.equals(Type.Any) && !isUndefined(value)) {
			return newType;
		}
		current = Type.evaluate(current, this);
		newType = Type.evaluate(newType, this);
		if (!current.matches(newType)) {
			throw new InterpreterError("Type of variable %s (%s) does not match value %s", name, current, newValue);
		}
		return current;
	}
	
	public Environment capture(String name) {
		Environment ret = new Environment(name);
		for (Entry<String, TableEntry> entry : values.entrySet()) {
			TableEntry te = entry.getValue();
			String key = checkKey(entry.getKey());
			if(key != null) ret.define(key, te.result(), te.mutable, te.publiq);
		}
		if (enclosing != null) {
			ret.merge(enclosing.capture(enclosing.name));
		}
		return ret;
	}

	private void merge(Environment other) {
		for (Entry<String, TableEntry> entry : other.values.entrySet()) {
			TableEntry te = entry.getValue();
			String key = checkKey(entry.getKey());
			if(key != null) define(key, te.result(), te.mutable, te.publiq);
		}
	}

	private String checkKey(String k) {
		/* exclude any synthetic fields */
		if(k.startsWith("$")) return null;
		return k;
	}
	
	@Override
	public String toString() {
		String result = values.toString();
		if (enclosing != null) {
			result += " -> " + enclosing.toString();
		}
		return result;
	}

	public Environment topLevel() {
		Environment env = this;
		while (env.enclosing != null)
			env = env.enclosing;
		return env;
	}

	public void undefine(String key) {
		values.remove(key);
	}

}