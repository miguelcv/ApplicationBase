package org.mcv.mu;

import java.util.List;

class MuFunction implements MuCallable {
	private final String name;
	private final Expr.Function declaration;
	private final Environment closure;
	private final boolean isInitializer;

	MuFunction(String name, Expr.Function declaration, 
			Environment closure, boolean isInitializer) {
		this.name = name;
		this.isInitializer = isInitializer;
		this.closure = closure;
		this.declaration = declaration;
	}

	MuFunction bind(MuInstance instance) {
		Environment environment = new Environment(closure);
		environment.define("this", instance, instance.klass.name);
		return new MuFunction(name, declaration, environment, isInitializer);
	}

	@Override
	public String toString() {
		if(name == null) return "<fn>";
		return "<fn " + name + ">";
	}

	@Override
	public int arity() {
		return declaration.parameters.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		Environment environment = new Environment(closure);
		
		if(declaration.parameters != null) {
			for (int i = 0; i < declaration.parameters.size(); i++) {
				environment.define(declaration.parameters.get(i).lexeme, 
						arguments.get(i), 
						// FIXME!!
						Interpreter.typeFromValue(declaration.parameters.get(i).literal));
			}
		}
		
		try {
			interpreter.executeBlock(declaration.body, environment);
		} catch (MuReturn returnValue) {
			return returnValue.value;
		}

		if (isInitializer)
			return closure.get("this");
		return null;
	}
	
	// FIXME
	public boolean isGetter() {
		return declaration.parameters == null;
	}
}