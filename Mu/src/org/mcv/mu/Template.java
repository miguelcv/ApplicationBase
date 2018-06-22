package org.mcv.mu;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.mcv.mu.Expr.Block;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Params.ParamFormal;

public class Template {

	String kind;
	TemplateDef def;
	Attributes attributes = new Attributes();
	String name;
	Params params;
	Type returnType;
	Expr.Block body;
	protected Environment closure;
	public Environment closure() {
		return closure;
	}
	Block around = null;
	Stack<Block> befores = new Stack<>();
	Stack<Block> afters = new Stack<>();
	Stack<Block> errors = new Stack<>();
	Stack<Block> alwayses = new Stack<>();
	
	/* JVM classes */
	Class<?> javaClass = null;
	List<Method> javaMethodList = new ArrayList<>();
	List<Constructor<?>> javaConstructorList = new ArrayList<>();
	
	Template() {
	}
	
	Template(String name, Environment env) {
		params = new Params(true);
		params.add(new ParamFormal("*", Type.Any, null, null, new Attributes()));
		body = new Block(-1, new ArrayList<>());
		def = new TemplateDef("main", 0, "fun", params, Type.Void, body, attributes);
		kind = "fun";
		this.name = name;
		returnType = Type.Void;
		closure = env;
	}

	public Template(TemplateDef stmt, Environment closure) {
		this.def = stmt;
		this.kind = stmt.kind;
		this.attributes = stmt.attributes;
		this.body = stmt.body;
		this.name = stmt.name;
		this.params = stmt.params;
		this.returnType = stmt.type;
		this.closure = closure;
	}

	public Object get(String key) {
		return params.get(key);
	}

	public void set(String key, Object value) {
		params.set(key, value);
	}

	public static synchronized Result call(Callee callee, Interpreter interpreter, Expr.Map args) {
		callee.params = new Params(callee.parent.params);
		
		callee.params.call(interpreter, callee, args);
		
		if (callee.params.curry) {
			Template curried = new Template((TemplateDef)callee.parent.def, callee.closure);
			curried.params = callee.params;
			return new Result(curried, new Type.SignatureType((TemplateDef)callee.parent.def));
		}
		if(!callee.params.rest.isEmpty()) {
			callee.closure.define("$rest", new Result(callee.params.rest, new Type.MapType(Type.Any)), false, false);
		} else {
			callee.closure.define("$rest", new Result(new ListMap<>(), new Type.MapType(Type.Any)), false, false);
		}
		callee.closure.define("$arguments", new Result(callee.params.toMap(), new Type.MapType(Type.Any)), false, true);
		
		// add THIS
		callee.closure.define("$this", new Result(callee, new Type.SignatureType((TemplateDef)callee.parent.def)), false, false);
				
		callee.around = callee.parent.around;
		callee.befores = copyStack(callee.parent.befores);
		callee.afters = copyStack(callee.parent.afters);
		callee.errors = copyStack(callee.parent.errors);
		callee.alwayses = copyStack(callee.parent.alwayses);
		callee.javaClass = callee.parent.javaClass;
		callee.javaMethodList = callee.parent.javaMethodList;
		return callee.call(interpreter);
	}
		
	public static synchronized Result call(Template self, Interpreter interpreter, Expr.Map args) {
		Callee callee = new Callee(self);
		return call(callee, interpreter, args);
	}

	private static Stack<Block> copyStack(Stack<Block> stk) {
		Stack<Block> ret = new Stack<>();
		for(Block block : stk) {
			ret.push(block);
		}
		return ret;
	}

	@Override public String toString() {
		return kind.toLowerCase() + " " + name;
	}
}
