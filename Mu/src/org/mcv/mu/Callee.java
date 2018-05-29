package org.mcv.mu;

import java.util.List;
import java.util.Stack;

import org.mcv.mu.Expr.Block;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Interpreter.InterpreterError;
import org.mcv.mu.Type.SignatureType;

public class Callee {
	Template parent;
	Attributes attributes;
	Environment closure;
	Params params;
	ListMap<Result> interfaces;
	TemplateDef def;
	Interpreter mu;
	Stack<Block> arounds;
	Stack<Block> befores;
	Stack<Block> afters;
	Stack<Block> errors;
	Stack<Block> alwayses;

	public Callee(Template tmpl) {
		parent = tmpl;
		closure = parent.closure;
		interfaces = parent.interfaces;
		def = (TemplateDef) parent.def;
		mu = (Interpreter) closure.get("Mu").value;
	}

	@Override
	public String toString() {
		Result res = call("toString", new ListMap<>());
		if (res == null)
			return "Object" + parent.name;
		return res.value.toString();
	}

	private Result call(String func, ListMap<Object> args) {
		if (interfaces.containsKey(func)) {
			Result f = interfaces.get(func);
			if (f.type instanceof SignatureType) {
				Template tmpl = new Template((TemplateDef) ((Template) f.value).def, closure);
				return Template.call(tmpl, mu, args);
			}
		}
		return null;
	}

	private static Result exec(Interpreter mu, List<Expr> expressions, Environment closure) {
		try {
			return mu.executeBlock(expressions, closure);
		} catch (ReturnJump ret) {
			return ret.value;
		} catch (Exception e) {
			if(e instanceof InterpreterError) {
				throw e;
			} else {
				throw new InterpreterError(e);
			}
		}
	}

	static Result call(Interpreter mu, Callee callee) {
		Result result = null;

		// call arounds
		if (!callee.arounds.isEmpty()) {
			while (!callee.arounds.isEmpty()) {
				Block block = callee.arounds.pop();
				result = exec(mu, block.expressions, callee.closure);
			}
			if (callee.parent.kind.equalsIgnoreCase(Keyword.CLASS.name())) {
				return new Result(callee, new Type.SignatureType((TemplateDef) callee.def));
			} else {
				return result;
			}
		}

		// call befores
		while (!callee.befores.isEmpty()) {
			Block block = callee.befores.pop();
			exec(mu, block.expressions, callee.closure);
		}
		result = exec(mu, callee.parent.body.expressions, callee.closure);
		if (result.type.equals(Type.Exception)) {
			// call errors
			callee.closure.define("exception", result, false, false);
			while (!callee.errors.isEmpty()) {
				Block block = callee.errors.pop();
				result = exec(mu, block.expressions, callee.closure);
			}
		} else {
			// call afters
			callee.closure.define("result", result, true, false);
			while (!callee.afters.isEmpty()) {
				Block block = callee.afters.pop();
				result = exec(mu, block.expressions, callee.closure);
			}
		}
		// call alwayses
		while (!callee.alwayses.isEmpty()) {
			Block block = callee.alwayses.pop();
			exec(mu, block.expressions, callee.closure);
		}

		// class body does implicit "return this"
		if (callee.parent.kind.equalsIgnoreCase(Keyword.CLASS.name())) {
			return new Result(callee, new Type.SignatureType((TemplateDef) callee.def));
		} else {
			return result;
		}
	}

}
