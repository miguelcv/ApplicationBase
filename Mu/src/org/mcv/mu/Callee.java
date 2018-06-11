package org.mcv.mu;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.mcv.mu.Expr.Block;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Interpreter.InterpreterError;

public class Callee {
	/* prevent infinite loop in AROUND */
	private static final String IN_AROUND = "__inaround";
	
	Template parent;
	Attributes attributes;
	protected Environment closure;
	public Environment closure() {
		return closure;
	}
	Params params;
	List<Mixin> mixins = new ArrayList<>();
	TemplateDef def;
	Interpreter mu;
	Block around;
	Stack<Block> befores;
	Stack<Block> afters;
	Stack<Block> errors;
	Stack<Block> alwayses;

	public Callee(Template tmpl) {
		parent = tmpl;
		closure = new Environment("callee(" + tmpl.name + ")", tmpl.closure());
		def = (TemplateDef) parent.def;
		mu = (Interpreter) closure.get("μ").value;
	}

	@Override
	public String toString() {
		return "object " + parent.name;
	}

	private static boolean isClass(Callee callee) {
		return callee.parent.kind.equalsIgnoreCase(Keyword.CLASS.name());
	}
	
	private static Result exec(Callee obj, Interpreter mu, List<Expr> expressions, Environment closure) {
		try {
			Result res = mu.executeBlock(expressions, closure);
			if(obj != null && isClass(obj)) return new Result(obj, new Type.SignatureType((TemplateDef)obj.def));
			return res;
		} catch (ReturnJump ret) {
			return ret.value;
		} catch (MuException ret) {
			return new Result(ret, Type.Exception);
		} catch(InterpreterError e) {
			throw e;
		} catch (Exception e) {
			throw new InterpreterError(e);
		}
	}

	static Result call(Interpreter mu, Callee callee) {
		Result result = null;
		
		// call around
		if (callee.around != null) {
			if(callee.attributes.get(IN_AROUND) == null) {
				callee.attributes.put(IN_AROUND, true);
				result = exec(callee, mu, callee.around.expressions, callee.closure);
				callee.attributes.remove(IN_AROUND);
				return result;
			}
		}

		// call befores
		while (!callee.befores.isEmpty()) {
			Block block = callee.befores.pop();
			exec(null, mu, block.expressions, callee.closure);
		}
		result = exec(callee, mu, callee.parent.body.expressions, callee.closure);
		if (result.type.equals(Type.Exception)) {
			// call errors
			callee.closure.define("$exception", result, false, false);
			while (!callee.errors.isEmpty()) {
				Block block = callee.errors.pop();
				result = exec(callee, mu, block.expressions, callee.closure);
			}
		} else {
			// call afters
			callee.closure.define("$result", result, true, false);
			while (!callee.afters.isEmpty()) {
				Block block = callee.afters.pop();
				result = exec(callee, mu, block.expressions, callee.closure);
			}
		}
		// call alwayses
		while (!callee.alwayses.isEmpty()) {
			Block block = callee.alwayses.pop();
			exec(null, mu, block.expressions, callee.closure);
		}

		return result;
	}

}
