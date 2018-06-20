package org.mcv.mu;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mcv.math.BigInteger;
import org.mcv.mu.Dependency.Coords;
import org.mcv.mu.Environment.TableEntry;
import org.mcv.mu.Expr.Assign;
import org.mcv.mu.Expr.Call;
import org.mcv.mu.Expr.Dot;
import org.mcv.mu.Expr.For;
import org.mcv.mu.Expr.Mapping;
import org.mcv.mu.Expr.Seq;
import org.mcv.mu.Expr.Subscript;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Expr.TypeDef;
import org.mcv.mu.Expr.UnitDefExpr;
import org.mcv.mu.Expr.Val;
import org.mcv.mu.Expr.Var;
import org.mcv.mu.Interpreter.InterpreterError;
import org.mcv.mu.Type.EnumType;
import org.mcv.mu.Type.IntersectionType;
import org.mcv.mu.Type.ListEnum;
import org.mcv.mu.Type.ListType;
import org.mcv.mu.Type.MapType;
import org.mcv.mu.Type.RefType;
import org.mcv.mu.Type.SetEnum;
import org.mcv.mu.Type.SetType;
import org.mcv.mu.Type.SignatureType;
import org.mcv.mu.Type.UnionType;
import org.mcv.uom.UnitRepo;
import org.mcv.uom.UnitValue;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Visitors implements Expr.Visitor {

	Interpreter mu;

	public Visitors(Interpreter mu) {
		this.mu = mu;
	}

	/* VISITORS */

	@SuppressWarnings("unchecked")
	@Override
	public Result visitVarDef(Expr.Var stmt) {
		Result val = null;
		if (stmt.initializer != null) {
			val = mu.evaluate(stmt.initializer);
		}
		if (val == null) {
			throw new InterpreterError("Missing initializer for var(s) %s", stmt.names);
		}
		if (val.value instanceof Type) {
			val.type = (Type) val.value;
			val.value = Type.UNDEFINED;
		}

		Interpreter.Source src = new Interpreter.Source(stmt.names.size(), val);

		/* mixin */
		if (stmt.attributes.isMixin()) {
			ListMap<String> where = new ListMap<>();
			if (stmt.where != null) {
				where = (ListMap<String>) mu.evaluate(stmt.where).value;
			}
			try {
				Callee callee = (Callee) (mu.environment.get("$this").value);
				if (callee != null) {
					callee.mixins.add(new Mixin((Callee) val.value, where));
				}
			} catch (Exception e) {
				throw new InterpreterError("Failed to load mixin: %s (%s)", val.value, e);
			}
		}

		for (String name : stmt.names) {

			Result value = src.next(name);

			if (stmt.attributes.isOwn()) {
				if (stmt.attributes.isProp()) {
					if (mu.environment.get(name) == null) {
						mu.environment.topLevel().define(name, new Result(new Property(stmt.line, true, name, value), value.type), true, true);
					} else {
						val = mu.environment.get(name);
					}
				} else {
					if (mu.environment.get(name) == null) {
						mu.environment.topLevel().define(name, value, true, stmt.attributes.isPublic());
					} else {
						val = mu.environment.get(name);
					}
				}
			} else if (stmt.attributes.isProp()) {
				mu.environment.topLevel().define(name, new Result(new Property(stmt.line, true, name, value), value.type), true, true);
			} else {
				mu.environment.define(name, value, true, stmt.attributes.isPublic());
			}
		}
		return val;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Result visitValDef(Expr.Val stmt) {
		Result val = null;
		if (stmt.initializer == null) {
			throw new InterpreterError("Val(s) %s must be initialized!", stmt.names);
		}
		val = mu.evaluate(stmt.initializer);
		Interpreter.Source src = new Interpreter.Source(stmt.names.size(), val);

		/* mixin */
		if (stmt.attributes.isMixin()) {
			ListMap<String> where = new ListMap<>();
			if (stmt.where != null) {
				where = (ListMap<String>) mu.evaluate(stmt.where).value;
			}
			try {
				Callee callee = (Callee) (mu.environment.get("$this").value);
				if (callee != null) {
					callee.mixins.add(new Mixin((Callee) val.value, where));
				}
			} catch (Exception e) {
				throw new InterpreterError("Failed to load mixin: %s (%s)", val.value, e);
			}
		}

		for (String name : stmt.names) {

			Result value = src.next(name);

			if (stmt.attributes.isOwn()) {
				if (stmt.attributes.isProp()) {
					if (mu.environment.get(name) == null) {
						mu.environment.topLevel().define(name, new Result(new Property(stmt.line, false, name, value), value.type), false, true);
					} else {
						val = mu.environment.get(name);
					}
				} else {
					if (mu.environment.get(name) == null) {
						mu.environment.topLevel().define(name, value, false, stmt.attributes.isPublic());
					} else {
						val = mu.environment.get(name);
					}
				}
			}
			if (stmt.attributes.isProp()) {
				mu.environment.topLevel().define(name, new Result(new Property(stmt.line, false, name, value), value.type), false, true);
			} else {
				mu.environment.define(name, value, false, stmt.attributes.isPublic());
			}
		}
		return val;
	}

	@Override
	public Result visitTemplateDef(Expr.TemplateDef stmt) {
		Template tmpl = new Template(stmt, mu.environment.capture(stmt.name));
		Result res = mu.environment.topLevel().define(stmt.name, new Result(tmpl, new Type.SignatureType(stmt)),
				false, true);
		tmpl.closure.define(stmt.name, new Result(tmpl, new Type.SignatureType(stmt)), false, true);

		if(stmt.attributes.containsKey("jvm")) {
			if(stmt.kind.equals("class")) {
				JavaHelper.evalClass(mu, tmpl);
			}
			return res;
		}

		if(stmt.attributes.containsKey("op")) {
			if(stmt.kind.equals("fun")) {
				String op = (String) stmt.attributes.get("op");
				Type type = Type.evaluate(tmpl.params.types().get(0), mu.environment);
				Signature sig = new Signature(tmpl.def);
				Map<Signature, Object> intface = type.interfaces();
				intface.put(sig, tmpl);
				Type.interfaces.put(type.name, intface);
				if(tmpl.params.toMap().size() == 2) {
					mu.ops.makeOperator(sig, op);
				} else {
					if(tmpl.params.listMap.get(0).id.equals("left"))
						mu.postfixOps.makeOperator(sig, op);
					if(tmpl.params.listMap.get(0).id.equals("right"))
						mu.prefixOps.makeOperator(sig, op);
				}
			}
		}

		// define all declarations inside templatedef interface
		for (Expr expr : tmpl.body.expressions) {

			/* non-local templdefs */
			if (expr instanceof Expr.TemplateDef) {
				Expr.TemplateDef def = (Expr.TemplateDef) expr;
				if (def.attributes.isPublic()) {
					List<Expr> list = new ArrayList<>();
					list.add(expr);
					Result result = mu.executeBlockReturn(list, tmpl.closure);
					tmpl.closure.define(def.name, result, false, true);
				}
			}
			
			/* non-local typedefs */
			if (expr instanceof Expr.TypeDef) {
				TypeDef def = (TypeDef) expr;
				if (def.attributes.isPublic()) {
					tmpl.closure.define(def.name, new Result(def.literal, Type.Type), false, true);
					// define SetEnum or ListEnum literals
					if (def.type instanceof Type.ListEnum) {
						for (Object val : ((Type.ListEnum) def.type).values) {
							tmpl.closure.define(val.toString(), new Result(val.toString(), def.type), false, true);
						}
					}
					if (def.type instanceof Type.SetEnum) {
						for (Object val : ((Type.SetEnum) def.type).values) {
							tmpl.closure.define(val.toString(), new Result(val.toString(), def.type), false, true);
						}
					}
				}
			}

			/* own [class] immutable properties */
			if (expr instanceof Expr.Val) {
				Val def = (Val) expr;
				if (def.attributes.isOwn()) {
					for (String name : def.names) {
						if (tmpl.closure.get(name) != null) {
							throw new InterpreterError("Name %s already defined.", name);

						}
					}
					List<Expr> list = new ArrayList<>();
					list.add(expr);
					mu.executeBlockReturn(list, tmpl.closure);
				}
			}

			/* own [class] mutable properties */
			if (expr instanceof Expr.Var) {
				Var def = (Var) expr;
				// getter & setter
				if (def.attributes.isOwn()) {
					for (String name : def.names) {
						if (tmpl.closure.get(name) != null) {
							throw new InterpreterError("Name %s already defined.", name);
						}
					}
					List<Expr> list = new ArrayList<>();
					list.add(expr);
					mu.executeBlockReturn(list, tmpl.closure);
				}
			}
		}
		return res;
	}

	@Override
	public Result visitAopExpr(Expr.Aop stmt) {
		Result callable = mu.evaluate(stmt.callable);
		if (callable.value instanceof Template) {
			Template tmpl = (Template) callable.value;
			switch (stmt.name.toLowerCase()) {
			case "around":
				if (tmpl.around != null) {
					System.err.println("Overwriting existing around block...");
				}
				tmpl.around = stmt.block;
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
	public Result visitGetterExpr(Expr.Getter stmt) {
		for (Token var : stmt.variables) {
			Result variable = mu.environment.get(var.lexeme);
			if (variable == null) {
				throw new InterpreterError("Undefined variable %s", var.lexeme);
			}
			if (variable.value instanceof Property) {
				Property prop = (Property) variable.value;
				prop.get = stmt.block;
			}
		}
		return new Result(null, Type.Void);
	}

	@Override
	public Result visitSetterExpr(Expr.Setter stmt) {
		for (Token var : stmt.variables) {
			Result variable = mu.environment.get(var.lexeme);
			if (variable == null) {
				throw new InterpreterError("Undefined variable %s", var.lexeme);
			}
			if (variable.value instanceof Property) {
				Property prop = (Property) variable.value;
				prop.set = stmt.block;
			}
		}
		return new Result(null, Type.Void);
	}

	@Override
	public Result visitImportExpr(Expr.Import expr) {
		Map<String, Dependency> deps = getDeps();
		ListMap<Template> result = new ListMap<>();
		if (expr.attributes.containsKey("jvm")) {
			File jarFile = null;
			if(expr.repo != null) {
				jarFile = MavenHelper.mavenImport(expr, deps, mu);
				if(jarFile != null) {
					storeDeps(deps);
				}
			}
			Result imp = mu.evaluate(expr.imports);
			if(imp.value instanceof ListMap) {
				@SuppressWarnings("unchecked")
				ListMap<Object> imports = (ListMap<Object>)imp.value;
				for(Entry<String, Object> entry : imports.entrySet()) {
					String key = entry.getKey();
					String value = (String)entry.getValue();
					if(entry.getKey().startsWith(ListMap.gensym.prefix)) {
						key = value;
					}
					if(jarFile != null) {
						String regex = value.replace("*", ".*");
						List<Class<?>> classes = JavaHelper.handleJar(mu.classLoader, jarFile, regex);
						for(Class<?> clazz : classes) {
							String k2 = key;
							if(key.contains("*")) {
								k2 = clazz.getSimpleName();
							}
							result.put(k2, JavaHelper.classToTemplate(mu, clazz.getSimpleName(), clazz));
						}
					}
				}
			}
		} else {
			Result imp = mu.evaluate(expr.imports);
			if(imp.value instanceof ListMap) {
				@SuppressWarnings("unchecked")
				ListMap<Object> imports = (ListMap<Object>)imp.value;
				for(Entry<String, Object> entry : imports.entrySet()) {
					String key = entry.getKey();
					String value = (String)entry.getValue();
					if(entry.getKey().startsWith(ListMap.gensym.prefix)) {
						key = strip(value);
					}
					@SuppressWarnings("unchecked")
					File cd = new File((String)((ListMap<Object>)mu.environment.get("system").value).get("currentDirectory"));
					File muFile = new File(cd, value);
					if(expr.repo != null) {
						Coords coords = new Coords(expr.repo);
						if(deps.get(coords.toKey()) == null) {
							deps.put(coords.toKey(), new Dependency(coords));
						}
						if(coords.version == null) {
							coords.version = deps.get(coords.toKey()).coords.version;
						}
						if(coords.version == null) {
							coords.version = "master";
						}
						muFile = GitHelper.gitImport(coords.toKey(), coords.version, value, deps);
						storeDeps(deps);
					}
					if(muFile.exists()) {
						Environment env = new Environment("import");
						result.put(key, (Template)mu.evalFile(muFile, env).value);
					}
				}
			}
		}
		return new Result(result, new MapType(Type.Type));
	}

	private static String strip(String name) {
		int ix0 = name.lastIndexOf('/') + 1;
		int ix1 = name.lastIndexOf('.');
		return name.substring(ix0, ix1);
	}

	Map<String, Dependency> getDeps() {
		try {
			@SuppressWarnings("unchecked")
			String path = (String) ((ListMap<Object>) mu.environment.get("system").value).get("currentFile");
			if (path != null) {
				File f = new File(path + ".deps");
				ObjectMapper mapper = new ObjectMapper();
				mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
				mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
				return mapper.readValue(f, new TypeReference<Map<String, Dependency>>() {
				});
			}
		} catch (Exception e) {
			//System.err.println(e);
		}
		return new HashMap<>();
	}

	void storeDeps(Map<String, Dependency> deps) {
		try {
			@SuppressWarnings("unchecked")
			String path = (String) ((ListMap<Object>) mu.environment.get("system").value).get("currentFile");
			if (path != null) {
				File f = new File(path + ".deps");
				ObjectMapper mapper = new ObjectMapper();
				mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
				mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
				mapper.writerWithDefaultPrettyPrinter().writeValue(f, deps);
			}
		} catch (Exception e) {
			System.err.println(e);
			/**/
		}
	}

	@Override
	public Result visitTypeDefExpr(Expr.TypeDef expr) {
		if (expr.name != null) {
			mu.environment.define(expr.name, new Result(expr.literal, Type.Type), false,
					expr.attributes.isPublic());

			if (expr.literal instanceof Type.ListEnum) {
				for (Object val : ((Type.ListEnum) expr.literal).values) {
					if (val instanceof BigInteger) {
						// Int subrange
						continue;
					}
					if (val instanceof Integer) {
						// Char subrange
						continue;
					}
					if (val instanceof String) {
						mu.environment.define((String) val, new Result(new Symbol((String) val), expr.literal), false,
								expr.attributes.isPublic());
					}
					if (val instanceof Symbol) {
						mu.environment.define(val.toString(), new Result(val, expr.literal), false,
								expr.attributes.isPublic());
					}
				}
			}
			if (expr.literal instanceof Type.SetEnum) {
				for (Object val : ((Type.SetEnum) expr.literal).values) {
					if (val instanceof String)
						val = new Symbol((String) val);
					mu.environment.define(val.toString(), new Result(val, expr.literal), false,
							expr.attributes.isPublic());
				}
			}
		}
		return new Result(null, Type.Void);
	}

	@Override
	public Result visitPrintExpr(Expr.Print stmt) {
		Expr blk = stmt.expression;
		System.out.println(mu.unquote(mu.stringify(mu.evaluate(blk))));
		return new Result(null, Type.Void);
	}

	@Override
	public Result visitAssertExpr(Expr.Assert assrt) {
		if (!mu.isTruthy(assrt.criteria, assrt.expression, false)) {
			System.err.println(mu.interpolate(assrt.msg));
			System.err.flush();
		}
		return new Result(null, Type.Void);
	}

	@Override
	public Result visitBinaryExpr(Expr.Binary expr) {
		Result leftOrg = mu.evaluate(expr.left);
		if (leftOrg.value instanceof UnitValue) {
			leftOrg.type = Type.Unit;
			((UnitValue) leftOrg.value).resolve();
		}
		Result left = leftOrg;
		if(left.type instanceof UnionType || left.type instanceof IntersectionType) {
			left.type = Interpreter.typeFromValue(left.value);
		}
		
		Signature sig = mu.ops.getFunction(expr.operator, left.type);
		if(!sig.paramTypes.get(0).matches(left.type)) {
			left = convert(leftOrg, sig.paramTypes.get(0));
		}
			
		/* START short circuit */
		switch (expr.operator) {
		case "&":
			/* String concatenation and logical AND */
			if (left.value instanceof String || left.value instanceof BigInteger) {
				break;
			}
			/* Short circuit AND */
			if (mu.isTruthy(null, expr.left, false)) {
				return mu.evaluate(expr.right);
			} else {
				return left;
			}
		case "|":
			/* Logical OR */
			if (left.value instanceof BigInteger) {
				break;
			}
			/* Short circuit OR */
			if (mu.isTruthy(null, expr.left, false)) {
				return left;
			} else {
				return mu.evaluate(expr.right);
			}
		default:
			break;
		}
		/* END short circuit */

		Result rightOrg = mu.evaluate(expr.right);
		if (rightOrg.value instanceof UnitValue) {
			rightOrg.type = Type.Unit;
			((UnitValue) rightOrg.value).resolve();
		}
		Result right = rightOrg;

		if(expr.operator.equals("as")) {
			/* casting operator */
			// expr AS type
			Type target = (Type)right.value;
			if(!leftOrg.type.equals(target)) {
				left = convert(leftOrg, target);
				return left;
			} else {
				return new Result(left.value, target);
			}
		}

		sig = mu.ops.getFunction(expr.operator, left.type, right.type);
		try {
			if(!sig.paramTypes.get(1).matches(right.type)) {
				right = convert(rightOrg, sig.paramTypes.get(1));
			}
		} catch(MuException e) {
			// turn order around
			sig = mu.ops.getFunction(expr.operator, rightOrg.type, leftOrg.type);
			if(!sig.paramTypes.get(0).matches(leftOrg.type)) {
				left = convert(leftOrg, sig.paramTypes.get(0));
			}
			if(!sig.paramTypes.get(1).matches(rightOrg.type)) {
				right = convert(rightOrg, sig.paramTypes.get(1));
			}
			return mu.invoke(sig.name, left, right, sig);
		}
		return mu.invoke(sig.name, left, right, sig);
	}

	private Result convert(Result res, Type t) {
		Signature sig = new Signature("to" + t, t, res.type);
		Object func = res.type.lookup("to" + t, sig);
		if (func != null && func instanceof Method) {
			try {
				Object val = ((Method)func).invoke(null, res.value);
				return new Result(val, t);
			} catch (Exception e) {
				//return res;
			}
		}
		if (func != null && func instanceof TemplateDef) {
			TemplateDef def = (TemplateDef)func;
			func = new Template(def, mu.environment.capture(def.name));
		}
		if (func != null && func instanceof Template) {
			try {
				Template tmpl = (Template)func;
				List<Expr> exprs = new ArrayList<>();
				exprs.add(new Expr.Literal(mu.currentLine, res.value));
				Expr.Map args = new Expr.Map(mu.currentLine, exprs);
				return Template.call(tmpl, mu, args);
			} catch (Exception e) {
				//return res;
			}
		}
		throw new MuException("Cannot convert %s to %s", res.type, t);
	}

	@Override
	public Result visitUnaryExpr(Expr.Unary expr) {
		Result right = mu.evaluate(expr.right);

		if (right.value instanceof UnitValue) {
			right.type = Type.Unit;
			((UnitValue) right.value).resolve();
		}
		if(right.type instanceof UnionType || right.type instanceof IntersectionType) {
			right.type = Interpreter.typeFromValue(right.value);
		}

		Signature sig = mu.prefixOps.getFunction(expr.operator, right.type);

		switch (expr.operator) {

		case "++":
			Result result = mu.invoke(sig.name, sig, right);
			mu.assign(((Expr.Variable) expr.right), result);
			return result;

		case "--":
			result = mu.invoke(sig.name, sig, right);
			mu.assign(((Expr.Variable) expr.right), result);
			return result;

		case "@":
			Type refType = null;
			if (right.type instanceof RefType) {
				refType = right.type;
			} else {
				refType = new RefType(right.type);
			}
			return mu.invoke(sig.name, sig, new Result(right.value, refType));

		case "↑":
			return mu.invoke(sig.name, sig, right);
			
		case "*":
			return mu.evaluate(expr.right);

		default:
			return mu.invoke(sig.name, sig, right);
		}
	}

	@Override
	public Result visitPostfixExpr(Expr.Postfix expr) {

		Result left = mu.evaluate(expr.left);
		if (left.value instanceof UnitValue) {
			left.type = Type.Unit;
			((UnitValue) left.value).resolve();
		}
		if(left.type instanceof UnionType || left.type instanceof IntersectionType) {
			left.type = Interpreter.typeFromValue(left.value);
		}
		Signature sig = mu.postfixOps.getFunction(expr.operator, left.type);

		switch (expr.operator) {

		case "++":
			Result result = mu.invoke(sig.name, sig, left);
			mu.assign(((Expr.Variable) expr.left), result);
			return left;

		case "--":
			result = mu.invoke(sig.name, sig, left);
			mu.assign(((Expr.Variable) expr.left), result);
			return left;

		default:
			return mu.invoke(sig.name, sig, left);
		}
	}

	@Override
	public Result visitLiteralExpr(Expr.Literal expr) {
		/* string interpolation */
		Type type = Interpreter.typeFromValue(expr.value);
		if (expr.value instanceof String) {
			return new Result(mu.interpolate((String) expr.value), type);
		}
		if (expr.value instanceof RString) {
			return new Result(((RString) expr.value).string, Type.String);
		}
		if (expr.value instanceof UnitValue) {
			((UnitValue) expr.value).resolve();
			return new Result(expr.value, Type.Real);
		}
		return new Result(expr.value, type);
	}

	@Override
	public Result visitTypeLiteralExpr(Expr.TypeLiteral expr) {
		return new Result(expr.literal, Type.Type);
	}

	@Override
	public Result visitSeqExpr(Expr.Seq lst) {
		List<Object> result = new ArrayList<>();
		Type eltType = Type.Any;
		for (Expr expr : lst.exprs) {
			Result r = mu.evaluate(expr);
			boolean spread = mu.spread(expr);
			if (spread) {
				if (eltType.equals(Type.Any)) {
					eltType = r.type;
					result.addAll((List<?>) r.value);
				} else {
					eltType = Type.unite(eltType, r.type);
					result.addAll((List<?>) r.value);
				}
			} else {
				if (eltType.equals(Type.Any)) {
					eltType = r.type;
					result.add(r.value);
				} else {
					eltType = Type.unite(eltType, r.type);
					result.add(r.value);
				}
			}
		}
		return new Result(result, new Type.ListType(eltType));
	}

	@Override
	public Result visitSetExpr(Expr.Set set) {
		Set<Object> result = new HashSet<>();
		Type eltType = Type.Any;
		for (Expr expr : set.exprs) {
			Result r = mu.evaluate(expr);
			boolean spread = mu.spread(expr);
			if (spread) {
				if (eltType.equals(Type.Any)) {
					eltType = r.type;
					result.addAll((Set<?>) r.value);
				} else {
					eltType = Type.unite(eltType, r.type);
					result.addAll((Set<?>) r.value);
				}
			} else {
				if (eltType.equals(Type.Any)) {
					eltType = r.type;
					result.add(r.value);
				} else {
					eltType = Type.unite(eltType, r.type);
					result.add(r.value);
				}
			}
		}
		return new Result(result, new Type.SetType(eltType));
	}

	@Override
	public Result visitMapExpr(Expr.Map map) {
		return mu.doMap(map);
	}

	@Override
	public Result visitRangeExpr(Expr.Range rng) {
		List<Object> result = new ArrayList<>();
		Result start = mu.evaluate(rng.start);
		Type eltType = Type.Any;
		// Int
		if (start.value instanceof BigInteger) {
			eltType = Type.Int;
			BigInteger st = toBigInteger(start.value);
			if (!rng.startIncl)
				st = st.add(BigInteger.ONE);
			Object val = mu.evaluate(rng.end).value;
			BigInteger end = toBigInteger(val);
			if (rng.endIncl)
				end = end.add(BigInteger.ONE);

			if (st.isNaN() || end.isNaN()) {
				return new Result(null, Type.Void);
			}
			if (st.isNegativeInfinity() || st.isPositiveInfinity()) {
				return new Result(null, Type.Void);
			}
			if (end.isNegativeInfinity()) {
				return new Result(new InfiniteList(st, true), new Type.ListType(eltType));
			}
			if (end.isPositiveInfinity()) {
				return new Result(new InfiniteList(st, false), new Type.ListType(eltType));
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
			int end = ((Integer) mu.evaluate(rng.end).value);
			if (rng.endIncl)
				++end;
			if (end < st) {
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

	private BigInteger toBigInteger(Object val) {
		if (val instanceof BigInteger)
			return (BigInteger) val;
		if (val instanceof Double)
			return new BigInteger((Double) val);
		throw new InterpreterError("Cannot cast %s to Int", val);
	}

	@Override
	public Result visitMappingExpr(Mapping expr) {
		return mu.evaluate(expr.value);
	}

	@Override
	public Result visitVariableExpr(Expr.Variable expr) {
		Result res = mu.environment.get(expr.name);
		if(res == null) {
			throw new MuException("Undefined varaiable '%s'", expr.name );
		}
		if (res.value instanceof Property) {
			/* it's a getter */
			return mu.callGetter((Property) res.value);
		}
		return res;
	}

	@Override
	public Result visitAssignExpr(Assign expr) {

		Result leftOrg = mu.environment.get(expr.var.get(0));
		if (leftOrg == null) {
			throw new InterpreterError("Undefined variable %s", expr.var.get(0));
		}
		Result left = leftOrg;
		Result rightOrg = mu.evaluate(expr.value);
		Result right = rightOrg;
		
		Result value = right;
		// op= 
		if(!expr.op.type.equals(Soperator.ASSIGN)) {
			if (left.value instanceof UnitValue) {
				left.type = Type.Unit;
				((UnitValue) left.value).resolve();
			}
			if(left.type instanceof UnionType || left.type instanceof IntersectionType) {
				left.type = Interpreter.typeFromValue(left.value);
			}
			if (right.value instanceof UnitValue) {
				right.type = Type.Unit;
				((UnitValue) right.value).resolve();
			}
			if(right.type instanceof UnionType || right.type instanceof IntersectionType) {
				right.type = Interpreter.typeFromValue(right.value);
			}
			
			String operator = expr.op.lexeme.substring(0, expr.op.lexeme.length() - 1);
			Signature sig = mu.ops.getFunction(operator, left.type, right.type);
			try {
				if(!sig.paramTypes.get(1).matches(right.type)) {
					right = convert(rightOrg, sig.paramTypes.get(1));
				}
			} catch(MuException e) {
				// turn order around
				sig = mu.ops.getFunction(operator, rightOrg.type, leftOrg.type);
				if(!sig.paramTypes.get(0).matches(leftOrg.type)) {
					left = convert(leftOrg, sig.paramTypes.get(0));
				}
				if(!sig.paramTypes.get(1).matches(rightOrg.type)) {
					right = convert(rightOrg, sig.paramTypes.get(1));
				}
				value = mu.invoke(sig.name, left, right, sig);
			}
			value = mu.invoke(sig.name, left, right, sig);
		}		
		mu.assign(expr, value);
		return value;
	}
	
	@Override
	public Result visitBlockExpr(Expr.Block expr) {
		return mu.executeBlock(expr.expressions, new Environment("block", mu.environment));
	}

	@Override
	public Result visitIfExpr(Expr.If stmt) {
		if (mu.isTruthy(stmt.criteria, stmt.condition, stmt.invert)) {
			return mu.evaluate(stmt.thenBranch);
		}
		if (stmt.elseBranch != null) {
			return mu.evaluate(stmt.elseBranch);
		} else {
			return new Result(null, Type.Void);
		}
	}

	@Override
	public Result visitSelectExpr(Expr.Select stmt) {
		Result zwitch = mu.evaluate(stmt.condition);
		if(stmt.whenExpressions.size() != stmt.whenBranches.size() ) {
			throw new InterpreterError("Bad select statement");
		}
		for (Pair<Expr, Expr> when : Interpreter.zip(stmt.whenExpressions, stmt.whenBranches)) {
			if (mu.evaluate(when.left).equals(zwitch)) {
				return mu.evaluate(when.right);
			}
		}
		if (stmt.elseBranch != null) {
			return mu.evaluate(stmt.elseBranch);
		}
		return new Result(null, Type.Void);
	}

	@Override
	public Result visitWhileExpr(Expr.While stmt) {
		List<Object> list = new ArrayList<>();
		Type returnType = Type.None;
		while (mu.isTruthy(stmt.criteria, stmt.condition, stmt.invert)) {
			try {
				Result result = mu.evaluate(stmt.body);
				if (result.value != null) {
					list.add(result.value);
					// UNION type??
					returnType = result.type;
				}
			} catch (BreakJump breakJump) {
				return new Result(list, new Type.ListType(returnType));
			} catch (ContinueJump continueJump) {
				// Do nothing.
			} catch (MuException e) {
				throw e;
			}
		}
		if (stmt.atEnd != null) {
			return mu.evaluate(stmt.atEnd);
		}
		return new Result(list, new Type.ListType(returnType));
	}

	@SuppressWarnings("unchecked")
	@Override
	public Result visitForExpr(For expr) {
		Environment env = new Environment("for", mu.environment);
		Result res = mu.evaluate(expr.range);
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
			type = new Type.MapType(Type.Any);
		} else if (res.value instanceof Callee) {
			Callee pat = (Callee) res.value;
			Map<String, Object> map = new HashMap<>();
			for (Entry<String, TableEntry> param : pat.closure.values.entrySet()) {
				Result val = pat.closure.get(param.getKey());
				map.put(param.getKey(), val.value);
			}
			values = new ArrayList<>(map.entrySet());
			type = new Type.MapType(Type.Any);
		} else if (res.value instanceof Template) {
			Template pat = (Template) res.value;
			Map<String, Object> map = new HashMap<>();
			for (Entry<String, ?> param : pat.closure.values.entrySet()) {
				Result val = pat.closure.get(param.getKey());
				map.put(param.getKey(), val.value);
			}
			values = new ArrayList<>(map.entrySet());
			type = new Type.MapType(Type.Any);
		}
		Object firstValue = values.iterator().next();
		env.define(expr.var.names.get(0), new Result(firstValue, Interpreter.typeFromValue(firstValue)), true,
				false);

		List<Object> list = new ArrayList<>();
		Type returnType = Type.None;
		for (Iterator<Object> it = values.iterator(); it.hasNext();) {
			try {
				env.assign(expr.var.names.get(0), new Result(it.next(), type));
				Result value = mu.executeBlock(expr.body.expressions, env);
				if (value.value != null) {
					list.add(value.value);
					returnType = value.type;
				}
			} catch (ReturnJump retJump) {
				throw retJump;
			} catch (BreakJump breakJump) {
				return new Result(list, new Type.ListType(returnType));
			} catch (ContinueJump continueJump) {
				// Do nothing.
			} catch (MuException me) {
				throw me;
			}
		}
		if (expr.atEnd != null) {
			// if no break encountered, do this:
			return mu.evaluate(expr.atEnd);
		}
		return new Result(list, new Type.ListType(returnType));
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
	public Result visitReturnExpr(Expr.Return stmt) {
		Result value = null;
		if (stmt.value != null)
			value = mu.evaluate(stmt.value);
		throw new ReturnJump(value);
	}

	@Override
	public Result visitThrowExpr(Expr.Throw stmt) {
		Result exception = mu.evaluate(stmt.thrown);
		throw new MuException(exception);
	}

	@Override
	public Result visitCallExpr(Call expr) {
		Result res = mu.evaluate(expr.current);
		if (expr.safe && res.value == null) {
			return res;
		}
		if (res.type instanceof SignatureType) {
			return mu.call(res, expr.next);
		}
		throw new InterpreterError(res.value + "(" + res.type + ") is not callable");
	}

	@Override
	public Result visitDotExpr(Dot expr) {
		Result res = mu.evaluate(expr.current);
		if (expr.safe && res.value == null) {
			return res;
		}
		String key = ((Expr.Variable) expr.next).name;

		try {
			Result ret = null;
			if (res.value instanceof Template || res.value instanceof Map || res.value instanceof Map.Entry
					|| res.value instanceof Callee) {
				if (expr.value != null) {
					return mu.setdot(res, key, mu.evaluate(expr.value).value);
				}
				ret = mu.dot(res, key);
			}
			if (ret == null || ret.value == null) {
				Result attr = objectAttributes(key, res);
				if (attr == null) {
					return ret;
				} else {
					return attr;
				}
			} else {
				return ret;
			}
		} catch (InterpreterError e) {
			Result attr = objectAttributes(key, res);
			if (attr == null) {
				throw new InterpreterError(e);
			} else {
				return attr;
			}
		}
	}

	private Result objectAttributes(String key, Result res) {
		/* handle standard object attributes */
		switch (key) {
		case "str":
			return new Result(mu.stringify(res), Type.String);
		case "len":
			return new Result(lengthOf(res), Type.Int);
		case "type":
			return new Result(res.type, Type.Type);
		case "ord":
			if (res.type instanceof ListEnum) {
				return new Result(ordOf(res), Type.Int);
			}
			break;
		case "minval":
			if (res.value instanceof ListEnum) {
				return new Result(((ListEnum) res.value).low, res.type);
			}
			break;
		case "maxval":
			if (res.value instanceof ListEnum) {
				return new Result(((ListEnum) res.value).high, res.type);
			}
			break;
		case "name":
			if (res.type.equals(Type.Char)) {
				return new Result(Encoding.toUnicodeName((int) res.value), Type.String);
			}
			break;
		case "interfaces":
			if (res.value instanceof Type) {
				return new Result(((Type) res.value).listInterfaces(), new ListType(Type.String));
			}
			break;
		case "values":
			if (res.value instanceof EnumType) {
				return new Result(((ListEnum) res.value).values, new ListType(Type.Any));
			}
			break;
		case "eltType":
			if (res.type instanceof ListType) {
				return new Result(((ListType) res.type).eltType, Type.Type);
			}
			if (res.type instanceof SetType) {
				return new Result(((SetType) res.type).eltType, Type.Type);
			}
			if (res.type instanceof MapType) {
				return new Result(((MapType) res.type).valType, Type.Type);
			}
			if (res.type instanceof RefType) {
				return new Result(((RefType) res.type).type, Type.Type);
			}
			break;
		case "returnType":
			if (res.type instanceof SignatureType) {
				return new Result(((SignatureType) res.type).returnType, Type.Type);
			}
			break;
		case "paramTypes":
			if (res.type instanceof SignatureType) {
				SignatureType sig = (SignatureType) res.type;
				List<Type> types = sig.params.types();
				return new Result(types, new ListType(Type.Type));
			}
			break;
		case "doc":
			if (res.value instanceof Template) {
				String doc = (String) ((Template) res.value).attributes.attr.get("doc");
				if (doc == null) {
					return new Result("<no doc string>", Type.String);
				}
				return new Result(doc, Type.String);
			}
			if (res.value instanceof Callee) {
				String doc = (String) ((Callee) res.value).parent.attributes.attr.get("doc");
				return new Result(doc, Type.String);
			}
			break;
		case "attributes":
			if (res.value instanceof Template) {
				List<String> attrs = ((Template) res.value).attributes.attributes();
				return new Result(attrs, new ListType(Type.String));
			}
			if (res.value instanceof Callee) {
				List<String> attrs = ((Callee) res.value).parent.attributes.attributes();
				return new Result(attrs, new ListType(Type.String));
			}
			break;

		default:
			return null;
		}
		return null;
	}

	private BigInteger ordOf(Result res) {
		ListEnum enumm = (ListEnum) res.type;
		if (res.value instanceof Boolean) {
			return ((Boolean) res.value) ? BigInteger.ONE : BigInteger.ZERO;
		}
		if (res.value instanceof Character) {
			int c = (Character) res.value;
			return BigInteger.valueOf(c);
		}
		if (res.value instanceof Integer) {
			return BigInteger.valueOf((int) res.value);
		}
		if (res.value instanceof Symbol) {
			return BigInteger.valueOf(enumm.values.indexOf(res.value));
		}
		return BigInteger.ZERO;
	}

	@SuppressWarnings("unchecked")
	private BigInteger lengthOf(Result res) {
		if (res.type.name.equals("None")) {
			return BigInteger.ZERO;
		}
		if (res.type instanceof ListType) {
			return BigInteger.valueOf(((List<Object>) res.value).size());
		}
		if (res.type instanceof SetType) {
			return BigInteger.valueOf(((Set<Object>) res.value).size());
		}
		if (res.type instanceof MapType) {
			return BigInteger.valueOf(((Map<String, Object>) res.value).size());
		}
		return BigInteger.ONE;
	}

	@Override
	public Result visitSubscriptExpr(Subscript expr) {
		Result res = mu.evaluate(expr.seq);
		if (expr.safe && res.value == null) {
			return res;
		}
		if (expr.sub instanceof Expr.Range) {
			return mu.sublist(res, (Expr.Range) expr.sub, ((ListType) res.type).eltType);
		}
		if (res.value instanceof Set) {
			throw new InterpreterError("Cannot subscript a set");
		}
		if (res.value instanceof List || res.value instanceof Map)
			return mu.subscript(res, (Seq) expr.sub, expr.value);
		else
			throw new InterpreterError(res.value + "(" + res.type + ") is not indexable");
	}

	@Override
	public Result visitUnitDefExpr(UnitDefExpr expr) {
		String category = expr.name;
		String unit = expr.unit;
		double offset = 0.0;
		double factor = 1.0;
		if (expr.offset != null) {
			Result r = mu.evaluate(expr.offset);
			if (r.type.equals(Type.Real)) {
				offset = (Double) r.value;
			}
			if (r.type.equals(Type.Int)) {
				offset = ((BigInteger) r.value).doubleValue();
			}
		}
		if (expr.factor != null) {
			Result r = mu.evaluate(expr.factor);
			if (r.type.equals(Type.Real)) {
				factor = (Double) r.value;
			}
			if (r.type.equals(Type.Int)) {
				factor = ((BigInteger) r.value).doubleValue();
			}
		}
		if (expr.units != null && !expr.units.isEmpty()) {
			// Derived unit(s)
			if (expr.si)
				UnitRepo.unitDefs(category, unit, expr.units);
			else
				UnitRepo.unitDef(category, unit, expr.units);
		} else if (offset == 0.0 && factor == 1.0) {
			// Base unit(s)
			if (expr.si)
				UnitRepo.unitDefs(category, unit);
			else
				UnitRepo.unitDef(category, unit);
		} else {
			// subunit(s)
			if (expr.si)
				UnitRepo.unitDefs(category, unit, offset, factor);
			else
				UnitRepo.unitDef(category, unit, offset, factor);
		}
		return new Result(null, Type.Void);
	}

}
