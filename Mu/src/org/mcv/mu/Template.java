package org.mcv.mu;

import java.util.ArrayList;
import java.util.Stack;

import org.mcv.mu.Expr.Block;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Params.ParamFormal;

public class Template {

	String kind;
	Expr def;
	Attributes attributes;
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
	
	Template() {
	}
	
	Template(String name, Environment env) {
		params = new Params(true);
		params.add(new ParamFormal("*", Type.Any, null, null, new Attributes()));
		Token tok = new Token(Keyword.FUN, "main", "main", 0);
		body = new Block(tok, new ArrayList<>());
		attributes = new Attributes();
		def = new TemplateDef(tok, "fun", params, Type.Void, body, attributes);
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
		this.name = stmt.name.lexeme;
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

	public static synchronized Result call(Template self, Interpreter interpreter, Expr.Map args) {
		Callee callee = new Callee(self);
		callee.params = new Params(self.params);
		callee.def = (TemplateDef)self.def;
		
		callee.params.call(interpreter, callee, args);
		
		if (callee.params.curry) {
			Template curried = new Template((TemplateDef)self.def, callee.closure);
			curried.params = callee.params;
			return new Result(curried, new Type.SignatureType((TemplateDef)self.def));
		}
		if(!callee.params.rest.isEmpty()) {
			callee.closure.define("$rest", new Result(callee.params.rest, new Type.MapType(Type.Any)), false, false);
		} else {
			callee.closure.define("$rest", new Result(new ListMap<>(), new Type.MapType(Type.Any)), false, false);
		}
		callee.closure.define("$arguments", new Result(callee.params.toMap(), new Type.MapType(Type.Any)), false, true);

		callee.attributes = self.attributes;
		
		// add THIS
		callee.closure.define("$this", new Result(callee, new Type.SignatureType((TemplateDef) self.def)), false, false);
				
		callee.around = self.around;
		callee.befores = copyStack(self.befores);
		callee.afters = copyStack(self.afters);
		callee.errors = copyStack(self.errors);
		callee.alwayses = copyStack(self.alwayses);		
		return Callee.call(interpreter, callee);
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
