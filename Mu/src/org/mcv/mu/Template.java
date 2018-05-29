package org.mcv.mu;

import java.util.Map.Entry;
import java.util.Stack;

import org.mcv.mu.Expr.Block;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Expr.Val;
import org.mcv.mu.Expr.Var;
import org.mcv.mu.Params.ParamFormal;

public class Template {

	String kind;
	Expr def;
	Attributes attributes;
	String name;
	Params params;
	ListMap<Result> interfaces = new ListMap<>();
	Type returnType;
	Expr.Block body;
	Environment closure;
	
	Stack<Block> arounds = new Stack<>();
	Stack<Block> befores = new Stack<>();
	Stack<Block> afters = new Stack<>();
	Stack<Block> errors = new Stack<>();
	Stack<Block> alwayses = new Stack<>();
	
	Template() {
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

	public void aop(String which, Block block) {
		switch(which) {
		case "around":
			arounds.push(block);
			break;
		case "before":
			befores.push(block);
			break;
		case "after":
			afters.push(block);
			break;
		case "error":
			errors.push(block);
			break;
		case "always":
			alwayses.push(block);
			break;
		default:
			break;
		}
	}
	
	public static synchronized Result call(Template self, Interpreter interpreter, ListMap<Object> args) {
		Callee callee = new Callee(self);
		callee.params = new Params(self.params);
		callee.def = (TemplateDef)self.def;
		
		// evaluate defvals!!
		for(Entry<String, ParamFormal> pf : callee.params.listMap.entrySet()) {
			if(pf.getValue().defval instanceof Expr) {
				pf.getValue().defval = interpreter.evaluate((Expr)pf.getValue().defval).value;
			}
		}

		callee.params.call(args);
		if (callee.params.curry) {
			Template curried = new Template((TemplateDef)self.def, self.closure);
			curried.params = callee.params;
			return new Result(curried, new Type.SignatureType((TemplateDef)self.def));
		}
		if(!callee.params.rest.isEmpty()) {
			callee.closure.define("rest", new Result(callee.params.rest, new Type.MapType(Type.String, Type.Any)), false, false);
		} else {
			callee.closure.define("rest", new Result(new ListMap<>(), new Type.MapType(Type.String, Type.Any)), false, false);
		}
		callee.attributes = self.attributes;
		callee.interfaces = new ListMap<>(self.interfaces);
		callee.closure = new Environment("callee " + self.name, self.closure);
		
		// add the actual parameters
		for (Entry<String, Params.ParamFormal> entry : callee.params.listMap.entrySet()) {
			// class parameters are mutable, function parameters are not
			boolean mutable = self.kind.equalsIgnoreCase(Keyword.CLASS.name()) ? true : false; 
			callee.closure.define(entry.getKey(), new Result(entry.getValue().val, entry.getValue().type), mutable, false);
			// if prop, add getter to interfaces
			if(entry.getValue().attributes.isProp()) {
				callee.interfaces.put(entry.getKey(), new Result(
						new Property(
							new Token(Soperator.ID, entry.getKey(), entry.getKey(), -1), 
							entry.getValue().type,
							callee.closure
							),
						entry.getValue().type));			
			}
		}
		// add THIS
		callee.closure.define("this", new Result(callee, new Type.SignatureType((TemplateDef) self.def)), false, false);
		
		// add the non-own properties
		for(Expr expr : self.body.expressions) {
			if(expr instanceof Expr.Val) {
				Val def = (Val)expr;
				if(def.attributes.isProp() && !def.attributes.isOwn()) {
					callee.interfaces.put(def.name.lexeme, new Result(new Property(def.name, def.type, callee.closure), def.type));
				}
			}
	
			if(expr instanceof Expr.Var) {
				Var def = (Var)expr;
				if(def.attributes.isProp() && !def.attributes.isOwn()) {
					callee.interfaces.put(def.name.lexeme, new Result(new Property(def.name, def.type, callee.closure), def.type));
				}				
			}
		}
		
		callee.arounds = copyStack(self.arounds);
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
