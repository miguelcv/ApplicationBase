package org.mcv.mu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mcv.mu.Expr.ClassDef;
import org.mcv.mu.Expr.FuncDef;
import org.mcv.mu.Expr.InterfaceDef;
import org.mcv.mu.Expr.IterDef;

class Type {

	public Type(String name) {
		this.name = name;
	}

	public Type() {
	}

	public String name;

	@Override
	public String toString() {
		return name;
	}

	/* SUM TYPES */
	public static class SetEnum extends Type {
		public SetEnum(String name, String... values) {
			this.name = name;
			this.values = new HashSet<>();
			this.values.addAll(Arrays.asList(values));
		}

		public SetEnum(String name, Set<String> values) {
			this.name = name;
			this.values = values;
		}

		final Set<String> values;

		@Override
		public String toString() {
			return "enum{" + values + "}";
		}
	}

	public static class ListEnum extends Type {
		public ListEnum(String name, String... values) {
			this.name = name;
			this.values = new ArrayList<>();
			this.values.addAll(Arrays.asList(values));
		}

		public ListEnum(String name, List<String> values) {
			this.name = name;
			this.values = values;
		}

		final List<String> values;

		@Override
		public String toString() {
			return "enum[" + values + "]";
		}
	}

	/* PRODUCT TYPES */
	public static class ListType extends Type {
		public ListType(Type type) {
			this.name = "List";
			this.type = type;
		}

		final Type type;

		@Override
		public String toString() {
			return "[" + type + "]";
		}
	}

	public static class SetType extends Type {
		public SetType(Type type) {
			this.name = "Set";
			this.type = type;
		}

		final Type type;

		@Override
		public String toString() {
			return "{" + type + "}";
		}
	}

	public static class RefType extends Type {
		public RefType(Type type) {
			this.name = "Ref";
			this.type = type;
		}

		final Type type;

		@Override
		public String toString() {
			return "@" + type;
		}
	}

	public static class StructType extends Type {
		public StructType(Type... types) {
			this.name = "Struct";
			this.types = Arrays.asList(types);
		}

		public StructType(List<Type> types) {
			this.name = "Struct";
			this.types = types;
		}

		final List<Type> types;

		@Override
		public String toString() {
			return types.toString();
		}
	}

	public static class TypeType extends Type {
		public TypeType(String... interfaces) {
			this.name = "Type";
			this.interfaces = Arrays.asList(interfaces);
		}

		public TypeType(List<String> interfaces) {
			this.name = "Struct";
			this.interfaces = interfaces;
		}

		final List<String> interfaces;

		@Override
		public String toString() {
			return "Type(" + interfaces.toString() + ")";
		}
	}

	public static class MapType extends Type {
		public MapType(Type left, Type right) {
			this.name = "Map";
			this.left = left;
			this.right = right;
		}

		final Type left;
		final Type right;

		@Override
		public String toString() {
			return "(" + left + ":" + right + ")";
		}
	}

	/* UNION AND INTERSECTION */
	public static class UnionType extends Type {
		public UnionType(Type left, Type right) {
			this.name = left.name + "|" + right.name;
			this.left = left;
			this.right = right;
		}

		final Type left;
		final Type right;

		@Override
		public String toString() {
			return left + "|" + right;
		}
	}

	public static class IntersectionType extends Type {
		public IntersectionType(Type left, Type right) {
			this.name = left.name + "&" + right.name;
			this.left = left;
			this.right = right;
		}

		final Type left;
		final Type right;

		@Override
		public String toString() {
			return left + "&" + right;
		}
	}

	static class SignatureType extends Type {
		SignatureType(String name, String kind, Params params, Set<String> interfaces, Type returnType) {
			this.name = name == null ? "λ" : name;
			this.interfaces = interfaces;
			this.kind = kind;
			this.params = params;
			this.returnType = returnType;
		}

		SignatureType(ClassDef def) {
			this.name = def.name == null ? "λ" : def.name.lexeme;
			this.interfaces = def.interfaces;
			this.kind = "class";
			this.params = def.params;
			this.returnType = null;
		}

		SignatureType(IterDef def) {
			this.name = def.name == null ? "λ" : def.name.lexeme;
			this.interfaces = null;
			this.kind = "iter";
			this.params = def.params;
			this.returnType = def.returnType;
		}

		SignatureType(InterfaceDef def) {
			this.name = def.name == null ? "λ" : def.name.lexeme;
			this.interfaces = def.interfaces;
			this.kind = "interface";
			this.params = def.params;
			this.returnType = null;
		}

		SignatureType(FuncDef def) {
			this.name = def.name == null ? "λ" : def.name.lexeme;
			this.interfaces = null;
			this.kind = "fun";
			this.params = def.params;
			this.returnType = def.returnType;
		}

		String kind; // fun, iter, class, interface
		Params params;
		Set<String> interfaces;
		Type returnType;

		@Override
		public String toString() {
			return kind + params.toString() + "=>" + (interfaces != null ? interfaces : returnType);
		}
	}

	boolean compatibleWith(Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	static Type None = new SetEnum("None");
	static Type Void = new SetEnum("Void", "null");
	static Type Bool = new ListEnum("Bool", "true", "false");
	static Type Int = new Type("Int");
	static Type Real = new Type("Real");
	static Type Char = new Type("Char");
	static Type String = new Type("String");
	static Type Any = new Type("Any");
	static Type Type = new Type("Type");
	// static Type Func = new Type("Func");
	// static Type List = new Type("List");
	// static Type Set = new Type("Set");
	// static Type Tuple = new Type("Tuple");
	// static Type Range = new Type("Range");
	static Type Num = new Type("Int&Real");
	static Type Text = new Type("Char&String");

}
