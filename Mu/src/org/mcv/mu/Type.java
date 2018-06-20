package org.mcv.mu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Params.ParamFormal;
import org.mcv.mu.stdlib.IAny;
import org.mcv.mu.stdlib.IBool;
import org.mcv.mu.stdlib.IChar;
import org.mcv.mu.stdlib.IException;
import org.mcv.mu.stdlib.IInt;
import org.mcv.mu.stdlib.IList;
import org.mcv.mu.stdlib.IMap;
import org.mcv.mu.stdlib.IRange;
import org.mcv.mu.stdlib.IReal;
import org.mcv.mu.stdlib.IRef;
import org.mcv.mu.stdlib.ISet;
import org.mcv.mu.stdlib.IString;
import org.mcv.mu.stdlib.IType;
import org.mcv.mu.stdlib.IUnit;
import org.mcv.mu.stdlib.IVoid;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Type {

	public static final Object UNDEFINED = new Object();
	static Interfaces interfaces = new Interfaces();

	public static Type None = new NoneType("None");
	public static Type Any = new AnyType("Any", IAny.class);
	public static Type Void = new SetEnum("Void", IVoid.class, new Symbol("nil"));
	public static Type Bool = new ListEnum("Bool", IBool.class, false, true);
	public static Type Int = new ListEnum("Int", IInt.class, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	public static Type Real = new ListEnum("Real", IReal.class, Double.MIN_VALUE, Double.MAX_VALUE);
	public static Type Char = new ListEnum("Char", IChar.class, 0, 0x10FFFF);
	public static Type String = new ListType("String", IString.class, Char);
	public static Type Type = new Type("Type", IType.class);
	public static Type Exception = new RefType("Exception", IException.class, Any);
	// dummy type for units
	public static Type Unit = new Type("Unit", IUnit.class);
	// template types for operators
	public static Type A = new NoneType("A");
	public static Type B = new NoneType("B");
	public static Type C = new NoneType("C");

	protected Class<?> javaClass;
	String name;
	boolean unevaluated;

	public Type(String name, Class<?> javaClass) {
		this.name = name;
		this.javaClass = javaClass;
	}

	public Type(String name, Map<Signature, Object> intface) {
		this.name = name;
		interfaces.put(name, intface);
	}

	public Type(String name) {
		this.name = name;
		try {
			javaClass = Class.forName("org.mcv.mu.stdlib.I" + name);
			interfaces.put(name, interfaces());
		} catch (Exception e) {
			// System.err.println("Class not found: I" + name);
		}
	}

	public Type(String name, boolean unevaluated) {
		this.name = name;
		this.unevaluated = unevaluated;
		try {
			javaClass = Class.forName("org.mcv.mu.stdlib.I" + name);
			interfaces.put(name, interfaces());
		} catch (Throwable e) {
			throw new MuException("Class interfaces not found: " + name);
		}
	}

	public static Type evaluate(Type type, Environment env) {
		try {
			Result res = env.get(type.name);
			Type t = type;
			while (t.unevaluated) {
				if (res == null)
					throw new Interpreter.InterpreterError("Unknown type %s", type.name);
				else if (res.value instanceof Template)
					t = res.type;
				else if (res.value instanceof Type) {
					t = (Type) res.value;
					t.unevaluated = false;
				} else
					throw new Interpreter.InterpreterError("Not a type %s", type.name);
			}
			if (t instanceof Aggregate) {
				Aggregate aggr = (Aggregate) t;
				List<Type> types = new ArrayList<>();
				for (Type tp : aggr.types()) {
					if (tp != null)
						types.add(evaluate(tp, env));
				}
				if (!types.isEmpty())
					aggr.setTypes(types);
			}
			t.interfaces();
			return t;
		} catch (Exception e) {
			log.error(e.toString(), e);
			return type;
		}
	}

	@Override
	public String toString() {
		return name;
	}

	public boolean matches(Type other) {
		if (other instanceof UnionType)
			return other.matches(this);
		return this.name.equals(other.name) || other.equals(Any);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Type) {
			Type other = (Type) o;
			return name.equals(other.name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public static class NoneType extends Type {
		public NoneType(String name) {
			super(name);
		}

		public boolean matches(Type other) {
			return false;
		}
	}

	public static class AnyType extends Type {
		public AnyType(String name) {
			super(name);
		}

		public AnyType(String name, Class<IAny> clazz) {
			super(name, clazz);
		}

		public boolean matches(Type other) {
			return true;
		}
	}

	/* SUM TYPES */
	public static abstract class EnumType extends Type {

		public EnumType(String name, Map<Signature, Object> interfaces) {
			super(name, interfaces);
		}

		public EnumType(String name) {
			super(name);
		}

		public EnumType(java.lang.String name, Class<?> clazz) {
			super(name, clazz);
		}

		public abstract boolean valueMatches(Object value);

	}

	public static class SetEnum extends EnumType {

		public SetEnum(String name, Map<Signature, Object> interfaces, Object... values) {
			super(name, interfaces);
			this.values = new HashSet<>();
			this.values.addAll(Arrays.asList(values));
		}

		public SetEnum(String name, Map<Signature, Object> interfaces, Set<Object> values) {
			super(name, interfaces);
			this.values = values;
		}

		public SetEnum(String name, Object... values) {
			super(name);
			this.values = new HashSet<>();
			this.values.addAll(Arrays.asList(values));
		}

		public SetEnum(String name, Set<Object> values) {
			super(name);
			this.values = values;
		}

		final Set<Object> values;

		@Override
		public boolean valueMatches(Object value) {
			return values.contains(value);
		}
	}

	public static class ListEnum extends EnumType {

		public ListEnum(String name, Class<?> clazz, List<Object> values) {
			super(name, clazz);
			this.values = values;
			this.low = values.get(0);
			this.high = values.get(values.size() - 1);
		}

		public ListEnum(String name, Class<?> clazz, Object low, Object high) {
			super(name, clazz);
			this.values = new ArrayList<>();
			this.virtualList = true;
			this.low = low;
			this.high = high;
		}

		public ListEnum(String name, Map<Signature, Object> interfaces, List<Object> values) {
			super(name, interfaces);
			this.values = values;
			this.low = values.get(0);
			this.high = values.get(values.size() - 1);
		}

		public ListEnum(String name, List<Object> values) {
			super(name);
			this.values = values;
			this.low = values.get(0);
			this.high = values.get(values.size() - 1);
		}

		Object low;
		Object high;
		boolean virtualList;
		final List<Object> values;

		@Override
		public boolean valueMatches(Object value) {
			return values.contains(value);
		}
	}

	public static class Range extends EnumType {
		public Range(String name, Map<Signature, Object> interfaces, Expr start, Expr end) {
			super(name, interfaces);
			this.start = start;
			this.end = end;
		}

		public Range(String name, Expr start, Expr end) {
			super(name);
			javaClass = IRange.class;
			interfaces();
			this.start = start;
			this.end = end;
		}

		final Expr start;
		final Expr end;

		@Override
		public boolean valueMatches(Object value) {
			return true;
		}
	}

	/* PRODUCT TYPES */
	public static abstract class Aggregate extends Type {

		public Aggregate(String name) {
			super(name);
		}

		public Aggregate(String name, Class<?> clazz) {
			super(name, clazz);
		}

		public Aggregate(String name, Map<Signature, Object> interfaces) {
			super(name, interfaces);
		}

		public abstract List<Type> types();

		public abstract void setTypes(List<Type> types);

		@Override
		public boolean matches(Type other) {
			if (other instanceof UnionType)
				return other.matches(this);
			if (other.getClass().equals(getClass())) {
				List<Type> list = this.types();
				List<Type> otherList = ((Aggregate) other).types();
				if (list.size() != otherList.size())
					return false;
				int len = list.size();
				for (int i = 0; i < len; i++) {
					Type type = list.get(i);
					Type otherType = otherList.get(i);
					if (type != null && otherType != null) {
						if (!type.matches(otherType)) {
							return false;
						}
					}
				}
				return true;
			}
			return other.equals(this);
		}
	}

	public static class ListType extends Aggregate {

		public ListType(String name, Class<?> clazz, Type type) {
			super(name, clazz);
			this.eltType = type;
		}

		public ListType(String name, Map<Signature, Object> interfaces, Type type) {
			super(name, interfaces);
			this.eltType = type;
		}

		public ListType(Map<Signature, Object> interfaces, Type type) {
			super("List[" + type + "]", interfaces);
			this.eltType = type;
		}

		public ListType(Type type) {
			super("List[" + type + "]");
			this.javaClass = IList.class;
			interfaces();
			this.eltType = type;
		}

		Type eltType;

		@Override
		public List<Type> types() {
			List<Type> ret = new ArrayList<>();
			ret.add(eltType);
			return ret;
		}

		@Override
		public void setTypes(List<Type> types) {
			eltType = types.get(0);
		}
	}

	public static class SetType extends Aggregate {
		public SetType(Map<Signature, Object> interfaces, Type type) {
			super("Set[" + type + "]", interfaces);
			this.eltType = type;
		}

		public SetType(Type type) {
			super("Set[" + type + "]");
			this.javaClass = ISet.class;
			interfaces();
			this.eltType = type;
		}

		Type eltType;

		@Override
		public List<org.mcv.mu.Type> types() {
			List<Type> ret = new ArrayList<>();
			ret.add(eltType);
			return ret;
		}

		@Override
		public void setTypes(List<Type> types) {
			eltType = types.get(0);
		}
	}

	public static class MapType extends Aggregate {
		public MapType(Map<Signature, Object> interfaces, Type valType) {
			super("Map[" + valType + "]", interfaces);
			this.valType = valType;
		}

		public MapType(Type valType) {
			super("Map[" + valType + "]");
			this.javaClass = IMap.class;
			interfaces();
			this.valType = valType;
		}

		Type valType;

		@Override
		public List<org.mcv.mu.Type> types() {
			List<Type> ret = new ArrayList<>();
			ret.add(valType);
			return ret;
		}

		@Override
		public void setTypes(List<Type> types) {
			valType = types.get(0);
		}
	}

	public static class RefType extends Aggregate {

		public RefType(String name, Class<?> clazz, Type type) {
			super(name, clazz);
			this.type = type;
		}

		public RefType(String name, Map<Signature, Object> interfaces, Type type) {
			super(name, interfaces);
			this.type = type;
		}

		public RefType(Map<Signature, Object> interfaces, Type type) {
			super("Ref[" + type + "]", interfaces);
			this.type = type;
		}

		public RefType(Type type) {
			super("Ref[" + type + "]");
			javaClass = IRef.class;
			interfaces();
			this.type = type;
		}

		Type type;

		@Override
		public List<org.mcv.mu.Type> types() {
			List<Type> ret = new ArrayList<>();
			ret.add(type);
			return ret;
		}

		@Override
		public void setTypes(List<Type> types) {
			type = types.get(0);
		}
	}

	public static class SignatureType extends Aggregate {

		SignatureType(TemplateDef def) {
			super(def.name == null ? "λ" : def.name);
			interfaces(def);
			this.kind = def.kind;
			this.params = def.params;
			this.returnType = def.returnType;
		}

		SignatureType(String kind, Type returnType, Params params) {
			super("λ");
			this.kind = kind;
			this.params = params;
			this.returnType = returnType;
		}

		String kind; // FUN, CLASS
		Params params;
		Type returnType;

		@Override
		public List<Type> types() {
			List<Type> list = new ArrayList<>();
			list.add(returnType);
			list.addAll(params.types());
			return list;
		}

		@Override
		public void setTypes(List<Type> types) {
			returnType = types.get(0);
			for (int i = 1; i <= params.listMap.size(); i++) {
				ParamFormal pf = params.listMap.get(i - 1);
				pf.type = types.get(i);
			}
		}

		@Override
		public boolean matches(Type other) {
			if (super.matches(other)) {
				if (other instanceof SignatureType)
					return this.kind.equals(((SignatureType) other).kind);
				return true;
			}
			return false;
		}
	}

	/* UNION AND INTERSECTION */
	public static class UnionType extends Aggregate {

		public UnionType(Type left, Type right) {
			super(left.name + "|" + right.name, union(left.interfaces(), right.interfaces()));
			this.left = left;
			this.right = right;
			types = typesOf(left, right);
			name = name(types);
		}

		public UnionType(Set<Type> types) {
			super(name(types), interfaces(types));
			List<Type> asList = new ArrayList<>(types);
			this.left = asList.get(0);
			this.right = asList.size() > 2 ? new UnionType(new HashSet<>(asList.subList(1, asList.size())))
					: asList.get(1);
			this.types = types;
		}

		private static String name(Set<Type> types) {
			StringBuilder sb = new StringBuilder();
			for (Type t : types) {
				if (sb.length() > 0)
					sb.append("|");
				sb.append(t);
			}
			return sb.toString();
		}

		private static Map<Signature, Object> interfaces(Set<Type> types) {
			Map<Signature, Object> ret = new LinkedHashMap<>();
			for (Type t : types) {
				ret.putAll(t.interfaces());
			}
			return ret;
		}

		final Type left;
		final Type right;
		Set<Type> types;

		public List<Type> types() {
			return new ArrayList<>(types);
		}

		@Override
		public void setTypes(List<Type> types) {
			this.types = new HashSet<>(types);
		}

		@Override
		public boolean matches(Type other) {
			List<Type> list = new ArrayList<>(types);
			int len = list.size();
			UnionType uo = null;
			List<Type> otherList = null;
			if (other instanceof UnionType) {
				uo = (UnionType) other;
				otherList = new ArrayList<>(uo.types);
				if (len != otherList.size())
					return false;
				for (int i = 0; i < len; i++) {
					Type type = list.get(i);
					if (!otherList.contains(type))
						return false;
				}
				return true;
			}
			for (int i = 0; i < len; i++) {
				Type type = list.get(i);
				if (type.matches(other)) {
					return true;
				}
			}
			return false;
		}
	}

	public static class IntersectionType extends Aggregate {
		public IntersectionType(Type left, Type right) {
			super(left.name + "&" + right.name, intersection(left.interfaces(), right.interfaces()));
			this.left = left;
			this.right = right;
			types = typesOfIntersect(left, right);
		}

		public IntersectionType(Set<Type> types) {
			super(name(types), interfaces(types));
			List<Type> asList = new ArrayList<>(types);
			this.left = asList.get(0);
			this.right = asList.size() > 2 ? new IntersectionType(new HashSet<>(asList.subList(1, asList.size())))
					: asList.get(1);
			this.types = types;
		}

		private static String name(Set<Type> types) {
			StringBuilder sb = new StringBuilder();
			for (Type t : types) {
				if (sb.length() > 0)
					sb.append("&");
				sb.append(t);
			}
			return sb.toString();
		}

		private static Map<Signature, Object> interfaces(Set<Type> types) {
			Iterator<Type> it = types.iterator();
			Map<Signature, Object> ret = it.next().interfaces();
			for (; it.hasNext();) {
				Type next = it.next();
				for (Entry<Signature, Object> entry : ret.entrySet()) {
					if (!next.interfaces().containsKey(entry.getKey())) {
						ret.remove(entry.getKey());
					}
				}
			}
			return ret;
		}

		final Type left;
		final Type right;
		Set<Type> types;

		@Override
		public List<Type> types() {
			return new ArrayList<>(types);
		}

		@Override
		public boolean matches(Type other) {
			List<Type> list = new ArrayList<>(types);
			int len = list.size();
			IntersectionType io = null;
			List<Type> otherList = null;
			if (other instanceof IntersectionType) {
				io = (IntersectionType) other;
				otherList = new ArrayList<>(io.types);
				if (len != otherList.size())
					return false;
				for (int i = 0; i < len; i++) {
					Type type = list.get(i);
					if (!otherList.contains(type))
						return false;
				}
				return true;
			}
			for (int i = 0; i < len; i++) {
				Type type = list.get(i);
				if (!type.matches(other)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public void setTypes(List<Type> types) {
			this.types = new HashSet<>(types);
		}
	}

	public Map<Signature, Object> interfaces(TemplateDef def) {
		Map<Signature, Object> intface = interfaces.get(name);
		if (intface == null)
			intface = new HashMap<>();
		if (intface.isEmpty()) {
			if (def.body != null) {
				for (Expr expr : def.body.expressions) {
					if (expr instanceof TemplateDef) {
						intface.put(new Signature((TemplateDef) expr), expr);
					}
				}
			}
			Method[] methods = IAny.class.getDeclaredMethods();
			for (Method m : methods) {
				intface.put(new Signature(m), m);
			}
		}
		interfaces.put(name, intface);
		return intface;
	}

	public Map<Signature, Object> interfaces() {
		Map<Signature, Object> intface = interfaces.get(name);
		if (intface == null)
			intface = new HashMap<>();
		if (intface.isEmpty() && javaClass != null) {
			Method[] methods = javaClass.getDeclaredMethods();
			for (Method m : methods) {
				intface.put(new Signature(m), m);
			}
		}
		interfaces.put(name, intface);
		return intface;
	}

	public static Map<Signature, Object> union(Map<Signature, Object> interfaces, Map<Signature, Object> interfaces2) {
		Map<Signature, Object> ret = new HashMap<>();
		for (Signature key : interfaces.keySet()) {
			ret.put(key, interfaces.get(key));
		}
		for (Signature key : interfaces2.keySet()) {
			ret.put(key, interfaces2.get(key));
		}
		return ret;
	}

	public static Map<Signature, Object> intersection(Map<Signature, Object> interfaces,
			Map<Signature, Object> interfaces2) {
		Map<Signature, Object> ret = new HashMap<>();
		for (Signature key : interfaces.keySet()) {
			if (interfaces2.containsKey(key)) {
				ret.put(key, interfaces.get(key));
			}
		}
		return ret;
	}

	private static Set<Type> typesOf(Type t1, Type t2) {
		Set<Type> types = new HashSet<>();
		if (t1.equals(t2))
			types.add(t1);
		else if (t1 instanceof UnionType && t2 instanceof UnionType) {
			UnionType u1 = (UnionType) t1;
			UnionType u2 = (UnionType) t2;
			types.addAll(typesOf(u1.left, u1.right));
			types.addAll(typesOf(u2.left, u2.right));
		} else if (t2 instanceof UnionType) {
			UnionType u = (UnionType) t2;
			types.addAll(typesOf(u.left, u.right));
			types.add(t1);
		} else if (t1 instanceof UnionType) {
			UnionType u = (UnionType) t1;
			types.addAll(typesOf(u.left, u.right));
			types.add(t2);
		} else {
			if (!t1.name.equals("None"))
				types.add(t1);
			if (!t2.name.equals("None"))
				types.add(t2);
		}
		return types;
	}

	public static Type unite(Type t1, Type t2) {
		if (t1.equals(t2))
			return t1;
		else if (t1 instanceof UnionType && t2 instanceof UnionType) {
			UnionType u1 = (UnionType) t1;
			UnionType u2 = (UnionType) t2;
			return new UnionType(unite(u1.left, u1.right), unite(u2.left, u2.right));
		} else if (t2 instanceof UnionType) {
			UnionType u = (UnionType) t2;
			return new UnionType(t1, unite(u.left, u.right));
		} else if (t1 instanceof UnionType) {
			UnionType u = (UnionType) t1;
			return new UnionType(unite(u.left, u.right), t2);
		} else {
			return new UnionType(t1, t2);
		}
	}

	private static Set<Type> typesOfIntersect(Type t1, Type t2) {
		Set<Type> types = new HashSet<>();
		if (t1.equals(t2))
			types.add(t1);
		else if (t2 instanceof IntersectionType) {
			IntersectionType i = (IntersectionType) t2;
			types.addAll(typesOfIntersect(i.left, i.right));
		} else if (t1 instanceof IntersectionType) {
			IntersectionType i = (IntersectionType) t1;
			types.addAll(typesOfIntersect(i.left, i.right));
		} else {
			types.add(t1);
			types.add(t2);
		}
		return types;
	}

	public Object lookup(String func, Signature signature) {

		this.interfaces();
		Map<Signature, Object> intface = interfaces.get(name);
		if (intface == null)
			intface = new HashMap<>();
		if (intface.containsKey(signature)) {
			return interfaces.get(name).get(signature);
		}

		for (Entry<Signature, Object> entry : intface.entrySet()) {
			Signature intfc = entry.getKey();
			if (!intfc.name.equals(func)) {
				continue;
			}
			if (!intfc.returnType.matches(signature.returnType)) {
				continue;
			}
			boolean allOk = true;
			if (intfc.paramTypes.size() != signature.paramTypes.size()) {
				continue;
			}
			for (Pair<Type, Type> pair : Interpreter.zip(intfc.paramTypes, signature.paramTypes)) {
				if (!pair.left.matches(pair.right)) {
					allOk = false;
					break;
				}
			}
			if (allOk) {
				return entry.getValue();
			}
		}
		return null;
	}

	public List<String> listInterfaces() {
		List<String> ret = new ArrayList<>();
		Map<Signature, Object> intface = interfaces();
		for (Signature sig : intface.keySet()) {
			ret.add(sig.name);
		}
		return ret;
	}

}
