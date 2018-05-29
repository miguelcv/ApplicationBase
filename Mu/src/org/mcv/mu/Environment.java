package org.mcv.mu;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.mcv.mu.Interpreter.InterpreterError;

class Environment {

	final Environment enclosing;
	String name;
	private final Map<String, Pair<Result, Boolean>> values = new HashMap<>();
	private final Map<String, Pair<Result, Boolean>> publicValues = new HashMap<>();
	private static final String UNDEFINED = "Undefined variable '%s'.";
	
	Environment(String name) {
		this.name = name;
		enclosing = null;
	}

	Environment(String name, Environment enclosing) {
		this.name = name;
		this.enclosing = enclosing;
	}

	public Result get(Token name) {
		Result ret = get(name.lexeme);
		if(ret != null) return ret;
		throw new InterpreterError(UNDEFINED, name.lexeme);
	}

	Result get(String key) {
		if (values.containsKey(key)) {
			return values.get(key).left;
		}
		if (enclosing != null)
			return enclosing.get(key);
		return null;
	}

	Result getPublic(Token name) {
		Result ret = getPublic(name.lexeme);
		if (ret != null)
			throw new InterpreterError(UNDEFINED, name.lexeme);
		return ret;
	}

	Result getPublic(String key) {
		if (publicValues.containsKey(key)) {
			return publicValues.get(key).left;
		}
		if (enclosing != null)
			return enclosing.get(key);
		return null;
	}

	Result assign(Token name, Result result) {
		if (publicValues.containsKey(name.lexeme)) {
			if (publicValues.get(name.lexeme).right) {
				checkType(name.lexeme, publicValues.get(name.lexeme).left, result);
				publicValues.put(name.lexeme, new Pair<Result, Boolean>(result, true));
			}
		}
		if (values.containsKey(name.lexeme)) {
			if (values.get(name.lexeme).right) {
				checkType(name.lexeme, values.get(name.lexeme).left, result);
				values.put(name.lexeme, new Pair<>(result, true));
				return result;
			} else {
				throw new InterpreterError("Variable '%s' is constant", name.lexeme);
			}
		}
		if (enclosing != null) {
			enclosing.assign(name, result);
			return result;
		}
		throw new InterpreterError(UNDEFINED, name.lexeme);
	}

	Result define(String name, Result result, boolean mutable, boolean pub) {
		if (values.containsKey(name)) {
			checkType(name, values.get(name).left, result);
		}
		values.put(name, new Pair<>(result, mutable));
		if(pub) publicValues.put(name, new Pair<>(result, mutable));
		return result;
	}

	void checkType(String name, Result currentVal, Result newVal) {
		if (!currentVal.type.name.equals(newVal.type.name)) {
			if (currentVal.type.name.equals("Any")) {
				currentVal.type = newVal.type;
			} else {
				throw new InterpreterError("Type of variable %s (%s) does not match type %s of value (%s)", 
						name, currentVal.type, newVal.type, newVal.value);
			}
		}
	}

	public Environment capture(String name) {
		Environment ret = new Environment(name);
		for(Entry<String, Pair<Result, Boolean>> entry : values.entrySet()) {
			Result value = copyIfMutable(entry.getValue().left, entry.getValue().right);
			ret.define(entry.getKey(), value, entry.getValue().right, false);
		}
		for(Entry<String, Pair<Result, Boolean>> entry : publicValues.entrySet()) {
			Result value = copyIfMutable(entry.getValue().left, entry.getValue().right);
			ret.define(entry.getKey(), value, entry.getValue().right, true);
		}
		if(enclosing != null) {
			ret.merge(enclosing.capture(enclosing.name));
		}
		return ret;
	}

	private void merge(Environment other) {
		for(Entry<String, Pair<Result, Boolean>> entry : other.values.entrySet()) {
			Result value = copyIfMutable(entry.getValue().left, entry.getValue().right);
			define(entry.getKey(), value, entry.getValue().right, false);
		}
		for(Entry<String, Pair<Result, Boolean>> entry : other.publicValues.entrySet()) {
			Result value = copyIfMutable(entry.getValue().left, entry.getValue().right);
			define(entry.getKey(), value, entry.getValue().right, true);
		}
	}

	private Result copyIfMutable(Result val, boolean mutable) {
		if(mutable) {
			return new Result(val.value, val.type);
		} else {
			return val;
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

	public Environment topLevel() {
		Environment env = this;
		while(env.enclosing != null) 
			env = env.enclosing;
		return env;
	}

}