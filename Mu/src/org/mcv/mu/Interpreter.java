package org.mcv.mu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mcv.math.BigInteger;
import org.mcv.mu.Expr.*;
import org.mcv.mu.Parser.ParserError;
import org.mcv.mu.Type.*;

public class Interpreter implements Expr.Visitor<Result> {

	private static final String DOES_NOT_UNDERSTAND = "doesNotUnderstand";
	Environment main;
	Environment environment;
	@SuppressWarnings("unused")
	private String moduleName;

	Expr current;
	
	static class InterpreterError extends ParserError {
		private static final long serialVersionUID = 1L;
		protected Expr expr;

		public InterpreterError(String msg) {
			super(msg);
		}

		public InterpreterError(String msg, Object... args) {
			super(String.format(msg, args));
		}

		public InterpreterError(Exception e) {
			super(e);
		}
	}

	public Interpreter(Environment main) {
		this.environment = new Environment("default", main);
		main.define("Mu", new Result(this, Type.Any), false, true);
	}

	void interpret(List<Expr> expressions) {
		
		for (Expr current : expressions) {
			try {
				evaluate(current);
			} catch(ReturnJump ret) {
				System.out.println("Program exited with: " + stringify(ret.value));
				return;
			} catch(MuException me) {
				me.expr = current;
				me.line = current.line;		
				Mu.error(me);
			} catch (InterpreterError e) {
				e.expr = current;
				e.line = current.line;
				Mu.error(e);
			} catch (Exception e) {
				InterpreterError ie = new InterpreterError(e);
				ie.expr = current;
				ie.line = current.line;
				Mu.error(ie);
			}
		}
	}

	Result evaluate(Expr expr) {
		return expr.accept(this);
	}

	/* invoke binary operator */
	Result invoke(String op, Result left, Result right) {
		
		if(!left.type.equals(right.type)) {
			Result[] converted = widen(left, right);
			left = converted[0];
			right = converted[1];
		}
		Type type = left.type;
		Object leftVal = left.value;
		Object rightVal = right.value;
		Object func = type.interfaces.get(op);

		if (func == null) {
			System.err.println("Func " + op + " not found for type " + type);
			func = type.interfaces.get(DOES_NOT_UNDERSTAND);
			if(func instanceof Method) {
				try {
					Method m = (Method)func;
					return new Result(m.invoke(null, op, left, right), typeFromClass(m.getReturnType()));
				} catch(Exception e) {}
			} else {
				throw new InterpreterError("No native operator: %s doesNotUnderstand %s", left.type, op, right.type);
			}
		}

		if (func instanceof Method) {
			Method method = (Method) func;
			try {
				return new Result(method.invoke(null, leftVal, rightVal), typeFromClass(method.getReturnType()));
			} catch (Exception e) {
				throw new InterpreterError("No such operator: %s %s %s", left.type, op, right.type);
			}
		} else {
			throw new InterpreterError("No native operator: %s %s %s", left.type, op, right.type);
		}
	}

	/* invoke unary operator */
	private Result invoke(Type type, String op, Object arg) {
		if (arg instanceof Result) {
			arg = ((Result) arg).value;
		}
		Object func = type.interfaces.get(op);

		if (func == null) {
			System.err.println("Func " + op + " not found for type " + type);
			func = type.interfaces.get(DOES_NOT_UNDERSTAND);
			if(func instanceof Method) {
				try {
					Method m = (Method)func;
					return new Result(m.invoke(null, op, arg), typeFromClass(m.getReturnType()));
				} catch(Exception e) {}
			} else {
				throw new InterpreterError("No native operator: doesNotUnderstand %s", arg);
			}
		}

		if (func instanceof Method) {
			Method method = (Method) func;
			try {
				return new Result(method.invoke(null, arg), typeFromClass(method.getReturnType()));
			} catch (Exception e) {
				throw new InterpreterError("No such operator: %s %s", op, type);
			}
		} else {
			throw new InterpreterError("No such operator: %s %s", op, type);
		}
	}

	private Result[] widen(Result r1, Result r2) {
		
		Object func1 = r1.type.interfaces.get("to" + r2.type);
		Object func2 = r2.type.interfaces.get("to" + r1.type);
		Result[] res = new Result[2];
		
		if (func1 != null && func1 instanceof Method) {
			res[0] = convert((Method)func1, r1);
			res[1] = r2;
		} else if (func2 != null && func2 instanceof Method) {
			res[0] = r1;
			res[1] = convert((Method)func2, r2);
		} else {
			System.err.println(String.format("No conversion between types %s and %s", r1.type, r2.type));
			res[0] = r1;
			res[1] = r2;
		}
		return res;
	}

	private Result convert(Method func, Result r) {
		try {
			Object val = func.invoke(null, r.value);
			return new Result(val, typeFromClass(val.getClass()));
		} catch(Exception e) {
			return r;
		}
	}

	private Result assign(Assign expr, Object value) {
		return environment.assign(expr.name, new Result(value, typeFromValue(value)));
	}

	private Result assign(Variable expr, Object value) {
		return environment.assign(expr.name, new Result(value, typeFromValue(value)));
	}

	/* VISITORS */

	@Override
	public Result visitPrintExpr(Expr.Print stmt) {
		Expr blk = stmt.expression;
		System.out.println(unquote(stringify(evaluate(blk))));
		return new Result(null, Type.Void);
	}

	@Override
	public Result visitPrintTypeExpr(Expr.PrintType stmt) {
		Expr blk = stmt.expression;
		System.out.println(unquote(stringifyType(evaluate(blk))));
		return new Result(null, Type.Void);
	}

	@Override
	public Result visitVarDef(Expr.Var stmt) {
		Result value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}
		if(value == null) {
			throw new InterpreterError("Missing initializer for var %s", stmt.name.lexeme);
		}
		if (value.value instanceof Type) {
			value.type = (Type) value.value;
		}
		if(stmt.attributes.isOwn()) {
			if(environment.topLevel().get(stmt.name.lexeme) == null) {
				return environment.topLevel().define(stmt.name.lexeme, value, true, stmt.attributes.isPublic());
			}
			return value;
		}
		return environment.define(stmt.name.lexeme, value, true, stmt.attributes.isPublic());
	}

	@Override
	public Result visitValDef(Expr.Val stmt) {
		Result value = null;
		if (stmt.initializer == null) {
			throw new InterpreterError("Val %s must be initialized!", stmt.name.lexeme);
		}
		value = evaluate(stmt.initializer);
		if(stmt.attributes.isOwn()) {
			return environment.topLevel().define(stmt.name.lexeme, value, false, stmt.attributes.isPublic());		
		}
		return environment.define(stmt.name.lexeme, value, false, stmt.attributes.isPublic());
	}

	@Override
	public Result visitVariableExpr(Expr.Variable expr) {
		return environment.get(expr.name);
	}

	@Override
	public Result visitLiteralExpr(Expr.Literal expr) {
		/* string interpolation */
		Type type = typeFromValue(expr.value);
		if (expr.value instanceof String) {
			return new Result(interpolate((String) expr.value), type);
		}
		if (expr.value instanceof RString) {
			return new Result(((RString) expr.value).string, Type.String);
		}
		return new Result(expr.value, type);
	}

	@Override
	public Result visitBinaryExpr(Expr.Binary expr) {
		Result left = evaluate(expr.left);
		expr.type = left.type;

		/* START short circuit */
		switch (soperator(expr.operator.type)) {
		case AND:
			if (left.value == null || left.value instanceof Boolean /* || left.value intanceof Exception */) {
				if (isTruthy(expr.type, expr.left, false)) {
					return evaluate(expr.right);
				} else {
					return left;
				}
			} else {
				return invoke("and", left, evaluate(expr.right));
			}
		case OR:
			if (left.value == null || left.value instanceof Boolean /* || left.value intanceof Exception */) {
				if (isTruthy(expr.type, expr.left, false)) {
					return left;
				} else {
					return evaluate(expr.right);
				}
			} else {
				return invoke("or", left, evaluate(expr.right));
			}
		default:
			break;
		}
		/* END short circuit */

		Result right = evaluate(expr.right);

		switch (keyword(expr.operator.type)) {
		case GCD:
			return invoke("gcd", left, right);
		case MAX:
			return invoke("max", left, right);
		case MIN:
			return invoke("min", left, right);
		case XOR:
			return invoke("xor", left, right);
		default:
			break;

		}

		switch (soperator(expr.operator.type)) {
		case GREATER:
			return invoke("gt", left, right);

		case GREATER_EQUAL:
			return invoke("ge", left, right);

		case LESS:
			return invoke("lt", left, right);

		case LESS_EQUAL:
			return invoke("le", left, right);

		case MINUS:
			return invoke("minus", left, right);

		case SLASH:
			return invoke("div", left, right);

		case PERCENT:
			return invoke("rem", left, right);

		case STAR:
			return invoke("mul", left, right);

		case PLUS:
			return invoke("plus", left, right);

		case NOT_EQUAL:
			return invoke("neq", left, right);
			
		case NEQEQ:
			return invoke("neqeq", left, right);

		case EQUAL:
			return invoke("eq", left, right);
			
		case EQEQ:
			return invoke("eqeq", left, right);

		case POW:
			if(expr.type.equals(Type.Bool)) {
				return invoke("xor", left, right);
			}
			return invoke("pow", left, right);

		case LEFTSHIFT:
			return invoke("lsh", left, right);
		
		case RIGHTSHIFT:
			return invoke("rsh", left, right);

		default:
			return new Result(null, Type.Void);
		}
	}

	public Result executeBlock(List<Expr> body, Environment environment) {
		Environment env = new Environment("block", environment);
		Environment previous = this.environment;
		try {
			this.environment = env;
			Result value = null;
			for (int i = 0; i < body.size(); i++) {
				value = evaluate(body.get(i));
			}
			return value;
		} catch(ReturnJump ret) {
			throw ret;
		} catch(MuException me) {
			return new Result(me, Type.Exception);
		} catch(InterpreterError ie) {
			throw ie;
		} catch(Exception e) {
			throw new InterpreterError(e);
		} finally {
			this.environment = previous;
		}
	}

	@Override
	public Result visitBlockExpr(Expr.Block expr) {
		return executeBlock(expr.expressions, environment);
	}

	@Override
	public Result visitUnaryExpr(Expr.Unary expr) {
		Result right = evaluate(expr.right);
		expr.type = right.type;

		switch (soperator(expr.operator.type)) {

		case MINUS:
			return invoke(expr.type, "neg", right);

		case PLUS:
			return right;

		case NOT:
			return invoke(expr.type, "not", right);

		case PLUSPLUS:
			Result result = invoke(expr.type, "inc", right);
			assign(((Expr.Variable) expr.right), result.value);
			return result;

		case MINMIN:
			result = invoke(expr.type, "dec", right);
			assign(((Expr.Variable) expr.right), result.value);
			return result;

		case BACKSLASH:
			return invoke(expr.type, "abs", right);

		default:
			break;
		}

		switch (keyword(expr.operator.type)) {
		case ABS:
			return invoke(expr.type, "abs", right);
		case SQRT:
			return invoke(expr.type, "sqrt", right);
		default:
			return new Result(null, Type.Void);
		}
	}

	@Override
	public Result visitPostfixExpr(Expr.Postfix expr) {

		Result left = evaluate(expr.left);
		expr.type = left.type;

		switch (soperator(expr.operator.type)) {

		case PLUSPLUS:
			Result result = invoke(expr.type, "inc", left);
			assign(((Expr.Variable) expr.left), result.value);
			return left;

		case MINMIN:
			result = invoke(expr.type, "dec", left);
			assign(((Expr.Variable) expr.left), result.value);
			return left;

		case BANG:
			return invoke(expr.type, "fac", left);

		default:
			return new Result(null, Type.Void);
		}
	}

	@Override
	public Result visitAssignExpr(Assign expr) {

		Result left = environment.get(expr.name.lexeme);
		expr.type = left.type;
		Result right = evaluate(expr.value);
		Result value = right;

		switch (soperator(expr.op.type)) {
		case ASSIGN:
			break;
		case PLUSIS:
			value = invoke("plus", left, right);
			break;
		case MINIS:
			value = invoke("minus", left, right);
			break;
		case STARIS:
			value = invoke("mul", left, right);
			break;
		case SLASHIS:
			value = invoke("div", left, right);
			break;
		case PERCENTIS:
			value = invoke("rem", left, right);
			break;
		case POWIS:
			if(expr.type.equals(Type.Bool)) {				
				value = invoke("xor", left, right);
			} else {
				value = invoke("pow", left, right);
			}
			break;
		case ANDIS:
			value = invoke("and", left, right);
			break;
		case ORIS:
			value = invoke("or", left, right);
			break;
		case LSHIFTIS:
			value = invoke("lsh", left, right);
			break;
		case RSHIFTIS:
			value = invoke("rsh", left, right);
			break;
		default:
			break;
		}
		assign(expr, value.value);
		return value;
	}

	@Override
	public Result visitIfExpr(Expr.If stmt) {
		if (isTruthy(stmt.condition.type, stmt.condition, stmt.invert)) {
			return evaluate(stmt.thenBranch);
		}		
		if (stmt.elseBranch != null) {
			return evaluate(stmt.elseBranch);
		} else {
			return new Result(null, Type.Void);
		}
	}

	@Override
	public Result visitSelectExpr(Expr.Select stmt) {
		Result zwitch = evaluate(stmt.condition);
		for (Pair<Expr, Expr> when : zip(stmt.whenExpressions, stmt.whenBranches)) {
			if (evaluate(when.left).equals(zwitch)) {
				return evaluate(when.right);
			}
		}
		if (stmt.elseBranch != null) {
			return evaluate(stmt.elseBranch);
		}
		return new Result(null, Type.Void);
	}

	public static <A, B> List<Pair<A, B>> zip(List<A> listA, List<B> listB) {
		if (listA.size() != listB.size()) {
			throw new InterpreterError("Lists must have same size");
		}
		List<Pair<A, B>> pairList = new LinkedList<>();
		for (int index = 0; index < listA.size(); index++) {
			pairList.add(new Pair<>(listA.get(index), listB.get(index)));
		}
		return pairList;
	}

	@Override
	public Result visitWhileExpr(Expr.While stmt) {
		List<Object> list = new ArrayList<>();
		Type returnType = Type.None;
		while (isTruthy(stmt.condition.type, stmt.condition, stmt.invert)) {
			try {
				Result result = evaluate(stmt.body);
				if (result.value != null) {
					list.add(result.value);
					returnType = result.type;
				}
			} catch (BreakJump breakJump) {
				return new Result(list, new Type.ListType(returnType));
			} catch (ContinueJump continueJump) {
				// Do nothing.
			} catch(MuException e) {
				return new Result(e, Type.Exception);
			}
		}
		if(stmt.atEnd != null) {
			return evaluate(stmt.atEnd);
		}
		return new Result(list, new Type.ListType(returnType));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Result visitForExpr(For expr) {
		Environment env = new Environment("for", environment);
		Result res = evaluate(expr.range);
		List<Object> values = new ArrayList<>();
		Type type = Type.Any;
		if (res.value instanceof List) {
			values = (List<Object>) res.value;
			type = ((ListType) res.type).eltType;
		} else if (res.value instanceof Set) {
			values = new ArrayList<>((Set<Object>) res.value);
			type = ((SetType) res.type).eltType;
		} else if (res.value instanceof Map) {
			values = new ArrayList<>(((Map<String, Object>) res.value).entrySet());
			type = new Type.MapType(Type.String, Type.Any);
		} else if (res.value instanceof Callee) {
			Callee pat = (Callee) res.value;
			Map<String, Object> map = new HashMap<>();
			for (Entry<String, Result> param : pat.interfaces.entrySet()) {
				if(param.getValue().value instanceof Property) {
					Property prop = (Property)param.getValue().value;
					prop = prop.mkGetter();
					map.put(param.getKey(), call(param.getValue(), new Result(new ListMap<>(), prop.returnType)).value);
				} else {
					map.put(param.getKey(), param.getValue().value);
				}
			}
			values = new ArrayList<>(map.entrySet());
			type = new Type.MapType(Type.String, Type.Any);
		} else if (res.value instanceof Template) {
			Template pat = (Template) res.value;
			Map<String, Object> map = new HashMap<>();
			for (Entry<String, Result> param : pat.interfaces.entrySet()) {
				map.put(param.getKey(), param.getValue().value);
			}
			values = new ArrayList<>(map.entrySet());
			type = new Type.MapType(Type.String, Type.Any);
		}
		env.define(expr.var.name.lexeme, new Result(values.get(0), typeFromValue(values.get(0))), true, false);

		List<Object> list = new ArrayList<>();
		Type returnType = Type.None;
		for (int i = 0; i < values.size(); i++) {
			try {
				env.assign(expr.var.name, new Result(values.get(i), type));
				Result value = executeBlock(expr.body.expressions, env);
				if (value.value != null) {
					list.add(value.value);
					returnType = value.type;
				}
			} catch (BreakJump breakJump) {
				return new Result(list, new Type.ListType(returnType));
			} catch (ContinueJump continueJump) {
				// Do nothing.
			} catch(MuException me) {
				return new Result(me, Type.Exception);
			}
		}
		if(expr.atEnd != null) {
			// if no break encountered, do this:
			return evaluate(expr.atEnd);
		}
		return new Result(list, new Type.ListType(returnType));
	}

	@Override
	public Result visitReturnExpr(Expr.Return stmt) {
		Result value = null;
		if (stmt.value != null)
			value = evaluate(stmt.value);
		throw new ReturnJump(value);
	}

	@Override
	public Result visitAopExpr(Expr.Aop stmt) {
		Result callable = evaluate(stmt.callable);
		if(callable.value instanceof Template) {
			Template tmpl = (Template)callable.value;
			switch(stmt.name.lexeme.toLowerCase()) {
			case "around":
				tmpl.arounds.push(stmt.block);
				break;
			case "before":
				tmpl.befores.push(stmt.block);
				break;
			case "after":
				tmpl.afters.push(stmt.block);
				break;
			case "error":
				tmpl.errors.push(stmt.block);
				break;
			case "always":
				tmpl.alwayses.push(stmt.block);
				break;
			}
		}
		return new Result(null, Type.Void);
	}

	@Override
	public Result visitThrowExpr(Expr.Throw stmt) {
		Result exception = evaluate(stmt.thrown);
		throw new MuException(exception);
	}

	@Override
	public Result visitModuleDef(Expr.Module stmt) {
		// TODO
		moduleName = stmt.name.lexeme;
		return new Result(null, Type.Void);
	}

	@Override
	public Result visitSetExpr(Expr.Set set) {
		Set<Object> result = new HashSet<>();
		Type eltType = Type.Any;
		for (Expr expr : set.exprs) {
			Result r = evaluate(expr);
			if (eltType.equals(Type.Any)) {
				eltType = r.type;
				result.add(r.value);
			} else {
				if (r.type.equals(eltType)) {
					result.add(r.value);
				} else {
					throw new InterpreterError("Type error: set eltType = %s, value type %s", eltType, r.type);
				}
			}
		}
		return new Result(result, new Type.SetType(eltType));
	}

	@Override
	public Result visitRangeExpr(Expr.Range rng) {
		List<Object> result = new ArrayList<>();
		Result start = evaluate(rng.start);
		Type eltType = Type.Any;
		// Int
		if (start.value instanceof BigInteger) {
			eltType = Type.Int;
			BigInteger st = (BigInteger) start.value;
			if (!rng.startIncl)
				st = st.add(BigInteger.ONE);
			BigInteger end = ((BigInteger) evaluate(rng.end).value);
			if (rng.endIncl)
				end = end.add(BigInteger.ONE);

			if (st.isNaN() || end.isNaN()) {
				return new Result(null, Type.Void);
			}
			if (st.isNegativeInfinity() || st.isPositiveInfinity()) {
				return new Result(null, Type.Void);
			}
			if (end.isNegativeInfinity() || end.isPositiveInfinity()) {
				// do not evaluate!!
				return new Result(rng, new Type.Range("Range", rng.start, rng.end));
			}
			if (end.intValue() < st.intValue()) {
				for (int i = end.intValue(); i >= st.intValue(); i--) {
					result.add(BigInteger.valueOf(i));
				}
			} else {
				for (int i = st.intValue(); i < end.intValue(); i++) {
					result.add(BigInteger.valueOf(i));
				}
			}
		}
		// Char
		if (start.value instanceof Integer) {
			eltType = Type.Char;
			int st = (Integer) start.value;
			if (!rng.startIncl)
				++st;
			int end = ((Integer) evaluate(rng.end).value);
			if (rng.endIncl)
				++end;
			if(end < st) {
				for (int i = end; i >= st; i--) {
					result.add(Integer.valueOf(i));
				}				
			} else {
				for (int i = st; i < end; i++) {
					result.add(Integer.valueOf(i));
				}
			}
		}
		return new Result(result, new Type.ListType(eltType));
	}

	@Override
	public Result visitMappingExpr(Mapping expr) {
		return evaluate(expr.value);
	}

	@Override
	public Result visitSeqExpr(Expr.Seq lst) {
		List<Object> result = new ArrayList<>();
		Type eltType = Type.Any;
		for (Expr expr : lst.exprs) {
			Result r = evaluate(expr);
			if (eltType.equals(Type.Any)) {
				eltType = r.type;
				result.add(r.value);
			} else {
				if (r.type.equals(eltType)) {
					result.add(r.value);
				} else {
					throw new InterpreterError("Type error: set eltType = %s, value type %s", eltType, r.type);
				}
			}
		}
		return new Result(result, new Type.ListType(eltType));
	}

	@Override
	public Result visitMapExpr(Expr.Map map) {
		return doMap(map);
	}

	Result doMap(Expr.Map map) {
		ListMap<Object> listmap = new ListMap<>();

		for (Expr expr : map.mappings) {
			Result value = evaluate(expr);
			if (expr instanceof Expr.Mapping) {
				Expr.Mapping entry = (Expr.Mapping) expr;
				// mutable true, public false ??
				// environment.define(entry.key, value, true, false);
				listmap.put(entry.key, value.value);
			} else {
				listmap.put(null, value.value);
			}
		}
		return new Result(listmap, new Type.MapType(Type.String, Type.Any));
	}

	public Result visitCallExpr(Call expr) {
		Result res = evaluate(expr.current);
		if (res.type instanceof SignatureType)
			return call(res, evaluate(expr.next));
		else
			return evaluate(expr.next);
	}

	public Result visitDotExpr(Dot expr) {
		Result res = evaluate(expr.current);
		if (res.value instanceof Template || res.value instanceof Map || res.value instanceof Map.Entry || res.value instanceof Callee) {
			if (expr.value != null) {
				return setdot(res, ((Expr.Variable) expr.next).name.lexeme, evaluate(expr.value).value);
			}
			return dot(res, ((Expr.Variable) expr.next).name.lexeme);
		} else {
			return evaluate(expr.next);
		}
	}

	public Result visitSubscriptExpr(Subscript expr) {
		Result res = evaluate(expr.seq);
		if (expr.sub instanceof Expr.Range) {
			return sublist(res, (Expr.Range) expr.sub);
		}
		if (res.value instanceof Set) {
			throw new InterpreterError("Cannot subscript a set");
		}
		if (res.value instanceof List || res.value instanceof Map)
			return subscript(res, (Seq)expr.sub, expr.value);
		else
			return evaluate(expr.sub);
	}

	private Result sublist(Result seq, Expr.Range range) {
		int start = ((BigInteger)evaluate(range.start).value).intValue();
		int end = ((BigInteger) evaluate(range.end).value).intValue();
		if (seq.value instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) seq.value;
			List<Object> subList = list.subList(start, end);
			return new Result(subList, typeFromValue(seq.value));
		}
		return new Result(null, Type.Void);
	}

	@SuppressWarnings("unchecked")
	private Result call(Result func, Result actuals) {
		if(func.value instanceof Property) {
			return Property.call((Property)func.value, this, (ListMap<Object>) actuals.value);			
		}
		return Template.call((Template)func.value, this, (ListMap<Object>) actuals.value);
	}

	@SuppressWarnings("unchecked")
	private Result subscript(Result listMap, Seq index, Expr value) {
		Result sub = evaluate(index.exprs.get(0));
		if(value != null) {
			Object val = evaluate(value).value;
			/* setter */
			if (listMap.value instanceof Map) {
				String key = (String)sub.value;
				Map<String, Object> map = (Map<String, Object>)listMap.value;
				if(map.containsKey(key)) {
					map.put(key, val);
				} else {
					throw new InterpreterError("No such key: %s", key);
				}
			}
			if (listMap.value instanceof List) {
				List<Object> list = ((List<Object>) listMap.value);
				int ix = ((BigInteger) sub.value).intValue();
				if(ix < 0) ix = list.size() + ix;
				list.set(ix, val);
			}
			return new Result(null, Type.Void);
		} else {
			/* getter */
			if (listMap.value instanceof Map) {
				String key = (String)sub.value;
				Map<String, Object> map = (Map<String, Object>)listMap.value;
				if(map.containsKey(key)) {
					Object val = map.get(key);
					return new Result(val, typeFromValue(val));
				} else {
					throw new InterpreterError("No such key: %s", key);
				}
			}
			if (listMap.value instanceof List) {
				List<Object> list = (List<Object>)listMap.value;
				int ix = ((BigInteger) sub.value).intValue();
				if(ix < 0) ix = list.size() + ix;
				Object val = list.get(ix);
				return new Result(val, typeFromValue(val));
			}
			return new Result(null, Type.Void);
		}
	}

	@SuppressWarnings("unchecked")
	private Result dot(Result listMap, String key) {
		if (listMap.value instanceof Map) {
			Object value = ((Map<String, Object>) listMap.value).get(key);
			return new Result(value, typeFromValue(value));
		}
		if (listMap.value instanceof Map.Entry) {
			Object value = ((Map.Entry<?, ?>) listMap.value).getValue();
			return new Result(value, typeFromValue(value));
		}
		if (listMap.value instanceof Template) {
			// call getter
			Template tmpl = (Template)listMap.value;
			Result res = tmpl.interfaces.get(key);
			if(res == null) {
				throw new InterpreterError("Undefined variable %s.", key);
			}
			if(res.value instanceof Property) {
				res.value = ((Property)res.value).mkGetter();
				return call(res, new Result(new ListMap<>(), Type.Any));
			}
			if(res.value instanceof Template) {
				return call(res, new Result(new ListMap<>(), Type.Any));
			}
		}
		if (listMap.value instanceof Callee) {
			Callee callee = (Callee)listMap.value;
			Result res = callee.interfaces.get(key);
			if(res.value instanceof Property) {
				res.value = ((Property)res.value).mkGetter();
				return call(res, new Result(new ListMap<>(), Type.Any));
			}
			if(res.value instanceof Template) {
				return call(res, new Result(new ListMap<>(), Type.Any));
			}
		}
		return new Result(null, Type.Void);
	}

	@SuppressWarnings("unchecked")
	private Result setdot(Result listMap, String key, Object value) {
		if (listMap.value instanceof Map) {
			((Map<String, Object>) listMap.value).put(key, value);
			return new Result(value, typeFromValue(value));
		}
		if (listMap.value instanceof Map.Entry) {
			((Map.Entry<String, Object>) listMap.value).setValue(value);
			return new Result(value, typeFromValue(value));
		}
		if (listMap.value instanceof Template) {
			// call setter
			Template tmpl = (Template)listMap.value;
			Result res = tmpl.interfaces.get(key);
			if(res.value instanceof Property) {
				ListMap<Object> params = new ListMap<>();
				params.put("value", value);
				res.value = ((Property)res.value).mkSetter(value);
				return call(res, new Result(params, Type.Any));
			}
		}
		if (listMap.value instanceof Callee) {
			// call setter
			Callee callee = (Callee)listMap.value;
			Result res = callee.interfaces.get(key);
			if(res.value instanceof Property) {
				ListMap<Object> params = new ListMap<>();
				params.put("value", value);
				res.value = ((Property)res.value).mkSetter(value);
				return call(res, new Result(params, Type.Any));
			}
		}
		return new Result(null, Type.Void);
	}

	@Override
	public Result visitTemplateDef(Expr.TemplateDef stmt) {
		Template tmpl = new Template(stmt, environment.capture(stmt.name.lexeme));
		Result res = environment.define(stmt.name.lexeme, new Result(tmpl, new Type.SignatureType(stmt)), false, true);
		tmpl.closure.define(stmt.name.lexeme, new Result(tmpl, new Type.SignatureType(stmt)), false, true);
		
		// define all declarations inside templatedef interface
		for(Expr expr : tmpl.body.expressions) {
			
			/* non-local nested functions and classes */
			if(expr instanceof Expr.TemplateDef) {
				TemplateDef def = (TemplateDef)expr;
				if(def.attributes.isPublic()) {
					Template p = new Template(def, tmpl.closure);
					tmpl.interfaces.put(def.name.lexeme, new Result(p, new Type.SignatureType(def)));
					tmpl.closure.define(def.name.lexeme, new Result(p, new Type.SignatureType(def)), false, true);
				}
			}
			
			/* non-local typedefs */
			if(expr instanceof Expr.TypeDef) {
				TypeDef def = (TypeDef)expr;
				if(def.attributes.isPublic()) {
					tmpl.interfaces.put(def.name.lexeme, new Result(def.literal, Type.Type));
					tmpl.closure.define(def.name.lexeme, new Result(def.literal, Type.Type), false, true);
					// define SetEnum or ListEnum literals
					if(def.type instanceof Type.ListEnum) {
						for(Object val : ((Type.ListEnum)def.type).values) {
							tmpl.interfaces.put(val.toString(), new Result(val.toString(), def.type));
							tmpl.closure.define(val.toString(), new Result(val.toString(), def.type), false, true);
						}
					}
					if(def.type instanceof Type.SetEnum) {
						for(Object val : ((Type.SetEnum)def.type).values) {
							tmpl.interfaces.put(val.toString(), new Result(val.toString(), def.type));
							tmpl.closure.define(val.toString(), new Result(val.toString(), def.type), false, true);
						}						
					}
				}
			}
			
			/* own [class] immutable properties */
			if(expr instanceof Expr.Val) {
				Val def = (Val)expr;
				if(def.attributes.isOwn()) {
					if(tmpl.closure.get(def.name.lexeme) != null) {
						throw new InterpreterError("Name %s already defined.", def.name.lexeme);
					}
					executeBlock(List.of(expr), tmpl.closure);
					if(def.attributes.isProp()) {
						// getter
						tmpl.interfaces.put(def.name.lexeme, new Result(new Property(def.name, def.type, tmpl.closure), def.type));
					}
				}
			}

			/* own [class] mutable properties */
			if(expr instanceof Expr.Var) {
				Var def = (Var)expr;
				// getter & setter
				if(def.attributes.isOwn()) {
					if(tmpl.closure.get(def.name.lexeme) != null) {
						throw new InterpreterError("Name %s already defined.", def.name.lexeme);
					}
					executeBlock(List.of(expr), tmpl.closure);
					if(def.attributes.isProp()) {
						Property prop = new Property(def.name, def.type, tmpl.closure);
						tmpl.interfaces.put(def.name.lexeme, new Result(prop, def.type));						
					}
				}
			}
		}		
		return res;
	}

	@Override
	public Result visitBreakExpr(Expr.Break stmt) {
		throw new BreakJump();
	}

	@Override
	public Result visitContinueExpr(Expr.Continue stmt) {
		throw new ContinueJump();
	}

	@Override
	public Result visitTypeLiteralExpr(Expr.TypeLiteral expr) {
		return new Result(expr.literal, Type.Type);
	}

	@Override
	public Result visitTypeDefExpr(Expr.TypeDef expr) {
		if (expr.name != null) {
			environment.define(expr.name.lexeme, new Result(expr.literal, Type.Type), false, expr.attributes.isPublic());
			
			// CHECKME
			if(expr.type instanceof Type.ListEnum) {
				for(Object val : ((Type.ListEnum)expr.type).values) {
					environment.define(val.toString(), new Result(val.toString(), expr.type), false, expr.attributes.isPublic());
				}
			}
			if(expr.type instanceof Type.SetEnum) {
				for(Object val : ((Type.SetEnum)expr.type).values) {
					environment.define(val.toString(), new Result(val.toString(), expr.type), false, expr.attributes.isPublic());
				}						
			}
		}
		return new Result(null, Type.Void);
	}

	/* UTIL */

	static Type typeFromClass(Class<?> clz) {
		/* We have char, string, int, real, bool */
		if (clz.equals(Void.class))
			return Type.Void;
		if (clz.equals(Integer.class) || clz.equals(int.class))
			return Type.Char;
		if (clz.equals(String.class))
			return Type.String;
		if (clz.equals(RString.class))
			return Type.String;
		if (clz.equals(BigInteger.class))
			return Type.Int;
		if (clz.equals(Double.class) || clz.equals(double.class))
			return Type.Real;
		if (clz.equals(Boolean.class) || clz.equals(boolean.class))
			return Type.Bool;

		/*
		 * if (clz.equals(Expr.ClassDef.class)) return new Type.SignatureType((ClassDef)
		 * val); if (clz.equals(Expr.FuncDef.class)) return new
		 * Type.SignatureType((FuncDef) val);
		 */

		if (clz.isAssignableFrom(java.util.List.class)) {
			return new Type.ListType(Type.Any);
		}
		if (clz.isAssignableFrom(java.util.Set.class)) {
			return new Type.SetType(Type.Any);
		}
		if (clz.isAssignableFrom(java.util.Map.class)) {
			return new Type.MapType(Type.String, Type.Any);
		}
		if (clz.isAssignableFrom(Map.Entry.class)) {
			return new Type.MapType(Type.String, Type.Any);
		}
		System.err.println("Type is " + clz);
		return Type.None;
	}

	public static Type typeFromValue(Object val) {

		/* We have char, string, int, real, bool */
		if (val == null)
			return Type.Void;
		if (val instanceof Integer)
			return Type.Char;
		if (val instanceof String)
			return Type.String;
		if (val instanceof RString)
			return Type.String;
		if (val instanceof BigInteger)
			return Type.Int;
		if (val instanceof Double)
			return Type.Real;
		if (val instanceof Boolean)
			return Type.Bool;

		if (val instanceof Expr.TemplateDef)
			return new Type.SignatureType((TemplateDef) val);

		if (val instanceof java.util.List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) val;
			if (!list.isEmpty()) {
				return new Type.ListType(typeFromValue(list.get(0)));
			}
		}
		if (val instanceof java.util.Set) {
			@SuppressWarnings("unchecked")
			Set<Object> set = (Set<Object>) val;
			if (!set.isEmpty()) {
				return new Type.SetType(typeFromValue(set.iterator().next()));
			}
		}
		if (val instanceof java.util.Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> set = (Map<String, Object>) val;
			if (!set.isEmpty()) {
				return new Type.MapType(Type.String, Type.Any);
			}
		}
		if (val instanceof Map.Entry) {
			return new Type.MapType(Type.String, Type.Any);
		}
		if (val instanceof Template) {
			return new Type.SignatureType((TemplateDef) ((Template) val).def);
		}
		if (val instanceof Callee) {
			return new Type.SignatureType((TemplateDef) ((Callee) val).def);
		}
		System.err.println("Type is " + val.getClass());
		return Type.None;
	}

	@SuppressWarnings("unchecked")
	private String stringify(Result obj) {
		if (obj == null || obj.value == null)
			return "nil";
		if (obj.value instanceof Integer) {
			return "'" + new String(Character.toChars((Integer) obj.value)) + "'";
		}
		if (obj.value instanceof String) {
			return "\"" + obj.value + "\"";
		}
		if (obj.value instanceof Collection) {
			return stringifyCollection((Collection<Object>) obj.value);
		}
		if (obj.value instanceof Map) {
			return stringifyMap((Map<String, Object>) obj.value);
		}
		return obj.value.toString();
	}

	private String stringifyType(Result obj) {
		if (obj == null || obj.value == null)
			return "Void";
		return obj.type.toString();
	}

	private String unquote(String s) {
		int ix = 0;
		if (s.startsWith("\""))
			ix = 1;
		int ixEnd = s.length();
		if (s.endsWith("\""))
			ixEnd -= 1;
		return s.substring(ix, ixEnd);
	}

	private String stringifyCollection(Collection<Object> collection) {
		StringBuilder sb = new StringBuilder();
		char bracket = (collection instanceof Set) ? '{' : '[';
		sb.append(bracket);
		for (Object o : collection) {
			if (sb.length() > 1) {
				sb.append(", ");
			}
			sb.append(stringify(new Result(o, Type.Any)));
		}
		sb.append(bracket == '{' ? '}' : ']');
		return sb.toString();
	}

	private String stringifyMap(Map<String, Object> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Entry<String, Object> entry : map.entrySet()) {
			if (sb.length() > 1) {
				sb.append(", ");
			}
			sb.append(entry.getKey()).append(" → ").append(stringify(new Result(entry.getValue(), Type.Any)));
		}
		sb.append(")");
		return sb.toString();
	}

	private String interpolate(String string) {
		StringBuilder sb = new StringBuilder();
		int ix1 = string.indexOf("#{");
		int ix2 = string.indexOf('}', ix1);
		if (ix1 >= 0 && ix2 > ix1) {
			String name = string.substring(ix1 + 2, ix2);
			String pre = string.substring(0, ix1);
			String post = string.substring(ix2 + 1);
			Expr expr = Mu.parse(name);
			if(expr == null) {
				return sb.append(string.substring(0, ix2 + 1)).append(interpolate(post)).toString();
			} else {
				Result subst = evaluate(expr);
				sb.append(pre).append(subst.value);
				return sb.append(interpolate(post)).toString();
			}
		} else {
			return string;
		}
	}

	boolean isTruthy(Type type, Expr expr, boolean invert) {
		return invert ? !isTruthy(type, expr) : isTruthy(type, expr);
	}
	
	@SuppressWarnings("unchecked")
	boolean isTruthy(Type type, Expr expr) { 
		Result condition = evaluate(expr);
		if(condition.value == null) {
			return false;
		}
		if(condition.value instanceof MuException) {
			return false;
		}
		if(condition.value instanceof Boolean) {
			return (Boolean)condition.value;
		}
		if(condition.value instanceof String) {
			String s = (String) condition.value;
			return !s.isEmpty();
		}
		if(condition.value instanceof List) {
			List<Object> list = (List<Object>) condition.value;
			return !list.isEmpty();
		}
		if(condition.value instanceof Set) {
			Set<Object> set = (Set<Object>) condition.value;
			return !set.isEmpty();
		}
		if(condition.value instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) condition.value;
			return !map.isEmpty();
		}
		return true;
	}

	Soperator soperator(TokenType type) {
		if (type instanceof Soperator)
			return (Soperator) type;
		return Soperator.NONE;
	}

	Keyword keyword(TokenType type) {
		if (type instanceof Keyword)
			return (Keyword) type;
		return Keyword.NONE;
	}

}
