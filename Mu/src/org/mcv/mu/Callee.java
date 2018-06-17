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
	private boolean inaround;
	
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
	
	private static Result exec(Callee obj, Interpreter mu, List<Expr> expressions, Environment closure) {
		try {
			Result res = mu.executeBlock(expressions, closure);
			if(isClass(obj)) return new Result(obj, new Type.SignatureType((TemplateDef)obj.parent.def));
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

	private static Result invokeJava(Interpreter mu, Callee obj) {
		try {
			if(obj.parent.kind.equals("fun")) {
				Object ret = JavaHelper.lookupAndInvokeMethod(obj);
				return JavaHelper.mkResult(mu, ret);
			} else {
				// class
				if(obj.javaClass == null) {
					obj.javaClass = JavaHelper.getClass(mu.classLoader, obj.parent.name);
				}
				obj.javaObject = JavaHelper.lookupAndInvokeConstructor(obj);
				return new Result(obj, new Type.SignatureType((TemplateDef)obj.parent.def));
			}
		} catch(Exception e) {
			log.error(e.toString(),e);
			throw new InterpreterError(e);
		}
	}

	static Result call(Interpreter mu, Callee callee) {
		Result result = null;
		
		// call around
		if (callee.around != null) {
			if(!callee.inaround) {
				callee.inaround = true;
				result = exec(callee, mu, callee.around.expressions, callee.closure);
				callee.inaround = false;
				return result;
			}
		}

		// call befores
		while (!callee.befores.isEmpty()) {
			Block block = callee.befores.pop();
			exec(callee, mu, block.expressions, callee.closure);
		}
		
		/* call the actual function/constructor */
		if(callee.parent.attributes.containsKey("jvm")) {
			/* native class or method! */
			result = invokeJava(mu, callee);
		} else {
			result = exec(callee, mu, callee.parent.body.expressions, callee.closure);
		}
		
		if (result.type.equals(Type.Exception)) {
			// call errors
			callee.closure.define("$exception", result, false, false);
			while (!callee.errors.isEmpty()) {
				Block block = callee.errors.pop();
				result = exec(callee, mu, block.expressions, callee.closure);
			}
		} else {
			// call afteFrs
			callee.closure.define("$result", result, true, false);
			while (!callee.afters.isEmpty()) {
				Block block = callee.afters.pop();
				result = exec(callee, mu, block.expressions, callee.closure);
			}
		}
		// call alwayses
		while (!callee.alwayses.isEmpty()) {
			Block block = callee.alwayses.pop();
			exec(callee, mu, block.expressions, callee.closure);
		}

		return result;
	}

}
