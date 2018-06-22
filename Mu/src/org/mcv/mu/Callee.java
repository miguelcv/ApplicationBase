package org.mcv.mu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.mcv.mu.Expr.Block;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Interpreter.InterpreterError;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Callee {
	/* prevent infinite loop in AROUND */
	protected boolean inaround;

	Template parent;
	protected Environment closure;

	public Environment closure() {
		return closure;
	}

	Params params;
	List<Mixin> mixins = new ArrayList<>();
	Interpreter mu;
	Block around;
	Stack<Block> befores;
	Stack<Block> afters;
	Stack<Block> errors;
	Stack<Block> alwayses;

	/* JVM */
	Class<?> javaClass;
	Object javaObject;
	List<Method> javaMethodList = new ArrayList<>();

	public Callee() {
	}

	public Callee(Template tmpl) {
		parent = tmpl;
		closure = new Environment("callee(" + tmpl.name + ")", tmpl.closure());
		mu = (Interpreter) closure.get("μ").value;
	}

	@Override
	public String toString() {
		return "object " + parent.name;
	}

	private static boolean isClass(Callee callee) {
		return callee.parent.kind.equalsIgnoreCase(Keyword.CLASS.name());
	}

	protected Result exec(Interpreter mu, List<Expr> expressions, Environment closure) {
		try {
			Result res = mu.executeBlock(expressions, closure);
			if (isClass(this))
				return new Result(this, new Type.SignatureType((TemplateDef) this.parent.def));
			return res;
		} catch (ReturnJump ret) {
			return ret.value;
		} catch (MuException ret) {
			return new Result(ret, Type.Exception);
		} catch (InterpreterError e) {
			throw e;
		} catch (Exception e) {
			throw new InterpreterError(e);
		}
	}

	protected Result invokeJava(Interpreter mu) {
		try {
			if (parent.kind.equals("fun")) {
				Object ret = JavaHelper.lookupAndInvokeMethod(this);
				return JavaHelper.mkResult(mu, ret);
			} else {
				// class
				if (javaClass == null) {
					javaClass = JavaHelper.getClass(mu.classLoader, parent.name);
				}
				javaObject = JavaHelper.lookupAndInvokeConstructor(this);
				return new Result(this, new Type.SignatureType((TemplateDef) parent.def));
			}
		} catch (Exception e) {
			log.error(e.toString(), e);
			throw new InterpreterError(e);
		}
	}

	Result call(Interpreter mu) {
		Result result = null;
		try {
			mu.funstk.push(parent.name);
			// call around
			if (around != null) {
				if (!inaround) {
					inaround = true;
					result = exec(mu, around.expressions, closure);
					inaround = false;
					return result;
				}
			}

			// call befores
			while (!befores.isEmpty()) {
				Block block = befores.pop();
				exec(mu, block.expressions, closure);
			}

			/* call the actual function/constructor */
			if (parent.attributes.containsKey("jvm")) {
				/* native class or method! */
				result = invokeJava(mu);
			} else {
				result = exec(mu, parent.body.expressions, closure);
			}

			if (result.type.equals(Type.Exception)) {
				// call errors
				closure.define("$exception", result, false, false);
				while (!errors.isEmpty()) {
					Block block = errors.pop();
					result = exec(mu, block.expressions, closure);
				}
			} else {
				// call afteFrs
				closure.define("$result", result, true, false);
				while (!afters.isEmpty()) {
					Block block = afters.pop();
					result = exec(mu, block.expressions, closure);
				}
			}
			// call alwayses
			while (!alwayses.isEmpty()) {
				Block block = alwayses.pop();
				exec(mu, block.expressions, closure);
			}

			return result;
		} catch (Exception e) {
			log.debug(e.toString(), e);
			return null;
		} finally {
			mu.funstk.pop();
		}
	}

}
