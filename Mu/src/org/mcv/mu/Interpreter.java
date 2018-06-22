package org.mcv.mu;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Future;

import org.mcv.math.BigInteger;
import org.mcv.mu.Expr.Assign;
import org.mcv.mu.Expr.Block;
import org.mcv.mu.Expr.Seq;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Expr.Variable;
import org.mcv.mu.Parser.ParserError;
import org.mcv.uom.UnitValue;
import org.mcv.utils.FileUtils;

public class Interpreter {

	Environment main;
	Environment environment;
	Visitors visitors;
	Mu handler;
	AddableURLClassLoader classLoader;
	Operators ops = new Operators();
	Operators prefixOps = new Operators();
	Operators postfixOps = new Operators();
	Stack<String> funstk = new Stack<>();
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
		defineOperators();
	}

	private void defineOperators() {
		ops.makeOperator(new Signature("and", Type.A, Type.A, Type.A), "&");
		ops.makeOperator(new Signature("or", Type.A, Type.A, Type.A), "|");
		ops.makeOperator(new Signature("xor", Type.A, Type.A, Type.A), "⊕", "⊻", "xor");

		ops.makeOperator(new Signature("in", Type.C, Type.A, Type.B), "in", "∈");

		ops.makeOperator(new Signature("as", Type.B, Type.A, Type.B), "as");
		ops.makeOperator(new Signature("lt", Type.Bool, Type.A, Type.A), "<");
		ops.makeOperator(new Signature("gt", Type.Bool, Type.A, Type.A), ">");
		ops.makeOperator(new Signature("le", Type.Bool, Type.A, Type.A), "<=", "≤");
		ops.makeOperator(new Signature("ge", Type.Bool, Type.A, Type.A), ">=", "≥");
		ops.makeOperator(new Signature("eq", Type.Bool, Type.A, Type.A), "=");
		ops.makeOperator(new Signature("eqeq", Type.Bool, Type.A, Type.A), "==");
		ops.makeOperator(new Signature("neq", Type.Bool, Type.A, Type.A), "~=", "¬=");
		ops.makeOperator(new Signature("neqeq", Type.Bool, Type.A, Type.A), "~==", "¬==");
		ops.makeOperator(new Signature("plus", Type.A, Type.A, Type.A), "+");
		ops.makeOperator(new Signature("minus", Type.A, Type.A, Type.A), "-");
		ops.makeOperator(new Signature("mul", Type.A, Type.A, Type.A), "*", "×");
		ops.makeOperator(new Signature("div", Type.A, Type.A, Type.A), "/", "÷");
		ops.makeOperator(new Signature("rem", Type.A, Type.A, Type.A), "%");
		ops.makeOperator(new Signature("pow", Type.A, Type.A, Type.B), "^");
		ops.makeOperator(new Signature("lsh", Type.A, Type.A, Type.A), "<<");
		ops.makeOperator(new Signature("rsh", Type.A, Type.A, Type.A), ">>");

		prefixOps.makeOperator(new Signature("neg", Type.A, Type.A), "-");
		prefixOps.makeOperator(new Signature("id", Type.A, Type.A), "+");
		prefixOps.makeOperator(new Signature("not", Type.B, Type.A), "~", "¬");
		prefixOps.makeOperator(new Signature("inc", Type.A, Type.A), "++");
		prefixOps.makeOperator(new Signature("dec", Type.A, Type.A), "--");
		prefixOps.makeOperator(new Signature("abs", Type.A, Type.A), "abs", "\\");
		prefixOps.makeOperator(new Signature("ref", new Type.RefType(Type.Any), Type.A), "@");
		prefixOps.makeOperator(new Signature("spread", Type.A, Type.A), "*");
		prefixOps.makeOperator(new Signature("sqrt", Type.A, Type.A), "sqrt", "√");

		postfixOps.makeOperator(new Signature("inc", Type.A, Type.A), "++");
		postfixOps.makeOperator(new Signature("dec", Type.A, Type.A), "--");
		postfixOps.makeOperator(new Signature("fac", Type.A, Type.A), "!");
		postfixOps.makeOperator(new Signature("deref", Type.A, new Type.RefType(Type.Any)), "↑", ":^");
	}

	public Result evalFile(File path, Environment env) {
		return handler.evalFile(path, env);
	}

	// DEBUG
	List<Expr> gexpressions;
	int currentLine = 0;

	Result interpret(List<Expr> expressions) {
		// DEBUG
		gexpressions = expressions;
		stdMacros();
		Result result = new Result(null, Type.Void);
		for (Expr current : expressions) {
			currentLine = current.line;
			updateMacros(currentLine, funstk.peek());
			try {
				boolean isImport = false;
				if (current instanceof Expr.Import) {
					isImport = true;
				}
				result = evaluate(current);
				if (result.type.equals(Type.Exception)) {
					throw (MuException) result.value;
				}
				if (isImport) {
					@SuppressWarnings("unchecked")
					ListMap<Object> listmap = (ListMap<Object>) result.value;
					for (Entry<String, Object> entry : listmap.entrySet()) {
						environment.define(entry.getKey(),
								new Result(entry.getValue(), Interpreter.typeFromValue(entry.getValue())), false, true);
					}
				}
			} catch (ReturnJump ret) {
				System.out.println("Program exited with: " + stringify(ret.value));
				return ret.value;
			} catch (MuException me) {
				if (environment.get("$this") != null) {
					try {
						Template tmpl = (Template) environment.get("$this").value;
						if (tmpl.errors != null && !tmpl.errors.isEmpty()) {
							Stack<Block> handlers = tmpl.errors;
							environment.define("$exception", new Result(me, Type.Exception), false, false);
							for (Block block : handlers) {
								executeBlock(block.expressions, environment);
							}
							continue;
						}
					} catch (ClassCastException e) {
						// for now
					}
				}
				me.expr = current;
				me.line = current.line;
				handler.error(me);
				return new Result(null, Type.Void);
			} catch (InterpreterError e) {
				e.expr = current;
				e.line = current.line;
				handler.error(e);
				return new Result(null, Type.Void);
			} catch (Exception e) {
				InterpreterError ie = new InterpreterError(e);
				ie.expr = current;
				ie.line = current.line;
				handler.error(ie);
				return new Result(null, Type.Void);
			}
		}
		return result;
	}

	Result evaluate(Expr expr) {
		Result res = expr.accept(visitors);
		if (res != null && res.value instanceof Expr) {
			return ((Expr) res.value).accept(visitors);
		}
		return res;
	}

	private void stdMacros() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd");
		SimpleDateFormat sdtf = new SimpleDateFormat("HH:mm:ss");
		// $line
		environment.define("$line", new Result(BigInteger.ZERO, Type.Int), true, true);
		// $file
		@SuppressWarnings("unchecked")
		File path = new File((String) ((ListMap<Object>) environment.get("system").value).get("currentFile"));
		environment.define("$file", new Result(path.getAbsolutePath(), Type.String), false, true);
		String funcname = FileUtils.getFileNameWithoutExtension(path);
		funstk.push(funcname);
		// $func
		environment.define("$func", new Result(funcname, Type.String), true, true);
		// $date
		environment.define("$date", new Result(sdf.format(new Date()), Type.String), true, true);
		// $time
		environment.define("$time", new Result(sdtf.format(new Date()), Type.String), true, true);
	}

	private void updateMacros(int line, String func) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd");
		SimpleDateFormat sdtf = new SimpleDateFormat("HH:mm:ss");
		// $line
		environment.define("$line", new Result(BigInteger.valueOf(line), Type.Int), true, true);
		// $func
		environment.define("$func", new Result(func, Type.String), true, true);
		// $date
		environment.define("$date", new Result(sdf.format(new Date()), Type.String), true, true);
		// $time
		environment.define("$time", new Result(sdtf.format(new Date()), Type.String), true, true);
	}

	/* invoke binary operator */
	Result invoke(String op, Result left, Result right, Signature sig) {

		Type type = Type.evaluate(left.type, this.environment);
		Object leftVal = left.value;
		Object rightVal = right.value;
		Object func = type.lookup(op, sig);

		if (func == null) {
			throw new MuException("Func '%s' not found for types %s and %s", op, left.type, right.type);
		}

		if (func instanceof Method) {
			Method method = (Method) func;
			try {
				return new Result(method.invoke(null, leftVal, rightVal), sig.returnType);
			} catch (IllegalArgumentException iae) {
				throw new MuException("Func '%s' not found for types %s and %s", op, left.type, right.type);
			} catch (InvocationTargetException ite) {
				throw new MuException(new Result(ite.getCause(), Type.Exception));
			} catch (Exception e) {
				if (e instanceof InterpreterError) {
					throw (InterpreterError) e;
				}
				throw new InterpreterError("No such operator: %s %s %s", left.type, op, right.type);
			}
		} else if (func instanceof Template) {
			Template tmpl = (Template) func;
			List<Expr> exprs = new ArrayList<>();
			exprs.add(new Expr.Literal(currentLine, left.value));
			exprs.add(new Expr.Literal(currentLine, right.value));
			Expr.Map args = new Expr.Map(currentLine, exprs);
			return Template.call(tmpl, this, args);
		} else {
			throw new InterpreterError("No operator: %s %s %s", left.type, op, right.type);
		}
	}

	/* invoke unary operator */
	Result invoke(String op, Signature sig, Result arg) {

		Object func = arg.type.lookup(op, sig);

		if (func == null) {
			throw new InterpreterError("Func " + op + " not found for type " + arg.type);
		}

		if (func instanceof Method) {
			Method method = (Method) func;
			try {
				return new Result(method.invoke(null, arg.value), sig.returnType);
			} catch (Exception e) {
				throw new InterpreterError("Error invoking operator: %s %s", op, arg.type);
			}
		} else if (func instanceof Template) {
			Template tmpl = (Template) func;
			List<Expr> exprs = new ArrayList<>();
			exprs.add(new Expr.Literal(currentLine, arg.value));
			Expr.Map args = new Expr.Map(currentLine, exprs);
			return Template.call(tmpl, this, args);
		} else {
			throw new InterpreterError("No native operator: %s %s", op, arg.type);
		}
	}

	static class Source {
		Iterator<Object> it = null;
		Map<String, Object> map = null;
		Result value;

		@SuppressWarnings("unchecked")
		public Source(int n, Result value) {
			this.value = value;
			if (n > 1) {
				if (value.value instanceof List) {
					it = ((List<Object>) value.value).iterator();
				}
				if (value.value instanceof Map) {
					map = (Map<String, Object>) value.value;
				}
			}
		}

		public Result next(String key) {
			if (it != null) {
				if (it.hasNext()) {
					Object next = it.next();
					return new Result(next, ((Type.ListType) value.type).eltType);
				}
				return new Result(null, Type.Void);
			}
			if (map != null) {
				if (map.containsKey(key)) {
					Object val = map.get(key);
					return new Result(val, ((Type.MapType) value.type).valType);
				}
				return new Result(null, Type.Void);
			}
			return value;
		}
	}

	Result assign(Assign expr, Result value) {
		Result result = null;
		Source src = new Source(expr.var.size(), value);
		for (String var : expr.var) {
			Result curr = environment.get(var);
			if (curr == null) {
				throw new InterpreterError("Undefined variable %s", var);
			}
			if (curr.value instanceof Property) {
				callSetter((Property) curr.value, src.next(var));
			}
			result = environment.assign(var, src.next(var));
		}
		return result;
	}

	Result assign(Variable expr, Result value) {
		Result curr = environment.get(expr.name);
		if (curr == null) {
			throw new InterpreterError("Undefined variable", expr.name);
		}
		if (curr.value instanceof Property) {
			callSetter((Property) curr.value, value);
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
		} catch (BreakJump brk) {
			throw brk;
		} catch (ContinueJump cnt) {
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
		} catch (BreakJump brk) {
			// ignore
		} catch (ContinueJump cnt) {
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
				boolean spread = spread(expr);
				if (spread) {
					listmap.putAll((ListMap<?>) value.value);
					valType = Type.unite(valType, value.type);
				} else {
					listmap.put(null, value.value);
					valType = Type.unite(valType, value.type);
				}
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
			return Template.call((Callee) func.value, this, args);
		} else if (func.value instanceof Template) {
			return Template.call((Template) func.value, this, args);
		} else {
			throw new InterpreterError("Cannot call object %s", func.value);
		}
	}

	@SuppressWarnings("unchecked")
	Result subscript(Result listMap, Seq index, Expr value) {

		if (value != null) {
			Object val = evaluate(value).value;
			/* setter */
			if (listMap.value instanceof Map) {
				String key = null;
				if (index.exprs.get(0) instanceof Expr.Variable) {
					key = ((Expr.Variable) index.exprs.get(0)).name;
				} else {
					Result sub = evaluate(index.exprs.get(0));
					key = (String) sub.value;
				}
				Map<String, Object> map = (Map<String, Object>) listMap.value;
				if (map.containsKey(key)) {
					map.put(key, val);
				} else {
					throw new InterpreterError("No such key: %s", key);
				}
			}
			if (listMap.value instanceof List) {
				Result sub = evaluate(index.exprs.get(0));
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
				String key = null;
				if (index.exprs.get(0) instanceof Expr.Variable) {
					key = ((Expr.Variable) index.exprs.get(0)).name;
				} else {
					Result sub = evaluate(index.exprs.get(0));
					key = (String) sub.value;
				}
				Map<String, Object> map = (Map<String, Object>) listMap.value;
				if (map.containsKey(key)) {
					Object val = map.get(key);
					return new Result(val, typeFromValue(val));
				} else {
					throw new InterpreterError("No such key: %s", key);
				}
			}
			if (listMap.value instanceof List) {
				Result sub = evaluate(index.exprs.get(0));
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

	@SuppressWarnings("unchecked")
	Result dot(Result listMap, String key) {
		if (listMap.value instanceof Map) {
			Object value = ((Map<String, Object>) listMap.value).get(key);
			return new Result(value, typeFromValue(value));
		}
		if (listMap.value instanceof Map.Entry) {
			Object value = ((Map.Entry<?, ?>) listMap.value).getValue();
			return new Result(value, typeFromValue(value));
		}
		if (listMap.value instanceof Type) {
			Map<Signature, Object> interfaces = ((Type) listMap.value).interfaces();
			for (Entry<Signature, Object> entry : interfaces.entrySet()) {
				if (entry.getKey().name.equals(key)) {
					Object value = entry.getValue();
					if (value instanceof Method) {
						value = JavaHelper.calleeFromMethod(this, (Method) value, null);
					}
					return new Result(value, typeFromValue(value));
				}
			}
		}
		if (listMap.value instanceof Template) {
			Template tmpl = (Template) listMap.value;
			Result value = tmpl.closure.get(key);
			if (value == null && tmpl.attributes.containsKey("jvm")) {
				value = tmpl.closure.get("__jvm_" + key);
			}
			if (value == null) {
				throw new InterpreterError("Interface %s not found", key);
			}
			if (value.value instanceof Property) {
				return callGetter((Property) value.value);
			}
			return value;
		}
		if (listMap.value instanceof Callee) {
			Callee callee = (Callee) listMap.value;
			Result value = callee.closure.get(key);
			if (value == null && callee.parent.attributes.containsKey("jvm")) {
				value = callee.closure.get("__jvm_" + key);
				((Callee) value.value).javaObject = callee.javaObject;
			}
			/* not found in current object, try mixins */
			if (value == null) {
				if (callee.mixins != null) {
					for (Mixin mixin : callee.mixins) {
						key = mixin.where.getOrDefault(key, key);
						value = mixin.object.closure.get(key);
						if (value != null) {
							break;
						}
					}
				}
			}
			if (value == null) {
				throw new InterpreterError("Interface %s not found", key);
			}

			/* Getter */
			if (value.value instanceof Property) {
				/* it's a getter */
				return callGetter((Property) value.value);
			}
			return value;
		}
		return new Result(null, Type.Void);
	}

	@SuppressWarnings("unchecked")
	Result setdot(Result listMap, String key, Object value) {
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
			if (rs == null) {
				throw new InterpreterError("Undefined field %s", key);
			}
			rs.value = value;
		}
		if (listMap.value instanceof Callee) {
			// call setter
			Callee callee = (Callee) listMap.value;
			Result rs = callee.closure.get(key);
			if (rs == null) {
				throw new InterpreterError("Undefined field %s", key);
			}
			if (rs.value instanceof Property) {
				callSetter((Property) rs.value, new Result(value, typeFromValue(value)));
			}
			rs.value = value;
		}
		return new Result(null, Type.Void);
	}

	/* UTIL */

	static Type typeFromClass(Class<?> clz) {
		if (clz.equals(Void.class) || clz.equals(void.class))
			return Type.Void;
		if (clz.equals(Object.class))
			return Type.Any;
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
		if (clz.equals(UnitValue.class)) {
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
		if (clz.isAssignableFrom(Type.class)) {
			return Type.Type;
		}
		// System.err.println("Type is " + clz);`
		return new Type(clz.getSimpleName());
	}

	static Type typeFromValue(Object val) {

		/* We have char, string, int, real, bool */
		if (val == null)
			return Type.Void;
		if (val instanceof Type)
			return Type.Type;
		if (val instanceof Integer)
			return Type.Char;
		if (val instanceof Symbol) {
			return Type.Any;
		}
		if (val.equals(Type.UNDEFINED)) {
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
			if (Double.isNaN((Double) val) || Double.isInfinite((Double) val)) {
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
			Map<String, Object> map = (Map<String, Object>) val;
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
			return new Type.SignatureType((TemplateDef) ((Callee) val).parent.def);
		}
		if (val instanceof Pointer) {
			return new Type.RefType(((Pointer) val).type);
		}
		if (val instanceof Future) {
			return Type.Future;
		}
		System.err.println("Type is " + val.getClass());
		return Type.None;
	}

	@SuppressWarnings("unchecked")
	String stringify(Result obj) {
		if (obj == null || obj.value == null)
			return "nil";
		if (obj.value instanceof Integer) {
			return "'" + new String(Character.toChars((Integer) obj.value)) + "'";
		}
		if (obj.value instanceof BigInteger) {
			return obj.value.toString();
		}
		if (obj.value instanceof Future) {
			try {
				Future<?> future = (Future<?>) obj.value;
				if (future.isDone())
					return "Future (" + future.get() + ")";
				return "Future (unresolved)";
			} catch (Exception e) {

			}
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
		if (obj.value instanceof Callee) {
			Callee callee = (Callee) obj.value;
			if (callee.javaObject != null)
				return callee.javaObject.toString();
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

	@SuppressWarnings("unchecked")
	boolean isTruthy(Object value, int flags) {

		if (value instanceof Boolean) {
			return (Boolean) value;
		}

		if (value == null) {
			if ((flags & 1) != 0) {
				// null => false
				return false;
			} else {
				throw new MuException("Criterion may not be nil");
			}
		}

		// fail => false
		if (value instanceof MuException) {
			if ((flags & 2) != 0) {
				return false;
			} else {
				throw (MuException) value;
			}
		}

		// empty
		if (value instanceof String) {
			if ((flags & 4) != 0) {
				String s = (String) value;
				return !s.isEmpty();
			}
		}

		if (value instanceof List) {
			if ((flags & 4) != 0) {
				List<Object> list = (List<Object>) value;
				return !list.isEmpty();
			}
		}

		if (value instanceof Set) {
			if ((flags & 4) != 0) {
				Set<Object> set = (Set<Object>) value;
				return !set.isEmpty();
			}
		}

		if (value instanceof Map) {
			if ((flags & 4) != 0) {
				Map<String, Object> map = (Map<String, Object>) value;
				return !map.isEmpty();
			}
		}

		if (value instanceof Future) {
			if ((flags & 2) != 0) {
				Future<?> future = (Future<?>)value;
				return future.isDone();
			}
		}

		if (flags != 0) {
			return true;
		} else {
			throw new MuException("Criterion must be Bool");
		}
	}

	public boolean isTruthy(Expr.Set criteria, Expr expr, boolean invert) {
		Result crit = null;
		int flags = 0;
		if (criteria != null) {
			crit = evaluate(criteria);
			if (crit.value instanceof Set) {
				@SuppressWarnings("unchecked")
				Set<String> set = (Set<String>) crit.value;
				for (String elt : set) {
					switch (elt) {
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
		Block blk = prop.get;
		Environment env = new Environment("get_" + prop.name, environment);
		env.define("$value", prop.var, false, true);
		return executeBlockReturn(blk.expressions, env);
	}

	void callSetter(Property prop, Result value) {
		Block blk = prop.set;
		Environment env = new Environment("set_" + prop.name, environment);
		env.define("$value", prop.var, true, true);
		env.define("$new", value, true, false);
		executeBlockReturn(blk.expressions, env);
		prop.var.value = env.get("$value").value;
	}

	public boolean spread(Expr expr) {
		if (expr instanceof Expr.Unary) {
			Expr.Unary u = (Expr.Unary) expr;
			return u.operator.equals("*");
		}
		return false;
	}

}
