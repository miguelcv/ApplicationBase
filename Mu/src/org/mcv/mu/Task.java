package org.mcv.mu;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mcv.mu.Expr.Block;

public class Task extends Callee {

	ExecutorService executor = Executors.newFixedThreadPool(10);
	
	public Task(Template tmpl) {
		parent = tmpl;
		closure = new Environment("callee(" + tmpl.name + ")", tmpl.closure());
		mu = (Interpreter) closure.get("μ").value;
	}

	@Override Result call(Interpreter mu) {
		try {
			mu.funstk.push(parent.name);
			Interpreter mu2 = new Interpreter(mu.environment, mu.handler);
			Future<Result> future = executor.submit(new Callable<Result>() {
				@Override
				public Result call() throws Exception {
					return doCall(mu2);
				}
			});
			return new Result(future, Type.Future);
		} catch (Exception e) {
			System.err.println("call: " + e);
			return null;
		} finally {
			mu.funstk.pop();
		}
	}

	Result doCall(Interpreter mu) {
		Result result = null;
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
			// call afters
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
	}

	@Override
	public String toString() {
		return "task " + parent.name;
	}

}
