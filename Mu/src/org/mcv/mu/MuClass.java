package org.mcv.mu;

import java.util.List;
import java.util.Map;

public class MuClass implements MuCallable {

	final String name;
	final MuClass superclass;
	private final Map<String, MuFunction> methods;

	MuClass(String name, MuClass superclass, Map<String, MuFunction> methods) {
		this.superclass = superclass;
		this.name = name;
		this.methods = methods;
	}

	MuFunction findMethod(MuInstance instance, String name) {
		if (methods.containsKey(name)) {
			return methods.get(name).bind(instance);
		}

		if (superclass != null) {
			return superclass.findMethod(instance, name);
		}

		return null;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		MuInstance instance = new MuInstance(this);
		MuFunction initializer = methods.get("init");
		if (initializer != null) {
			initializer.bind(instance).call(interpreter, arguments);
		}

		return instance;
	}

	@Override
	public int arity() {
		MuFunction initializer = methods.get("init");
		if (initializer == null)
			return 0;
		return initializer.arity();
	}
}
