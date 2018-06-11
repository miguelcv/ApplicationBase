package org.mcv.mu;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.mcv.math.BigInteger;
import org.mcv.mu.Expr.*;
import org.mcv.mu.Parser.ParserError;
import org.mcv.uom.UnitValue;

public class Interpreter {

	Environment main;
	Environment environment;
	Visitors visitors;
	Mu handler;
	
	Expr current;

	public static class InterpreterError extends ParserError {
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

	public Interpreter(Environment main, Mu mu) {
		visitors = new Visitors(this);
		handler = mu;
		this.environment = new Environment("default", main);
		main.define("μ", new Result(this, Type.Any), false, true);
	}

	// DEBUG
	List<Expr> gexpressions;

	void interpret(List<Expr> expressions) {
		// DEBUG
		gexpressions = expressions;
		for (Expr current : expressions) {
			try {
				Result result = evaluate(current);
				if(result.type.equals(Type.Exception)) {
					throw (MuException)result.value;
				}
			} catch (ReturnJump ret) {
				System.out.println("Program exited with: " + stringify(ret.value));
				return;
			} catch (MuException me) {
				if(environment.get("$this") != null) {
					Template tmpl = (Template)environment.get("$this").value;
					if(tmpl.errors != null && !tmpl.errors.isEmpty()) {
						Stack<Block> handlers = tmpl.errors;
						environment.define("$exception", new Result(me, Type.Exception), false, false);
						for(Block block : handlers) {
							executeBlock(block.expressions, environment);
						}
						continue;
					}
				}
				me.expr = current;
				me.line = current.line;
				handler.error(me);
			} catch (InterpreterError e) {
				e.expr = current;
				e.line = current.line;
				handler.error(e);
			} catch (Exception e) {
				InterpreterError ie = new InterpreterError(e);
				ie.expr = current;
				ie.line = current.line;
				handler.error(ie);
			}
		}
	}

	Result evaluate(Expr expr) {
		Result res = expr.accept(visitors);
		if(res.value instanceof Expr) {
			return ((Expr)res.value).accept(visitors);
		}
		return res;
	}

	/* invoke binary operator */
	Result invoke(String op, Result left, Result right, Type returnType) {

		if(left.value instanceof UnitValue) {
			left.type = Type.Unit;
			((UnitValue)left.value).resolve();
		}
		if(right.value instanceof UnitValue) {
			right.type = Type.Unit;
			((UnitValue)right.value).resolve();
		}
		
		Type type = left.type;
		Object leftVal = left.value;
		Object rightVal = right.value;
		Object func = type.lookup(environment, op, returnType, left, right);

		if (func == null) {
			if (!left.type.equals(right.type)) {
				Result[] converted = widen(left, right);
				left = converted[0];
				right = converted[1];
				leftVal = left.value;
				rightVal = right.value;
				returnType = left.type;
			}
			func = type.lookup(environment, op, returnType, left, right);
			if (func == null) {
				throw new InterpreterError("Func %s not found for types %s and %s", op, left.type, right.type);
			}
		}

		if (func instanceof Method) {
			Method method = (Method) func;
			try {
				return new Result(method.invoke(null, leftVal, rightVal), typeFromClass(method.getReturnType()));
			} catch(IllegalArgumentException iae) {
				if (!left.type.equals(right.type)) {
					Result[] converted = widen(left, right);
					left = converted[0];
					right = converted[1];
					leftVal = left.value;
					rightVal = right.value;
					returnType = left.type;
					func = returnType.lookup(environment, op, returnType, left, right);
					if (func == null) {
						throw new InterpreterError("Func %s not found for types %s and %s", op, left.type, right.type);
					}
					try {
						method = (Method)func;
						return new Result(method.invoke(null, leftVal, rightVal), typeFromClass(method.getReturnType()));
					} catch(Exception e) {
						throw new InterpreterError("Error invoking %s for types %s and %s", op, left.type, right.type);
					}
				}
				throw new InterpreterError("Func %s not found for types %s and %s", op, left.type, right.type);
			} catch(InvocationTargetException ite) {
				throw new MuException(new Result(ite.getCause(), Type.Exception));
			} catch (Exception e) {
				if(e instanceof InterpreterError) {
					throw (InterpreterError)e;
				}
				throw new InterpreterError("No such operator: %s %s %s", left.type, op, right.type);
			}
		} else {
			// TODO allow native operators
			throw new InterpreterError("No native operator: %s %s %s", left.type, op, right.type);
		}
	}

	/* invoke unary operator */
	Result invoke(String op, Type returnType, Result arg) {

		if(arg.value instanceof UnitValue) {
			arg.type = Type.Unit;
			((UnitValue)arg.value).resolve();
			returnType = Type.Real;			
		}

		Object func = arg.type.lookup(environment, op, returnType, arg);

		if (func == null) {
			throw new InterpreterError("Func " + op + " not found for type " + arg.type);
		}

		if (func instanceof Method) {
			Method method = (Method) func;
			try {
				return new Result(method.invoke(null, arg.value), returnType);
			} catch(IllegalArgumentException iae) {
				if(arg.value instanceof Double) {
					double d = (Double)arg.value;
					if(Double.isNaN(d)) {
						arg.value = BigInteger.NAN;
					}
					if(Double.isInfinite(d)) {
						if(d == Double.NEGATIVE_INFINITY) {
							arg.value = BigInteger.NEGATIVE_INFINITY;
						}
						if(d == Double.POSITIVE_INFINITY) {
							arg.value = BigInteger.POSITIVE_INFINITY;
						}
					}
					try {
						return new Result(method.invoke(null, arg.value), typeFromClass(method.getReturnType()));
					} catch(Exception e) {
						throw new InterpreterError(e);
					}
				}
				throw new InterpreterError("Error invoking operator: %s %s", op, arg.type);
			} catch (Exception e) {
				throw new InterpreterError("Error invoking operator: %s %s", op, arg.type);
			}
		} else {
			throw new InterpreterError("No such operator: %s %s", op, arg.type);
		}
	}

	private Result[] widen(Result r1, Result r2) {

		Object func1 = r1.type.lookup(environment, "to" + r2.type, r2.type, r1);
		Object func2 = r2.type.lookup(environment, "to" + r1.type, r1.type, r2);
		Result[] res = new Result[2];

		if (func1 != null && func1 instanceof Method) {
			res[0] = convert((Method) func1, r1);
			res[1] = r2;
		} else if (func2 != null && func2 instanceof Method) {
			res[0] = r1;
			res[1] = convert((Method) func2, r2);
		} else {
			throw new InterpreterError("No conversion between types %s and %s", r1.type, r2.type);
		}
		return res;
	}

	private Result convert(Method func, Result r) {
		try {
			Object val = func.invoke(null, r.value);
			return new Result(val, typeFromClass(val.getClass()));
		} catch (Exception e) {
			return r;
		}
	}

	static class Source {
		Iterator<Object> it = null;
		Map<String, Object> map = null;
		Result value;
		
		@SuppressWarnings("unchecked")
		public Source(int n, Result value) {
			this.value = value;
			if(n > 1) {
				if(value.value instanceof List) {
					it = ((List<Object>) value.value).iterator();
				}
				if(value.value instanceof Map) {
					map = (Map<String, Object>)value.value;
				}
			}
		}

		public Result next(String key) {
			if(it != null) {
				if(it.hasNext()) {
					Object next = it.next();
					return new Result(next, ((Type.ListType)value.type).eltType);
				}
				return new Result(null, Type.Void);
			}
			if(map != null) {
				if(map.containsKey(key)) {
					Object val = map.get(key);
					return new Result(val, ((Type.MapType)value.type).valType);
				}
				return new Result(null, Type.Void);
			}
			return value;
		}
	}
	
	Result assign(Assign expr, Result value) {
		Result result = null;
		Source src = new Source(expr.var.size(), value);
		for(Token tok : expr.var) {
			Result curr = environment.get(tok.lexeme);
			if(curr == null) {
				throw new InterpreterError("Undefined variable %s", tok.lexeme);
			}
			if(curr.value instanceof Property) {
				callSetter((Property)curr.value, src.next(tok.lexeme));
			}
			result = environment.assign(tok, src.next(tok.lexeme));
		}
		return result;
	}

	Result assign(Variable expr, Result value) {
		Result curr = environment.get(expr.name);
		if(curr == null) {
			throw new InterpreterError("Undefined variable", expr.name);
		}
		if(curr.value instanceof Property) {
			callSetter((Property)curr.value, value);
		}
		return environment.assign(expr.name, value);
	}


	public Result executeBlock(List<Expr> body, Environment environment) {
		Environment previous = this.environment;
		try {
			this.environment = environment;
			Result value = null;
			for (int i = 0; i < body.size(); i++) {
				value = evaluate(body.get(i));
			}
			return value;
		} catch (ReturnJump ret) {
			throw ret;
		} catch (MuException me) {
			throw me;
		} catch(BreakJump brk) {
			throw brk;
		} catch(ContinueJump cnt) {
			throw cnt;
		} catch (InterpreterError ie) {
			throw ie;
		} catch (Exception e) {
			throw new InterpreterError(e);
		} finally {
			this.environment = previous;
		}
	}


	public Result executeBlockReturn(List<Expr> body, Environment environment) {
		Result val = null;
		try {
			val = executeBlock(body, environment);
		} catch (ReturnJump ret) {
			val = ret.value;
		} catch(BreakJump brk) {
			// ignore
		} catch(ContinueJump cnt) {
			// ignore
		}
		return val;
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

	Result doMap(Expr.Map map) {
		ListMap<Object> listmap = new ListMap<>();

		Type valType = Type.None;
		for (Expr expr : map.mappings) {
			Result value = evaluate(expr);
			if (expr instanceof Expr.Mapping) {
				Expr.Mapping entry = (Expr.Mapping) expr;
				listmap.put(entry.key, value.value);
				valType = Type.unite(valType, value.type);
			} else {
				listmap.put(null, value.value);
				valType = Type.unite(valType, value.type);
			}
		}
		return new Result(listmap, new Type.MapType(valType));
	}

	Result sublist(Result seq, Expr.Range range, Type eltType) {
		int start = ((BigInteger) evaluate(range.start).value).intValue();
		int end = ((BigInteger) evaluate(range.end).value).intValue();
		if (seq.value instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) seq.value;
			List<Object> subList = list.subList(start, end);
			return new Result(subList, eltType);
		}
		return new Result(null, Type.Void);
	}

	Result call(Result func, Expr.Map args) {
		if (func.value instanceof Callee) {
			return Template.call(((Callee)func.value).parent, this, args);
		} else if (func.value instanceof Template) {
			return Template.call((Template) func.value, this, args);
		} else {
			throw new InterpreterError("Cannot call object %s", func.value);
		}
	}

	@SuppressWarnings("unchecked") Result subscript(Result listMap, Seq index, Expr value) {
		Result sub = evaluate(index.exprs.get(0));
		if (value != null) {
			Object val = evaluate(value).value;
			/* setter */
			if (listMap.value instanceof Map) {
				String key = (String) sub.value;
				Map<String, Object> map = (Map<String, Object>) listMap.value;
				if (map.containsKey(key)) {
					map.put(key, val);
				} else {
					throw new InterpreterError("No such key: %s", key);
				}
			}
			if (listMap.value instanceof List) {
				List<Object> list = ((List<Object>) listMap.value);
				int ix = ((BigInteger) sub.value).intValue();
				if (ix < 0)
					ix = list.size() + ix;
				list.set(ix, val);
			}
			return new Result(null, Type.Void);
		} else {
			/* getter */
			if (listMap.value instanceof Map) {
				String key = (String) sub.value;
				Map<String, Object> map = (Map<String, Object>) listMap.value;
				if (map.containsKey(key)) {
					Object val = map.get(key);
					return new Result(val, typeFromValue(val));
				} else {
					throw new InterpreterError("No such key: %s", key);
				}
			}
			if (listMap.value instanceof List) {
				List<Object> list = (List<Object>) listMap.value;
				int ix = ((BigInteger) sub.value).intValue();
				if (ix < 0)
					ix = list.size() + ix;
				Object val = list.get(ix);
				return new Result(val, typeFromValue(val));
			}
			return new Result(null, Type.Void);
		}
	}

	@SuppressWarnings("unchecked") Result dot(Result listMap, String key) {
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
			Template tmpl = (Template) listMap.value;
			Result value = tmpl.closure.get(key);
			if(value == null) {
				throw new InterpreterError("Interface %s not found", key);
			}
			if(value.value instanceof Property) {
				return callGetter((Property)value.value);
			}
			return value;
		}
		if (listMap.value instanceof Callee) {
			Callee callee = (Callee) listMap.value;
			Result value = callee.closure.get(key);
			
			/* not found in current object, try mixins */
			if(value == null) {
				if(callee.mixins != null) {
					for(Mixin mixin : callee.mixins) {
						key = mixin.where.getOrDefault(key, key);
						value = mixin.object.closure.get(key);
						if(value != null) {
							break;
						}
					}
				}
			}
			if(value == null) {
				throw new InterpreterError("Interface %s not found", key);
			}
			
			/* Getter */
			if(value.value instanceof Property) {
				/* it's a getter */
				return callGetter((Property)value.value);
			}
			return value;
		}
		return new Result(null, Type.Void);
	}

	@SuppressWarnings("unchecked") Result setdot(Result listMap, String key, Object value) {
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
			Template tmpl = (Template) listMap.value;
			Result rs = tmpl.closure.get(key);
			if(rs == null) {
				throw new InterpreterError("Undefined field %s", key);
			}
			rs.value = value;
		}
		if (listMap.value instanceof Callee) {
			// call setter
			Callee callee = (Callee) listMap.value;
			Result rs = callee.closure.get(key);
			if(rs == null) {
				throw new InterpreterError("Undefined field %s", key);
			}
			if(rs.value instanceof Property) {
				callSetter((Property)rs.value, new Result(value, typeFromValue(value)));
			}
			rs.value = value;				
		}
		return new Result(null, Type.Void);
	}

	/* UTIL */

	static Type typeFromClass(Class<?> clz) {
		/* We have char, string, int, real, bool */
		if (clz.equals(Void.class))
			return Type.Void;
		if (clz.isAssignableFrom(Type.class))
			return Type.Type;
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
		if(clz.equals(UnitValue.class)) {
			return Type.Real;
		}
		if (clz.equals(Boolean.class) || clz.equals(boolean.class))
			return Type.Bool;
		if (clz.isAssignableFrom(java.util.List.class)) {
			return new Type.ListType(Type.Any);
		}
		if (clz.isAssignableFrom(java.util.Set.class)) {
			return new Type.SetType(Type.Any);
		}
		if (clz.isAssignableFrom(java.util.Map.class)) {
			return new Type.MapType(Type.Any);
		}
		if (clz.isAssignableFrom(Map.Entry.class)) {
			return new Type.MapType(Type.Any);
		}
		if (clz.isAssignableFrom(Pointer.class)) {
			return new Type.RefType(Type.Any);
		}
		System.err.println("Type is " + clz);
		return Type.None;
	}

	public static Type typeFromValue(Object val) {

		/* We have char, string, int, real, bool */
		if (val == null)
			return Type.Void;
		if (val instanceof Type)
			return Type.Type;
		if (val instanceof Integer)
			return Type.Char;
		if(val instanceof Symbol) {
			return Type.Any;
		}
		if(val.equals(Type.UNDEFINED)) {
			return Type.Any;
		}
		if (val instanceof String) {
			return Type.String;
		}
		if (val instanceof RString)
			return Type.String;
		if (val instanceof BigInteger)
			return Type.Int;
		if (val instanceof Double) {
			if(Double.isNaN((Double)val) || Double.isInfinite((Double)val)) {
				return Type.Int;
			}
			return Type.Real;
		}
		if (val instanceof UnitValue)
			return Type.Unit;
		if (val instanceof Boolean)
			return Type.Bool;

		if (val instanceof Expr.TemplateDef)
			return new Type.SignatureType((TemplateDef) val);

		if (val instanceof java.util.List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) val;
			if (!list.isEmpty()) {
				return new Type.ListType(typeFromValue(list.get(0)));
			} else {
				return new Type.ListType(Type.Any);
			}
		}
		if (val instanceof java.util.Set) {
			@SuppressWarnings("unchecked")
			Set<Object> set = (Set<Object>) val;
			if (!set.isEmpty()) {
				return new Type.SetType(typeFromValue(set.iterator().next()));
			} else {
				return new Type.SetType(Type.Any);
			}
		}
		if (val instanceof java.util.Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String,Object>) val;
			if (!map.isEmpty()) {
				return new Type.MapType(typeFromValue(map.values().iterator().next()));
			} else {
				return new Type.MapType(Type.Any);
			}
		}
		if (val instanceof Map.Entry) {
			return new Type.MapType(Type.Any);
		}
		if (val instanceof Template) {
			return new Type.SignatureType((TemplateDef) ((Template) val).def);
		}
		if (val instanceof Callee) {
			return new Type.SignatureType((TemplateDef) ((Callee) val).def);
		}
		if (val instanceof Pointer) {
			return new Type.RefType(((Pointer) val).type);
		}
		System.err.println("Type is " + val.getClass());
		return Type.None;
	}

	@SuppressWarnings("unchecked") String stringify(Result obj) {
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
		if (obj.value instanceof Map.Entry) {
			Map<String, Object> map = new HashMap<>();
			Map.Entry<String, Object> e = (Entry<String, Object>) obj.value;
			map.put(e.getKey(), e.getValue());
			return stringifyMap(map);
		}
		return obj.value.toString();
	}

	String stringifyType(Result obj) {
		if (obj == null || obj.value == null)
			return "Void";
		return obj.type.toString();
	}

	String unquote(String s) {
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

	String interpolate(String string) {
		StringBuilder sb = new StringBuilder();
		int ix1 = string.indexOf("#{");
		int ix2 = string.indexOf('}', ix1);
		if (ix1 >= 0 && ix2 > ix1) {
			String name = string.substring(ix1 + 2, ix2);
			String pre = string.substring(0, ix1);
			String post = string.substring(ix2 + 1);
			Expr expr = new Mu("-eval").parse(name);
			if (expr == null) {
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

	@SuppressWarnings("unchecked") boolean isTruthy(Object value, int flags) {
		
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		
		if (value == null) {
			if((flags & 1) != 0) {
				// null => false
				return false;
			} else {
				throw new InterpreterError("Criterion may not be nil");
			}
		}
		
		// fail => false
		if (value instanceof MuException) {
			if((flags & 2) != 0) {
				return false;
			} else {
				throw (MuException)value;
			}
		}
		
		// empty
		if (value instanceof String) {
			if((flags & 4) != 0) {
				String s = (String) value;
				return !s.isEmpty();
			}
		}
		
		if (value instanceof List) {
			if((flags & 4) != 0) {
				List<Object> list = (List<Object>)value;
				return !list.isEmpty();
			}
		}
		
		if (value instanceof Set) {
			if((flags & 4) != 0) {
				Set<Object> set = (Set<Object>)value;
				return !set.isEmpty();
			}
		}
		
		if (value instanceof Map) {
			if((flags & 4) != 0) {
				Map<String, Object> map = (Map<String, Object>)value;
				return !map.isEmpty();
			}
		}
		
		if(flags != 0) {
			return true;
		} else {
			throw new InterpreterError("Criterion must be Bool");
		}
	}

	public boolean isTruthy(Expr.Set criteria, Expr expr, boolean invert) {
		Result crit = null;
		int flags = 0;
		if(criteria != null ) {
			crit = evaluate(criteria);
			if(crit.value instanceof Set) {
				@SuppressWarnings("unchecked")
				Set<String> set = (Set<String>)crit.value;
				for(String elt : set) {
					switch(elt) {
					case "strict":
						flags = 0;
						break;
					case "empty":
						flags |= 4;
						break;
					case "null":
						flags |= 1;
						break;
					case "fail":
						flags |= 2;
						break;
					default:
						flags = 7;
					}
				}
			}
		} else {
			flags = 7;
		}
		Result condition = evaluate(expr);
		return invert ? !isTruthy(condition.value, flags) : isTruthy(condition.value, flags);
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

	Result callGetter(Property prop) {
		Block blk =  prop.get;
		Environment env = new Environment("get_" + prop.name, environment);
		env.define("$value", prop.var, false, true);
		return executeBlockReturn(blk.expressions, env);
	}
	
	void callSetter(Property prop, Result value) {
		Block blk =  prop.set;
		Environment env = new Environment("set_" + prop.name, environment);
		env.define("$value", prop.var, true, true);
		env.define("$new", value, true, false);
		executeBlockReturn(blk.expressions, env);
		prop.var.value = env.get("$value").value;
	}

}
