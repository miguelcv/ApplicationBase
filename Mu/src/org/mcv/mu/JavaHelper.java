package org.mcv.mu;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.mcv.math.BigInteger;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Params.ParamFormal;

public class JavaHelper {

	public static List<Class<?>> handleJar(URLClassLoader loader, File jar, String regex) {
		List<Class<?>> result = new ArrayList<>();
		try (JarInputStream jarFile = new JarInputStream(new FileInputStream(jar))) {
			while (true) {
				try {
					JarEntry jarEntry = jarFile.getNextJarEntry();
					if (jarEntry == null) {
						break;
					}
					if (jarEntry.getName().endsWith(".class")) {
						String className = jarEntry.getName().replaceAll("/", "\\.");
						className = className.substring(0, className.length() - ".class".length());
						String shortName = className.substring(className.lastIndexOf('.') + 1);
						if (shortName.matches(regex) && !className.contains("$")) {
							try {
								Class<?> cl = loader.loadClass(className);
								result.add(cl);
							} catch (Exception e) {
								System.err.println(e.toString());
							}
						}
					}
				} catch (EOFException e) {
					break;
				}
			}
			return result;
		} catch (Exception e) {
			throw new Interpreter.InterpreterError(e);
		}
	}

	public static Class<?> getClass(URLClassLoader loader, String name) {
		try {
			return loader.loadClass(name);
		} catch (Exception e) {
			throw new MuException("Unable to load class %s", name);
		}
	}

	public static void evalClass(Interpreter mu, Template tmpl) {
		if (tmpl.javaClass == null)
			return;
		// define self
		tmpl.closure.define(tmpl.javaClass.getSimpleName(),
				new Result(tmpl, new Type.SignatureType((TemplateDef) tmpl.def)), false, false);
		Method[] methods = tmpl.javaClass.getMethods();
		for (Method method : methods) {
			// Java has overloaded methods!!
			// Java is a Lisp2!!
			Callee callee = null;
			if (tmpl.closure.get("__jvm_" + method.getName()) != null) {
				Result res = tmpl.closure.get("__jvm_" + method.getName());
				callee = (Callee) res.value;
				callee.parent.javaMethodList.add(method);
				tmpl.closure.assign("__jvm_" + method.getName(), new Result(callee, res.type));
			} else {
				callee = JavaHelper.calleeFromMethod(mu, method, null);
				tmpl.closure.define("__jvm_" + method.getName(),
						new Result(callee, new Type.SignatureType(callee.parent.def)), true, false);
			}
		}
		Field[] fields = tmpl.javaClass.getFields();
		for (Field field : fields) {
			tmpl.closure.define(field.getName(), new Result(Type.UNDEFINED, typeFromClass(field.getType())), true,
					false);
		}
		Class<?>[] classes = tmpl.javaClass.getClasses();
		for (Class<?> clazz : classes) {
			if (tmpl.closure.get(clazz.getSimpleName()) == null) {
				Template clz = classToTemplate(mu, clazz.getSimpleName(), clazz);
				tmpl.closure.define(clazz.getSimpleName(),
						new Result(clz, new Type.SignatureType((TemplateDef) clz.def)), false, false);
			}
		}
	}

	private static Callee calleeFromMethod(Interpreter mu, Method method, Object object) {
		Template fun = new Template();
		fun.kind = "fun";
		fun.attributes.put("jvm", true);
		fun.closure = new Environment("method", mu.environment);
		fun.params = new Params();
		fun.params.vararg = true;
		fun.returnType = Type.Any;
		fun.name = method.getName();
		fun.def = new TemplateDef(new Token(Keyword.FUN, fun.name, fun.name, -1), "fun", fun.params, fun.returnType,
				new Expr.Block(new Token(Soperator.LEFT_PAREN, "(", "(", -1), new ArrayList<>()), fun.attributes);
		fun.javaClass = method.getDeclaringClass();
		fun.javaMethodList.add(method);
		Callee callee = new Callee(fun);
		callee.javaObject = object;
		return callee;
	}

	private static Type typeFromClass(Class<?> clz) {
		if (clz == null || clz.equals(Void.class) || clz.equals(void.class))
			return Type.Void;

		if (clz.equals(Object.class))
			return Type.Any;

		if (clz.equals(Integer.class) || clz.equals(int.class))
			return Type.Int;
		if (clz.equals(Byte.class) || clz.equals(byte.class))
			return Type.Int;
		if (clz.equals(Short.class) || clz.equals(short.class))
			return Type.Int;
		if (clz.equals(Integer.class) || clz.equals(int.class))
			return Type.Int;
		if (clz.equals(Long.class) || clz.equals(long.class))
			return Type.Int;
		if (clz.equals(java.math.BigInteger.class))
			return Type.Int;
		if (clz.equals(Double.class) || clz.equals(double.class))
			return Type.Real;
		if (clz.equals(Float.class) || clz.equals(float.class))
			return Type.Real;

		if (clz.equals(String.class))
			return Type.String;

		if (clz.equals(Character.class) || clz.equals(char.class))
			return Type.Char;

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

		return new Type(clz.getSimpleName());
	}

	public static Template classToTemplate(Interpreter mu, String name, Class<?> clazz) {
		Template tmpl = new Template();
		tmpl.attributes.put("jvm", true);
		tmpl.javaClass = clazz;

		tmpl.closure = new Environment(name, mu.environment);

		tmpl.kind = "class";
		tmpl.name = name;

		tmpl.params = new Params();
		tmpl.params.vararg = true;

		Constructor<?>[] constructors = clazz.getConstructors();
		tmpl.javaConstructorList = Arrays.asList(constructors);
		Token tok = new Token(Keyword.CLASS, name, name, -1);
		tmpl.def = new Expr.TemplateDef(tok, "class", tmpl.params, tmpl.returnType,
				new Expr.Block(tok, new ArrayList<>()), tmpl.attributes);
		tmpl.returnType = new Type.SignatureType((TemplateDef) tmpl.def);
		evalClass(mu, tmpl);

		return tmpl;
	}

	public static Object[] toJavaArgs(Params params, Class<?>[] classes) {
		int i = 0;
		Object[] ret = new Object[classes.length];
		for (Entry<String, ParamFormal> param : params.listMap.entrySet()) {
			ParamFormal pf = param.getValue();
			ret[i] = mkParam(pf.val, classes[i]);
			++i;
		}
		return ret;
	}

	/* Java return value to Mu result */
	@SuppressWarnings("unchecked")
	public static Result mkResult(Interpreter mu, Object ret) {

		if (ret == null)
			return new Result(null, Type.Void);

		if (ret instanceof Byte)
			return new Result(BigInteger.valueOf((long) ret), Type.Int);
		if (ret instanceof Short)
			return new Result(BigInteger.valueOf((long) ret), Type.Int);
		if (ret instanceof Integer)
			return new Result(BigInteger.valueOf((long) ret), Type.Int);
		if (ret instanceof Long)
			return new Result(BigInteger.valueOf((long) ret), Type.Int);

		if (ret instanceof Float)
			return new Result((double) ret, Type.Real);
		if (ret instanceof Double)
			return new Result((double) ret, Type.Real);

		if (ret instanceof Boolean)
			return new Result((boolean) ret, Type.Bool);
		if (ret instanceof Character)
			return new Result((int) ret, Type.Char);

		if (ret instanceof String)
			return new Result((String) ret, Type.String);
		if (ret instanceof List)
			return new Result((List<Object>) ret, new Type.ListType(Type.Any));
		if (ret.getClass().isArray())
			return new Result(Arrays.asList(ret), new Type.ListType(typeFromClass(ret.getClass().getComponentType())));
		if (ret instanceof Map)
			return new Result((Map<String, Object>) ret, new Type.MapType(Type.Any));
		if (ret instanceof Set)
			return new Result((Set<Object>) ret, new Type.SetType(Type.Any));

		if (ret instanceof Throwable) {
			return new Result(ret, Type.Exception);
		}
		if (ret instanceof Class) {
			Template tmpl = classToTemplate(mu, ((Class<?>) ret).getSimpleName(), (Class<?>) ret);
			return new Result(tmpl, new Type.SignatureType((TemplateDef) tmpl.def));
		} else {
			Class<?> clz = ret.getClass();
			Callee callee = new Callee(classToTemplate(mu, clz.getSimpleName(), clz));
			callee.javaObject = ret;
			callee.params = new Params();
			callee.params.vararg = true;
			return new Result(callee, new Type.SignatureType(callee.parent.def));
		}
	}

	/* Mu parameter to Java Object */
	@SuppressWarnings("unchecked")
	public static Object mkParam(Object param, Class<?> javaType) {
		try {
			if (param == null)
				return null;
			if (javaType.equals(Byte.class) || javaType.equals(byte.class))
				return ((BigInteger) param).byteValue();
			if (javaType.equals(Short.class) || javaType.equals(short.class))
				return ((BigInteger) param).shortValue();
			if (javaType.equals(Integer.class) || javaType.equals(int.class))
				return ((BigInteger) param).intValue();
			if (javaType.equals(Long.class) || javaType.equals(long.class))
				return ((BigInteger) param).longValue();
			if (javaType.equals(Float.class) || javaType.equals(float.class))
				return ((Double) param).floatValue();
			if (javaType.equals(Double.class) || javaType.equals(double.class))
				return param;
			if (javaType.equals(Boolean.class) || javaType.equals(boolean.class))
				return param;
			if (javaType.equals(Character.class) || javaType.equals(boolean.class))
				return (char) ((Integer) param).intValue();
			if (javaType.equals(String.class))
				return param;
			if (javaType.isAssignableFrom(List.class))
				return param;
			if (javaType.isArray())
				return ((List<Object>) param).toArray();
			if (javaType.isAssignableFrom(Map.class))
				return param;
			if (javaType.isAssignableFrom(Set.class))
				return param;
			if (javaType.isAssignableFrom(Throwable.class))
				return param;
			if (javaType.isAssignableFrom(Class.class)) {
				return ((Template) param).javaClass;
			} else {
				return ((Callee) param).javaObject;
			}
		} catch (Exception e) {
			throw new Interpreter.InterpreterError("Type error: " + javaType);
		}
	}

	public static Object lookupAndInvokeMethod(Callee obj) {
		nextMethod: for (Method method : obj.javaMethodList) {
			try {
				Class<?>[] types = method.getParameterTypes();
				if (types.length != obj.params.listMap.size())
					continue;
				Object[] args = toJavaArgs(obj.params, types);
				for (int i = 0; i < types.length; i++) {
					if (!types[i].isAssignableFrom(args[i].getClass())) {
						continue nextMethod;
					}
				}
				return method.invoke(obj.javaObject, args);
			} catch (Exception e) {
			}
		}
		throw new Interpreter.InterpreterError("No suitable method: " + obj.javaMethodList.get(0).getName());
	}

	public static Object lookupAndInvokeConstructor(Callee obj) {
		nextConstructor: for (Constructor<?> constructor : obj.parent.javaConstructorList) {
			try {
				Class<?>[] types = constructor.getParameterTypes();
				if (types.length != obj.params.listMap.size())
					continue;
				Object[] args = toJavaArgs(obj.params, types);
				for (int i = 0; i < types.length; i++) {
					if (!types[i].isAssignableFrom(args[i].getClass())) {
						continue nextConstructor;
					}
				}
				obj.javaObject = constructor.newInstance(args);
				return obj.javaObject;
			} catch (Exception e) {
				//
			}
		}
		throw new Interpreter.InterpreterError("No suitable constructor: " + obj.toString());
	}

}
