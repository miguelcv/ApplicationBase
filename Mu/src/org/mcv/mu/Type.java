package org.mcv.mu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mcv.mu.Expr.*;
import org.mcv.mu.stdlib.IAny;
import org.mcv.mu.stdlib.IBool;
import org.mcv.mu.stdlib.IChar;
import org.mcv.mu.stdlib.IException;
import org.mcv.mu.stdlib.IInt;
import org.mcv.mu.stdlib.IList;
import org.mcv.mu.stdlib.IListEnum;
import org.mcv.mu.stdlib.IMap;
import org.mcv.mu.stdlib.INone;
import org.mcv.mu.stdlib.IRange;
import org.mcv.mu.stdlib.IReal;
import org.mcv.mu.stdlib.IRef;
import org.mcv.mu.stdlib.ISet;
import org.mcv.mu.stdlib.ISetEnum;
import org.mcv.mu.stdlib.IString;
import org.mcv.mu.stdlib.IStruct;
import org.mcv.mu.stdlib.IThunk;
import org.mcv.mu.stdlib.IType;
import org.mcv.mu.stdlib.IVoid;

public class Type {

	public String name;
	public Map<String, Object> interfaces = new HashMap<>();
	
	public Type(String name, Map<String, Object> interfaces) {
		this.name = name;
		this.interfaces = interfaces;                                                                                        
	}

	public Type() {
	}

	public Type(String name) {
		this.name = name;
		try {
			this.interfaces = interfacesOf(Class.forName("org.mcv.mu.stdlib.I"+name));
		} catch(Exception e) {
			System.err.println("Class not found: I" + name);
		}
	}

	@Override
	public String toString() {
		return name;                                                                                    
		
	}
	
	public boolean matches(Object o) {
		if(o instanceof Type) {
			Type other = (Type)o;
			if(this instanceof UnionType) {
				return ((UnionType)this).left.matches(other) ||
						((UnionType)this).right.matches(other);
			}
			if(other instanceof UnionType) {	
				return ((UnionType)other).left.matches(this) ||
						((UnionType)other).right.matches(this);
			}
			if(this instanceof IntersectionType) {
				return ((IntersectionType)this).left.matches(other) &&
						((IntersectionType)this).right.matches(other);
			}
			if(other instanceof IntersectionType) {	
				return ((IntersectionType)other).left.matches(this) ||
						((IntersectionType)other).right.matches(this);
			}
			if(name.equals("Any") || other.name.equals("Any")) 
				return true;
			if(name.equals("None") || other.name.equals("None")) 
				return false;
			return name.equals(other.name);
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Type) {
			Type other = (Type)o;
			return name.equals(other.name);
		}
		return false;
	}

	/* SUM TYPES */
	public static class SetEnum extends Type {
		public SetEnum(String name, Map<String, Object>interfaces, Object... values) {
			super(name, interfaces);
			this.name = name;
			this.values = new HashSet<>();
			this.values.addAll(Arrays.asList(values));
		}

		public SetEnum(String name, Map<String, Object>interfaces, Set<Object> values) {
			super(name, interfaces);
			this.values = values;
		}
		
		public SetEnum(String name, Object... values) {
			super(name, interfacesOf(ISetEnum.class));
			this.name = name;
			this.values = new HashSet<>();
			this.values.addAll(Arrays.asList(values));
		}

		public SetEnum(String name, Set<Object> values) {
			super(name, interfacesOf(ISetEnum.class));
			this.values = values;
		}

		final Set<Object> values;
	}

	public static class ListEnum extends Type {
		public ListEnum(String name, Map<String, Object> interfaces, Object... values) {
			super(name, interfaces);
			this.values = new ArrayList<>();
			this.values.addAll(Arrays.asList(values));
		}

		public ListEnum(String name,  Map<String, Object> interfaces, List<Object> values) {
			super(name, interfaces);
			this.values = values;
		}

		public ListEnum(String name, Object... values) {
			super(name, interfacesOf(IListEnum.class));
			this.values = new ArrayList<>();
			this.values.addAll(Arrays.asList(values));
		}

		public ListEnum(String name, List<Object> values) {
			super(name, interfacesOf(IListEnum.class));
			this.values = values;
		}

		final List<Object> values;
	}

	/* PRODUCT TYPES */
	public static class ListType extends Type {
		public ListType(String name, Map<String, Object> interfaces, Type type) {
			super(name, interfaces);
			this.eltType = type;
		}
		public ListType(Map<String, Object> interfaces, Type type) {
			super("List["+ type +"]", interfaces);
			this.eltType = type;
		}

		public ListType(Type type) {
			super("List[" + type +"]", interfacesOf(IList.class));
			this.eltType = type;
		}

		final Type eltType;
	}
	
	public static class Range extends Type {
		public Range(String name, Map<String, Object> interfaces, Expr start, Expr end) {
			super(name, interfaces);
			this.start = start;
			this.end = end;
		}

		public Range(String name, Expr start, Expr end) {
			super(name, interfacesOf(IRange.class));
			this.start = start;
			this.end = end;
		}

		final Expr start;
		final Expr end;
	}


	public static class SetType extends Type {
		public SetType(Map<String, Object> interfaces, Type type) {
			super("Set["+type+"]", interfaces);
			this.eltType = type;
		}

		public SetType(Type type) {
			super("Set["+type+"]", interfacesOf(ISet.class));
			this.eltType = type;
		}

		final Type eltType;
	}

	public static class RefType extends Type {
		public RefType(Map<String, Object> interfaces, Type type) {
			super("Ref["+type+"]", interfaces);
			this.type = type;
		}

		public RefType(Type type) {
			super("Ref", interfacesOf(IRef.class));
			this.type = type;
		}

		final Type type;
	}

	public static class ThunkType extends Type {
		public ThunkType(String name, Map<String, Object> interfaces, Expr expr) {
			super(name, interfaces);
			this.expr = expr;
		}

		public ThunkType(String name, Expr expr) {
			super(name, interfacesOf(IThunk.class));
			this.expr = expr;
		}

		final Expr expr;
	}

	public static class StructType extends Type {
		public StructType(Map<String, Object> interfaces, Type... types) {
			super("Struct" + Arrays.deepToString(types), interfaces);
			this.types = Arrays.asList(types);
		}

		public StructType(Type... types) {
			super("Struct" + Arrays.deepToString(types), interfacesOf(IStruct.class));
			this.types = Arrays.asList(types);
		}

		public StructType(List<Type> types) {
			super("Struct" + Arrays.deepToString(types.toArray(new Type[0])), interfacesOf(IStruct.class));
			this.types = types;
		}

		final List<Type> types;
	}

	public static class MapType extends Type {
		public MapType(Map<String, Object> interfaces, Type left, Type right) {
			super("Map["+left+":"+right+"]", interfaces);
			this.left = left;
			this.right = right;
		}

		public MapType(Type left, Type right) {
			super("Map["+left+":"+right+"]", interfacesOf(IMap.class));
			this.left = left;
			this.right = right;
		}

		final Type left;
		final Type right;
	}

	/* UNION AND INTERSECTION */
	public static class UnionType extends Type {
		public UnionType(Type left, Type right) {
			super(left.name + "|" + right.name, union(left.interfaces, right.interfaces));
			this.left = left;
			this.right = right;
		}

		final Type left;
		final Type right;
	}

	public static class IntersectionType extends Type {
		public IntersectionType(Type left, Type right) {
			super(left.name + "&" + right.name, intersection(left.interfaces, right.interfaces));
			this.left = left;
			this.right = right;
		}

		final Type left;
		final Type right;
	}

	static class SignatureType extends Type {
		SignatureType(TemplateDef def) {
			super(def.name == null ? "λ" : 
				def.name.lexeme, 
				interfacesOf(def));
			this.kind = def.kind;
			this.params = def.params;
			this.returnType = this;
		}
		String kind; // FUN, CLASS
		Params params;
		Type returnType;
	}

	static Type None = new SetEnum("None", interfacesOf(INone.class), Set.of());
	static Type Void = new SetEnum("Void", interfacesOf(IVoid.class), Set.of("null"));
	static Type Bool = new ListEnum("Bool", interfacesOf(IBool.class), List.of("true", "false"));
	static Type Int = new ListEnum("Int", interfacesOf(IInt.class), List.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));  // range!
	static Type Real = new ListEnum("Real", interfacesOf(IReal.class), List.of(Double.MIN_VALUE, Double.MAX_VALUE)); // range!
	static Type Char = new ListEnum("Char", interfacesOf(IChar.class), List.of(0, 0x10FFFF)); // range!
	static Type String = new ListType("String", interfacesOf(IString.class), Char);	
	static Type Any = new Type("Any", interfacesOf(IAny.class));
	static Type Type = new Type("Type", interfacesOf(IType.class));
	static Type Exception = new Type("Exception", interfacesOf(IException.class));
	
	public static Map<java.lang.String, Object> interfacesOf(Class<?> clazz) {
		Method[] methods = clazz.getDeclaredMethods();
		Map<String, Object> ret = new HashMap<>();
		for(Method m : methods) {
			ret.put(m.getName(), m);
		}
		return ret;
	}

	public static Map<java.lang.String, Object> intersection(Map<java.lang.String, Object> interfaces,
			Map<java.lang.String, Object> interfaces2) {
		Map<String, Object> ret = new HashMap<>();
		for(String key : interfaces.keySet()) {
			ret.put(key, interfaces.get(key));
		}
		for(String key : interfaces2.keySet()) {
			ret.put(key, interfaces2.get(key));
		}
		return ret;
	}

	public static Map<java.lang.String, Object> union(Map<java.lang.String, Object> interfaces,
			Map<java.lang.String, Object> interfaces2) {
		Map<String, Object> ret = new HashMap<>();
		for(String key : interfaces.keySet()) {
			if(interfaces2.containsKey(key)) {
				ret.put(key, interfaces.get(key));
			}
		}
		return ret;
	}
	
	public static Map<java.lang.String, Object> interfacesOf(TemplateDef def) {
		Map<String, Object> ret = new HashMap<>();
		if(def.body == null) return ret;
		for(Expr expr : def.body.expressions) {
			if(expr instanceof TemplateDef) {
				ret.put(((TemplateDef) expr).name.lexeme, expr);
			}
		}
		return ret;
	}

}
