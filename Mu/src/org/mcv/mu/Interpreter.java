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
import java.util.Set;

import org.mcv.mu.Expr.Assign;
import org.mcv.mu.Expr.Call;
import org.mcv.mu.Expr.Getter;
import org.mcv.mu.Expr.Mapping;
import org.mcv.mu.Expr.Setter;
import org.mcv.mu.Expr.Super;
import org.mcv.mu.Expr.This;
import org.mcv.mu.Expr.Variable;
import org.mcv.mu.Stmt.Break;
import org.mcv.mu.Stmt.Continue;
import org.mcv.mu.Stmt.Module;
import org.mcv.mu.stdlib.IAny;
import org.mcv.mu.stdlib.IBool;
import org.mcv.mu.stdlib.IChar;
import org.mcv.mu.stdlib.IFunc;
import org.mcv.mu.stdlib.IInt;
import org.mcv.mu.stdlib.IList;
import org.mcv.mu.stdlib.IMap;
import org.mcv.mu.stdlib.INone;
import org.mcv.mu.stdlib.INum;
import org.mcv.mu.stdlib.IRange;
import org.mcv.mu.stdlib.IReal;
import org.mcv.mu.stdlib.ISet;
import org.mcv.mu.stdlib.IString;
import org.mcv.mu.stdlib.IText;
import org.mcv.mu.stdlib.IType;
import org.mcv.mu.stdlib.IVoid;
import org.mcv.mu.stdlib.TypeError;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

	final Environment globals = new Environment();
	static final Map<String, TypeInfo> typePool = new HashMap<>();
	private Environment environment = globals;
	@SuppressWarnings("unused")
	private String moduleName;
	
	Interpreter() {
		globals.define("clock", new MuCallable() {
			@Override
			public int arity() {
				return 0;
			}

			@Override
			public Object call(Interpreter interpreter, List<Object> arguments) {
				return (double) System.currentTimeMillis() / 1000.0;
			}
		}, "Func");

		typePool.put("None", new TypeInfo("None", INone.class));
		typePool.put("Void", new TypeInfo("Void", IVoid.class));
		typePool.put("Bool", new TypeInfo("Bool", IBool.class));
		TypeInfo num = new TypeInfo("Num", INum.class);
		typePool.put("Int&Real", num);
		typePool.put("Real&Int", num);
		typePool.put("Int", new TypeInfo("Int", IInt.class));
		typePool.put("Real", new TypeInfo("Real", IReal.class));
		TypeInfo text = new TypeInfo("Text", IText.class);
		typePool.put("Char&String", text);
		typePool.put("String&Char", text);
		typePool.put("Char", new TypeInfo("Char", IChar.class));
		typePool.put("String", new TypeInfo("String", IString.class));
		typePool.put("Any", new TypeInfo("Any", IAny.class));
		typePool.put("Type", new TypeInfo("Type", IType.class));
		typePool.put("Func", new TypeInfo("Func", IFunc.class));
		typePool.put("List", new TypeInfo("List", IList.class));
		typePool.put("Set", new TypeInfo("Set", ISet.class));
		typePool.put("Map", new TypeInfo("Map", IMap.class));
		typePool.put("Range", new TypeInfo("Range", IRange.class));
	}

	void interpret(List<Stmt> statements) {
		try {
			for (Stmt statement : statements) {
				execute(statement);
			}
		} catch (Exception error) {
			Mu.runtimeError(error);
		}
	}

	private void execute(Stmt stmt) {
		stmt.accept(this);
	}

	private void execute(Expr expr) {
		expr.accept(this);
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		evaluate(stmt.expr);
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}
		environment.define(stmt.name.lexeme, value, typeFromValue(value));
		return null;
	}

	@Override
	public Void visitValStmt(Stmt.Val stmt) {
		Object value = null;
		if (stmt.initializer == null) {
			throw new RuntimeError(stmt.name, "val must be initialized!");
		}
		value = evaluate(stmt.initializer);
		environment.define(stmt.name.lexeme, value, typeFromValue(value));
		return null;
	}

	@SuppressWarnings("rawtypes")
	private String stringify(Object object) {
		if (object == null)
			return "nil";
		if (object instanceof Integer) {
			return "'"+ new String(Character.toChars((Integer) object)) + "'";
		}
		if (object instanceof String) {
			return "\""+ object + "\"";
		}
		if(object instanceof Collection) {
			StringBuilder sb = new StringBuilder();
			if(object instanceof Set) sb.append("{");
			if(object instanceof List) sb.append("[");
			for(Object o : ((Collection)object)) {
				if(sb.length() > 1) {
					sb.append(",");
				}
				sb.append(stringify(o));
			}
			if(object instanceof Set) sb.append("}");
			if(object instanceof List) sb.append("]");
			return sb.toString();
		}
		return object.toString();
	}

	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return environment.get(expr.name);
	}

	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		/* string interpolation */
		if(expr.value instanceof String) {
			return substVars((String)expr.value);
		}
		return expr.value;
	}

	private String substVars(String string) {
		StringBuilder sb = new StringBuilder();
		int ix1 = string.indexOf("#{");
		int ix2 = string.indexOf('}');
		if(ix1 >= 0 && ix2 > ix1) {
			String name = string.substring(ix1+2, ix2);
			String pre = string.substring(0, ix1);
			String post = string.substring(ix2+1);
			Expr expr = Mu.eval(name);
			Object subst = evaluate(expr);
			if(subst == null) {
				return sb.append(string.substring(0, ix2+1)).append(substVars(post)).toString();
			} else {
				sb.append(pre).append(subst);
				return sb.append(substVars(post)).toString();
			}
		} else {
			return string;
		}
	}
	
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
			throw new TypeError(left + " " + method + " " + right);
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
				throw new TypeError(left + " " + method + " " + right);
			}
			try {
				return method.invoke(null, convert.invoke(null, left), convert.invoke(null, right));				
			} catch (Exception e2) {
				throw new TypeError(left + " " + method + " " + right);
			}
		} catch (Exception e) {
			throw new TypeError(left + " " + method + " " + right);
		}
	}

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
			throw new TypeError(method + " " + arg);
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
				throw new TypeError(method + " " + arg);
			}
			try {
				return method.invoke(null, convert.invoke(null, arg));
			} catch (Exception e2) {
				throw new TypeError(method + " " + arg);
			}
		} catch (Exception e) {
			throw new TypeError(method + " " + arg);
		}
	}

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		
		/* START short circuit */
		switch (expr.operator.type) {
		case AND:
			/* if Int -> bitwise AND 
			 * else shortcircuit AND */
			if(left instanceof BigInteger) {
				return invoke(expr.type, "and", left, evaluate(expr.right));
			} else {
				if(isTruthy(expr.type, expr.left)) {
					return evaluate(expr.right);
				} else {
					return left;
				}
			}
		case OR:
			/* if Int -> bitwise OR 
			 * else shortcircuit OR */
			if(left instanceof BigInteger) {
				return invoke(expr.type, "or", left, evaluate(expr.right));
			} else {
				if(isTruthy(expr.type, expr.left)) {
					return left;
				} else {
					return evaluate(expr.right);
				}
			}
		default:
			break;
		}
		/* END short circuit*/
		
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {

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

		case GCD:
			return invoke(expr.type, "gcd", left, right);
		case MAX:
			return invoke(expr.type, "max", left, right);
		case MIN:
			return invoke(expr.type, "min", left, right);
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

			for (int i = 0; i < expr.statements.size() - 1; i++) {
				execute(expr.statements.get(i));
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

		switch (expr.operator.type) {

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

		case ABS:
		case BACKSLASH:
			return invoke(expr.type, "abs", right);

		case SQRT:
			return invoke(expr.type, "sqrt", right);

		default:
		}

		// Unreachable.
		return null;
	}

	@Override
	public Object visitPostfixExpr(Expr.Postfix expr) {

		Object left = evaluate(expr.left);

		switch (expr.operator.type) {

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

	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	@Override
	public Object visitAssignExpr(Assign expr) {
		
		Object left = environment.get(expr.name);
		Object right = evaluate(expr.value);
		Object value = right;
		
		switch (expr.op.type) {
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

	private void assign(Assign expr, Object value) {
		environment.assign(expr.name, value, typeFromValue(value));
	}

	private void assign(Variable expr, Object value) {
		environment.assign(expr.name, value, typeFromValue(value));
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		while (isTruthy(stmt.condition.type, stmt.condition)) {
			try {
				execute(stmt.body);
			} catch (BreakJump breakJump) {
				break;
			} catch (ContinueJump continueJump) {
				// Do nothing.
			}
		}
		return null;
	}

	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCallExpr(Call expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitGetExpr(Getter expr) {
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

	public void executeBlock(List<Stmt> body, Environment environment2) {
		// TODO Auto-generated method stub
	}

	public void resolve(Expr expr, int i) {
		// TODO Auto-generated method stub
	}

	@Override
	public Object visitFunctionExpr(org.mcv.mu.Expr.Function expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitBreakStmt(Break stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitContinueStmt(Continue stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		// TODO
		// Function function = new Function(stmt.name.lexeme, stmt.body, environment,
		// false);
		// environment.define(stmt.name.lexeme, function);
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
	public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null)
			value = evaluate(stmt.value);
		throw new MuReturn(value);
	}

	boolean isTruthy(String type, Expr expr) {
		Object condition = evaluate(expr);
		return (boolean) invoke(type, "isTrue", condition);
	}

	@Override
	public Void visitModuleStmt(Module stmt) {
		moduleName = stmt.name.lexeme;
		return null;
	}

	@Override
	public Object visitListExpr(Expr.List lst) {
		List<Object> result = new ArrayList<>();
		for(Expr expr : lst.exprs) {
			result.add(evaluate(expr));
		}
		return result;
	}

	@Override
	public Object visitSetExpr(Expr.Set set) {
		Set<Object> result = new HashSet<>();
		for(Expr expr : set.exprs) {
			result.add(evaluate(expr));
		}
		return result;
	}

	@Override
	public Object visitRangeExpr(Expr.Range rng) {
		List<Object> result = new ArrayList<>();
		Object start = evaluate(rng.start);
		// Int
		if(start instanceof BigInteger) {
			BigInteger st = (BigInteger)start;
			if(!rng.startIncl) st = st.add(BigInteger.ONE);
			BigInteger end = ((BigInteger)evaluate(rng.end));
			if(rng.endIncl) end = end.add(BigInteger.ONE);
		
			for(int i=st.intValue(); i < end.intValue(); i++) {
				result.add(BigInteger.valueOf(i));
			}
		}
		// Char
		if(start instanceof Integer) {
			int st = (Integer)start;
			if(!rng.startIncl) ++st;
			int end = ((Integer)evaluate(rng.end));
			if(rng.endIncl) ++end;
		
			for(int i=st; i < end; i++) {
				result.add(Integer.valueOf(i));
			}
		}
		return result;
	}

	public static String typeFromValue(Object val) {
		/* We have char, string, int, real, bool */
		if(val == null) return "Void";
		if(val instanceof Integer) return "Char";
		if(val instanceof String) return "String";
		if(val instanceof BigInteger) return "Int";
		if(val instanceof BigDecimal) return "Real";
		if(val instanceof Boolean) return "Bool";

		if(val instanceof MuClass) return ((MuClass)val).name;
		if(val instanceof MuFunction) return "Func";
		if(val instanceof java.util.List) return "List";
		if(val instanceof java.util.Set) return "Set";
			
		return "None";
	}

	@Override
	public Object visitTypeExpr(Expr.Type expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitMappingExpr(Mapping expr) {
		return evaluate(expr.value);
	}

	@Override
	public Object visitMapExpr(Expr.Map map) {
		ListMap<Object> listmap = new ListMap<>();
		for(Stmt stmt : map.mappings) {
			if(stmt instanceof Stmt.Expression) {
				Expr expr = ((Stmt.Expression)stmt).expr;
				Object value = evaluate(expr);
				if(expr instanceof Expr.Mapping) {
					Expr.Mapping entry = (Expr.Mapping)expr;
					environment.define(entry.key, value, typeFromValue(value));
					listmap.put(entry.key, value);
				} else {
					listmap.put(null, value);
				}
			}
		}
		return listmap;
	}

}
