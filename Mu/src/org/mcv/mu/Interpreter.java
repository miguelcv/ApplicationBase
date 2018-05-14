package org.mcv.mu;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mcv.mu.Expr.*;
import org.mcv.mu.stdlib.*;

public class Interpreter implements Expr.Visitor<Object> {

	final Environment main = new Environment();
	// FOR NOW
	static final Map<String, TypeInfo> typePool = new HashMap<>();
	private Environment environment = main;
	@SuppressWarnings("unused")
	private String moduleName;

	Interpreter() {
		main.define("clock", new MuCallable() {
			@Override
			public int arity() {
				return 0;
			}

			@Override
			public Object call(Interpreter interpreter, List<Object> arguments) {
				return (double) System.currentTimeMillis() / 1000.0;
			}
		}, Types.Func.name());

		typePool.put(Types.None.name(), new TypeInfo(Types.None, INone.class));
		typePool.put(Types.Void.name(), new TypeInfo(Types.Void, IVoid.class));
		typePool.put(Types.Bool.name(), new TypeInfo(Types.Bool, IBool.class));
		TypeInfo num = new TypeInfo(Types.Num, INum.class);
		typePool.put("Int&Real", num);
		typePool.put("Real&Int", num);
		typePool.put(Types.Int.name(), new TypeInfo(Types.Int, IInt.class));
		typePool.put(Types.Real.name(), new TypeInfo(Types.Real, IReal.class));
		TypeInfo text = new TypeInfo(Types.Text, IText.class);
		typePool.put("Char&String", text);
		typePool.put("String&Char", text);
		typePool.put(Types.Char.name(), new TypeInfo(Types.Char, IChar.class));
		typePool.put(Types.String.name(), new TypeInfo(Types.String, IString.class));
		typePool.put(Types.Any.name(), new TypeInfo(Types.Any, IAny.class));
		typePool.put(Types.Type.name(), new TypeInfo(Types.Type, IType.class));
		typePool.put(Types.Func.name(), new TypeInfo(Types.Func, IFunc.class));
		typePool.put(Types.List.name(), new TypeInfo(Types.List, IList.class));
		typePool.put(Types.Set.name(), new TypeInfo(Types.Set, ISet.class));
		typePool.put(Types.Map.name(), new TypeInfo(Types.Map, IMap.class));
		typePool.put(Types.Range.name(), new TypeInfo(Types.Range, IRange.class));
	}

	void interpret(List<Expr> statements) {
		try {
			for (Expr statement : statements) {
				evaluate(statement);
			}
		} catch (Exception error) {
			Mu.runtimeError(error, "Error in interpreter");
		}
	}

	public Object executeBlock(List<Expr> body, Environment environment) {
		Environment env = new Environment(environment);
		Environment previous = this.environment;
		try {

			this.environment = env;
			Object value = null;
			for (int i = 0; i < body.size(); i++) {
				value = evaluate(body.get(i));
			}
			return value;

		} finally {
			this.environment = previous;
		}
	}

	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	/* invoke binary operator */
	Object invoke(String type, String op, Object left, Object right) {
		TypeInfo info = typePool.get(type);
		Class<?> clazz = info.trait;
		Method[] methods = clazz.getMethods();
		Method method = null;
		for (Method m : methods) {
			if (m.getName().equals(op)) {
				method = m;
				break;
			}
		}
		if (method == null) {
			return Mu.runtimeError("No such operator: %s %s %s", left, method, right);
		}
		try {
			return method.invoke(null, left, right);
		} catch (IllegalArgumentException e) {
			// try convert();
			Method convert = null;
			for (Method m : methods) {
				if (m.getName().equals("convert")) {
					convert = m;
					break;
				}
			}
			if (convert == null) {
				return Mu.runtimeError("No such operator: %s %s %s", left, method, right);
			}
			try {
				return method.invoke(null, convert.invoke(null, left), convert.invoke(null, right));
			} catch (Exception e2) {
				return Mu.runtimeError("No such operator: %s %s %s", left, method, right);
			}
		} catch (Exception e) {
			return Mu.runtimeError("No such operator: %s %s %s", left, method, right);
		}
	}

	/* invoke unary operator */
	private Object invoke(String type, String op, Object arg) {
		TypeInfo info = typePool.get(type);
		Class<?> clazz = info.trait;
		Method[] methods = clazz.getMethods();
		Method method = null;
		for (Method m : methods) {
			if (m.getName().equals(op)) {
				method = m;
				break;
			}
		}
		if (method == null) {
			return Mu.runtimeError("No such operator: %s %s", method, arg);
		}
		try {
			return method.invoke(null, arg);
		} catch (IllegalArgumentException e) {
			// try convert();
			Method convert = null;
			for (Method m : methods) {
				if (m.getName().equals("convert")) {
					convert = m;
					break;
				}
			}
			if (convert == null) {
				return Mu.runtimeError("No such operator: %s %s", method, arg);
			}
			try {
				return method.invoke(null, convert.invoke(null, arg));
			} catch (Exception e2) {
				return Mu.runtimeError("No such operator: %s %s", method, arg);
			}
		} catch (Exception e) {
			return Mu.runtimeError("No such operator: %s %s", method, arg);
		}
	}

	private Object assign(Assign expr, Object value) {
		return environment.assign(expr.name, value, typeFromValue(value));
	}

	private Object assign(Variable expr, Object value) {
		return environment.assign(expr.name, value, typeFromValue(value));
	}

	/* VISITORS */

	@Override
	public Object visitPrintExpr(Expr.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(unquote(stringify(value)));
		return null;
	}

	@Override
	public Object visitVarExpr(Expr.Var stmt) {
		Object value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}
		environment.define(stmt.name.lexeme, value, typeFromValue(value));
		return null;
	}

	@Override
	public Object visitValExpr(Expr.Val stmt) {
		Object value = null;
		if (stmt.initializer == null) {
			return Mu.runtimeError("Val %s must be initialized!", stmt.name.lexeme);
		}
		value = evaluate(stmt.initializer);
		environment.define(stmt.name.lexeme, value, typeFromValue(value));
		return null;
	}

	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return environment.get(expr.name);
	}

	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		/* string interpolation */
		if (expr.value instanceof String) {
			return interpolate((String) expr.value);
		}
		return expr.value;
	}

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);

		/* START short circuit */
		switch (soperator(expr.operator.type)) {
		case AND:
			/*
			 * if Int -> bitwise AND else shortcircuit AND
			 */
			if (left instanceof BigInteger) {
				return invoke(expr.type, "and", left, evaluate(expr.right));
			} else {
				if (isTruthy(expr.type, expr.left)) {
					return evaluate(expr.right);
				} else {
					return left;
				}
			}
		case OR:
			/*
			 * if Int -> bitwise OR else shortcircuit OR
			 */
			if (left instanceof BigInteger) {
				return invoke(expr.type, "or", left, evaluate(expr.right));
			} else {
				if (isTruthy(expr.type, expr.left)) {
					return left;
				} else {
					return evaluate(expr.right);
				}
			}
		default:
			break;
		}
		/* END short circuit */

		Object right = evaluate(expr.right);

		switch (keyword(expr.operator.type)) {
		case GCD:
			return invoke(expr.type, "gcd", left, right);
		case MAX:
			return invoke(expr.type, "max", left, right);
		case MIN:
			return invoke(expr.type, "min", left, right);
		default:
			break;

		}

		switch (soperator(expr.operator.type)) {
		case GREATER:
			return invoke(expr.type, "gt", left, right);

		case GREATER_EQUAL:
			return invoke(expr.type, "ge", left, right);

		case LESS:
			return invoke(expr.type, "lt", left, right);

		case LESS_EQUAL:
			return invoke(expr.type, "le", left, right);

		case MINUS:
			return invoke(expr.type, "minus", left, right);

		case SLASH:
			return invoke(expr.type, "div", left, right);

		case PERCENT:
			return invoke(expr.type, "rem", left, right);

		case STAR:
			return invoke(expr.type, "mul", left, right);

		case PLUS:
			return invoke(expr.type, "plus", left, right);

		case NOT_EQUAL:
			return invoke(expr.type, "neq", left, right);

		case EQUAL:
			return invoke(expr.type, "eq", left, right);

		case POW:
			return invoke(expr.type, "pow", left, right);
		case LEFTSHIFT:
			return invoke(expr.type, "lsh", left, right);
		case RIGHTSHIFT:
			return invoke(expr.type, "rsh", left, right);
		case XOR:
			return invoke(expr.type, "xor", left, right);

		default:
			break;
		}

		// Unreachable.
		return null;
	}

	@Override
	public Object visitBlockExpr(Expr.Block expr) {
		Environment env = new Environment(environment);
		Environment previous = this.environment;
		try {
			this.environment = env;

			for (int i = 0; i < expr.expressions.size() - 1; i++) {
				evaluate(expr.expressions.get(i));
			}
			/* return value of last expression */
			return evaluate(expr.last);
		} finally {
			this.environment = previous;
		}
	}

	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);

		switch (soperator(expr.operator.type)) {

		case MINUS:
			return invoke(expr.type, "neg", right);

		case PLUS:
			return right;

		case NOT:
			return invoke(expr.type, "not", right);

		case PLUSPLUS:
			return invoke(expr.type, "inc", right);

		case MINMIN:
			return invoke(expr.type, "dec", right);

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
			break;
		}

		// Unreachable.
		return null;
	}

	@Override
	public Object visitPostfixExpr(Expr.Postfix expr) {

		Object left = evaluate(expr.left);

		switch (soperator(expr.operator.type)) {

		case PLUSPLUS:
			Object xx = invoke(expr.type, "inc", left);
			assign(((Expr.Variable) expr.left), xx);
			return left;

		case MINMIN:
			xx = invoke(expr.type, "dec", left);
			assign(((Expr.Variable) expr.left), xx);
			return left;

		case BANG:
			return invoke(expr.type, "fac", left);

		default:
		}

		// Unreachable.
		return null;
	}

	@Override
	public Object visitAssignExpr(Assign expr) {

		Object left = environment.get(expr.name);
		Object right = evaluate(expr.value);
		Object value = right;

		switch (soperator(expr.op.type)) {
		case ASSIGN:
			break;
		case PLUSIS:
			value = invoke(expr.type, "plus", left, right);
			break;
		case MINIS:
			value = invoke(expr.type, "minus", left, right);
			break;
		case STARIS:
			value = invoke(expr.type, "mul", left, right);
			break;
		case SLASHIS:
			value = invoke(expr.type, "div", left, right);
			break;
		case PERCENTIS:
			value = invoke(expr.type, "rem", left, right);
			break;
		case POWIS:
			value = invoke(expr.type, "pow", left, right);
			break;
		case ANDIS:
			value = invoke(expr.type, "and", left, right);
			break;
		case ORIS:
			value = invoke(expr.type, "or", left, right);
			break;
		case LSHIFTIS:
			value = invoke(expr.type, "ls", left, right);
			break;
		case RSHIFTIS:
			value = invoke(expr.type, "rs", left, right);
			break;
		default:
			break;
		}
		assign(expr, value);
		return value;
	}

	@Override
	public Object visitWhileExpr(Expr.While stmt) {
		while (isTruthy(stmt.condition.type, stmt.condition)) {
			try {
				evaluate(stmt.body);
			} catch (BreakJump breakJump) {
				break;
			} catch (ContinueJump continueJump) {
				// Do nothing.
			}
		}
		return null;
	}

	@Override
	public Object visitIfExpr(Expr.If stmt) {
		if (isTruthy(stmt.condition.type, stmt.condition)) {
			return evaluate(stmt.thenBranch);
		} else if (stmt.elseBranch != null) {
			return evaluate(stmt.elseBranch);
		} else {
			return null;
		}
	}

	@Override
	public Object visitReturnExpr(Expr.Return stmt) {
		Object value = null;
		if (stmt.value != null)
			value = evaluate(stmt.value);
		throw new MuReturn(value);
	}

	@Override
	public Object visitModuleExpr(Expr.Module stmt) {
		moduleName = stmt.name.lexeme;
		return null;
	}

	@Override
	public Object visitSeqExpr(Expr.Seq lst) {
		List<Object> result = new ArrayList<>();
		for (Expr expr : lst.exprs) {
			result.add(evaluate(expr));
		}
		return result;
	}

	@Override
	public Object visitSetExpr(Expr.Set set) {
		Set<Object> result = new HashSet<>();
		for (Expr expr : set.exprs) {
			result.add(evaluate(expr));
		}
		return result;
	}

	@Override
	public Object visitRangeExpr(Expr.Range rng) {
		List<Object> result = new ArrayList<>();
		Object start = evaluate(rng.start);
		// Int
		if (start instanceof BigInteger) {
			BigInteger st = (BigInteger) start;
			if (!rng.startIncl)
				st = st.add(BigInteger.ONE);
			BigInteger end = ((BigInteger) evaluate(rng.end));
			if (rng.endIncl)
				end = end.add(BigInteger.ONE);

			for (int i = st.intValue(); i < end.intValue(); i++) {
				result.add(BigInteger.valueOf(i));
			}
		}
		// Char
		if (start instanceof Integer) {
			int st = (Integer) start;
			if (!rng.startIncl)
				++st;
			int end = ((Integer) evaluate(rng.end));
			if (rng.endIncl)
				++end;

			for (int i = st; i < end; i++) {
				result.add(Integer.valueOf(i));
			}
		}
		return result;
	}

	@Override
	public Object visitMappingExpr(Mapping expr) {
		return evaluate(expr.value);
	}

	@Override
	public Object visitMapExpr(Expr.Map map) {
		ListMap<Object> listmap = new ListMap<>();
		for (Expr expr : map.mappings) {
			Object value = evaluate(expr);
			if (expr instanceof Expr.Mapping) {
				Expr.Mapping entry = (Expr.Mapping) expr;
				environment.define(entry.key, value, typeFromValue(value));
				listmap.put(entry.key, value);
			} else {
				listmap.put(null, value);
			}
		}
		return listmap;
	}

	@Override
	public Object visitForExpr(For expr) {
		Environment env = new Environment(environment);
		@SuppressWarnings("unchecked")
		List<Object> values = (List<Object>) evaluate(expr.range);
		String type = typeFromValue(values.get(0));
		env.define(expr.var.name.lexeme, values.get(0), typeFromValue(values.get(0)));

		for (int i = 0; i < values.size(); i++) {
			try {
				env.assign(expr.var.name, values.get(i), type);
				executeBlock(expr.body.expressions, env);
			} catch (BreakJump breakJump) {
				break;
			} catch (ContinueJump continueJump) {
				// Do nothing.
			}
		}
		return null;
	}

	@Override
	public Object visitClassExpr(Expr.ClassDef stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCallExpr(Call expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitGetterExpr(Getter expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitSetterExpr(Setter expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitSuperExpr(Super expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitThisExpr(This expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitBreakExpr(Break stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitContinueExpr(Continue stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitFunctionExpr(Expr.Function stmt) {
		// TODO
		// Function function = new Function(stmt.name.lexeme, stmt.body, environment,
		// false);
		// environment.define(stmt.name.lexeme, function);
		return null;
	}

	@Override
	public Object visitTypeExpr(Expr.Type expr) {
		// TODO Auto-generated method stub
		return null;
	}

	/* UTIL */
	public static String typeFromValue(Object val) {
		/* We have char, string, int, real, bool */
		if (val == null)
			return "Void";
		if (val instanceof Integer)
			return "Char";
		if (val instanceof String)
			return "String";
		if (val instanceof BigInteger)
			return "Int";
		if (val instanceof BigDecimal)
			return "Real";
		if (val instanceof Boolean)
			return "Bool";

		if (val instanceof MuClass)
			return ((MuClass) val).name;
		if (val instanceof MuFunction)
			return "Func";
		if (val instanceof java.util.List)
			return "List";
		if (val instanceof java.util.Set)
			return "Set";

		return "None";
	}

	@SuppressWarnings("unchecked")
	private String stringify(Object object) {
		if (object == null)
			return "nil";
		if (object instanceof Integer) {
			return "'" + new String(Character.toChars((Integer) object)) + "'";
		}
		if (object instanceof String) {
			return "\"" + object + "\"";
		}
		if (object instanceof Collection) {
			return stringifyCollection((Collection<Object>) object);
		}
		if (object instanceof Map) {
			return stringifyMap((Map<String, Object>) object);
		}
		return object.toString();
	}

	private String unquote(String s) {
		int ix = 0;
		if(s.startsWith("\"")) ix = 1;
		int ixEnd = s.length();
		if(s.endsWith("\"")) ixEnd -= 1;
		return s.substring(ix, ixEnd);
	}
	
	private String stringifyCollection(Collection<Object> collection) {
		StringBuilder sb = new StringBuilder();
		char bracket = (collection instanceof Set) ? '{' : '[';
		sb.append(bracket);
		for (Object o : collection) {
			if (sb.length() > 1) {
				sb.append(",");
			}
			sb.append(stringify(o));
		}
		sb.append(bracket == '{' ? '}' : ']');
		return sb.toString();
	}

	private String stringifyMap(Map<String, Object> map) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Entry<String, Object> entry : map.entrySet()) {
			if (sb.length() > 1) {
				sb.append(",");
			}
			sb.append(entry.getKey()).append(":").append(stringify(entry.getValue()));
		}
		sb.append(")");
		return sb.toString();
	}

	private String interpolate(String string) {
		StringBuilder sb = new StringBuilder();
		int ix1 = string.indexOf("#{");
		int ix2 = string.indexOf('}');
		if (ix1 >= 0 && ix2 > ix1) {
			String name = string.substring(ix1 + 2, ix2);
			String pre = string.substring(0, ix1);
			String post = string.substring(ix2 + 1);
			Expr expr = Mu.eval(name);
			Object subst = evaluate(expr);
			if (subst == null) {
				return sb.append(string.substring(0, ix2 + 1)).append(interpolate(post)).toString();
			} else {
				sb.append(pre).append(subst);
				return sb.append(interpolate(post)).toString();
			}
		} else {
			return string;
		}
	}

	boolean isTruthy(String type, Expr expr) {
		Object condition = evaluate(expr);
		return (boolean) invoke(type, "isTrue", condition);
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
