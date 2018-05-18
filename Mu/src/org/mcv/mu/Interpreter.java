package org.mcv.mu;

import java.lang.reflect.Method;
import org.mcv.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
		// FOR NOW
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
	Object invoke(Type type, String op, Object left, Object right) {
		TypeInfo info = typePool.get(type.name);
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
	private Object invoke(Type type, String op, Object arg) {
		TypeInfo info = typePool.get(type.name);
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
	public Object visitVarDef(Expr.Var stmt) {
		Object value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}
		if(value instanceof Type) {
			environment.define(stmt.name.lexeme, null, (Type)value, true, stmt.isShared());
		} else {
			environment.define(stmt.name.lexeme, value, typeFromValue(value), true, stmt.isShared());
		}
		return null;
	}

	@Override
	public Object visitValDef(Expr.Val stmt) {
		Object value = null;
		if (stmt.initializer == null) {
			return Mu.runtimeError("Val %s must be initialized!", stmt.name.lexeme);
		}
		value = evaluate(stmt.initializer);
		environment.define(stmt.name.lexeme, value, typeFromValue(value), false, stmt.isShared());
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
		if(expr.value instanceof Double) {
			// INF and NaN
			
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
			if (left instanceof Integer) {
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
			if (left instanceof Integer) {
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
		case EQEQ:
			return invoke(expr.type, "eqeq", left, right);

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

		Object left = environment.get(expr.name.lexeme);
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
	public Object visitSelectExpr(Expr.Select stmt) {
		Object zwitch = evaluate(stmt.condition);
		for(Pair<Expr, Expr> when : zip(stmt.whenExpressions, stmt.whenBranches)) {
			if(evaluate(when.left).equals(zwitch)) {
				return evaluate(when.right);
			}
		} 
		if (stmt.elseBranch != null) {
			return evaluate(stmt.elseBranch);
		}
		return null;
	}

	public static <A, B> List<Pair<A, B>> zip(List<A> listA, List<B> listB) {
	    if (listA.size() != listB.size()) {
	        throw new IllegalArgumentException("Lists must have same size");
	    }
	    List<Pair<A, B>> pairList = new LinkedList<>();
	    for (int index = 0; index < listA.size(); index++) {
	        pairList.add(new Pair<>(listA.get(index), listB.get(index), false));
	    }
	    return pairList;
	}
	
	@Override
	public Object visitForExpr(For expr) {
		Environment env = new Environment(environment);
		@SuppressWarnings("unchecked")
		List<Object> values = (List<Object>) evaluate(expr.range);
		Type type = typeFromValue(values.get(0));
		env.define(expr.var.name.lexeme, values.get(0), typeFromValue(values.get(0)), true, false);

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
	public Object visitReturnExpr(Expr.Return stmt) {
		Object value = null;
		if (stmt.value != null)
			value = evaluate(stmt.value);
		throw new MuReturn(value);
	}

	@Override
	public Object visitModuleDef(Expr.Module stmt) {
		// TODO
		moduleName = stmt.name.lexeme;
		return null;
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

			if(st.isNaN() || end.isNaN()) {
				return null;
			}
			if(st.isNegativeInfinity() || end.isPositiveInfinity()) {
				// do not evaluate!!
				return rng;
			}

			for (int i = st.intValue(); i < end.intValue(); i++) {
				result.add(Integer.valueOf(i));
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
	public Object visitSeqExpr(Expr.Seq lst) {
		// OR IS IT A SUBSCRIPT???
		List<Object> result = new ArrayList<>();
		for (Expr expr : lst.exprs) {
			result.add(evaluate(expr));
		}
		return result;
	}

	@Override
	public Object visitMapExpr(Expr.Map map) {
		// OR IS IT A FUNCTION CALL??
		ListMap<Object> listmap = new ListMap<>();
		for (Expr expr : map.mappings) {
			Object value = evaluate(expr);
			if (expr instanceof Expr.Mapping) {
				Expr.Mapping entry = (Expr.Mapping) expr;
				// mutable true, shared false ??
				environment.define(entry.key, value, typeFromValue(value), true, false);
				listmap.put(entry.key, value);
			} else {
				listmap.put(null, value);
			}
		}
		return listmap;
	}

	@Override
	public Object visitClassDef(Expr.ClassDef stmt) {
		environment.define(stmt.name.lexeme, stmt, new Type.SignatureType(stmt), false, true);
		return null;
	}
	@Override
	public Object visitFuncDef(Expr.FuncDef stmt) {
		environment.define(stmt.name.lexeme, stmt, new Type.SignatureType(stmt), false, true);
		return null;
	}

	@Override
	public Object visitInterfaceDef(InterfaceDef stmt) {
		environment.define(stmt.name.lexeme, stmt, new Type.SignatureType(stmt), false, true);
		return null;
	}

	@Override
	public Object visitIterDef(IterDef stmt) {
		environment.define(stmt.name.lexeme, stmt, new Type.SignatureType(stmt), false, true);
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object visitGetterExpr(Getter expr) {
		Object obj = evaluate(expr.object);
		if(obj instanceof ListMap) {
			return ((ListMap<Object>)obj).get(expr.name.lexeme);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object visitSetterExpr(Setter expr) {
		Object obj = evaluate(expr.object);
		if(obj instanceof ListMap) {
			return ((ListMap<Object>)obj).put(expr.name.lexeme, evaluate(expr.value));
		}
		return null;
	}

	@Override
	public Object visitThisExpr(This expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
    public Object visitBreakExpr(Expr.Break stmt) {
        throw new BreakJump();
    }

    @Override
    public Object visitContinueExpr(Expr.Continue stmt) {
        throw new ContinueJump();
    }
    
    @Override
	public Object visitTypeLiteralExpr(Expr.TypeLiteral expr) {
		if(expr.name != null)
			environment.define(expr.name.lexeme, expr.literal, new Type("Type"), false, expr.isShared());
		return expr.literal;
	}

	/* UTIL */
	public static Type typeFromValue(Object val) {
		/* We have char, string, int, real, bool */
		if (val == null)
			return Type.Void;
		if (val instanceof Integer)
			return Type.Char;
		if (val instanceof String)
			return Type.String;
		if (val instanceof BigInteger)
			return Type.Int;
		if (val instanceof Double)
			return Type.Real;
		if (val instanceof Boolean)
			return Type.Bool;

		if (val instanceof Expr.ClassDef)
			return new Type.SignatureType((ClassDef)val);
		if (val instanceof Expr.InterfaceDef)
			return new Type.SignatureType((InterfaceDef)val);
		if (val instanceof Expr.IterDef)
			return new Type.SignatureType((IterDef)val);
		if (val instanceof Expr.FuncDef)
			return new Type.SignatureType((FuncDef)val);
		
		if (val instanceof java.util.List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) val;
			if(!list.isEmpty()) {
				return new Type.ListType(typeFromValue(list.get(0)));
			}
		}
		if (val instanceof java.util.Set) {
			@SuppressWarnings("unchecked")
			Set<Object> set = (Set<Object>) val;
			if(!set.isEmpty()) {
				return new Type.SetType(typeFromValue(set.iterator().next()));
			}
		}
		return new Type("None");
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

	boolean isTruthy(Type type, Expr expr) {
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
