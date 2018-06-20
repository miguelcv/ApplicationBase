package org.mcv.mu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mcv.math.BigInteger;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Params.ParamFormal;
import org.mcv.uom.UnitValue;

public class Signature {
	
	String name = "";
	Type returnType = Type.None;
	List<Type> paramTypes = new ArrayList<>();
	
	public Signature(TemplateDef def) {
		name = def.name;
		returnType = def.returnType;
		paramTypes = new ArrayList<>();
		for(Entry<String, ParamFormal> entry : def.params.listMap.entrySet()) {
			paramTypes.add(entry.getValue().type);
		}
	}
	
	public Signature(Method method) {
		name = method.getName();
		returnType = typeFromClass(method.getReturnType());
		paramTypes = new ArrayList<>();
		for(Class<?> clazz : method.getParameterTypes()) {
			paramTypes.add(typeFromClass(clazz));
		}
	}

	public Signature(String name, Type returnType, Type... paramTypes) {
		this.name = name;
		this.returnType = returnType;
		this.paramTypes = new ArrayList<>();
		for(Type type : paramTypes) {
			this.paramTypes.add(type);
		}		
	}

	public Signature(Signature tmpl) {
		this.name = tmpl.name;
		this.returnType = tmpl.returnType;
		this.paramTypes = new ArrayList<>();
		for(Type type : tmpl.paramTypes) {
			this.paramTypes.add(type);
		}
	}

	static Type typeFromClass(Class<?> clz) {
		/* We have char, string, int, real, bool */
		if (clz.equals(Void.class))
			return Type.Void;
		if (clz.equals(Object.class))
			return Type.Any;
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
		if (clz.equals(UnitValue.class))
			return Type.Unit;
		if (clz.equals(MuException.class))
			return Type.Exception;
		if (clz.equals(Double.class) || clz.equals(double.class))
			return Type.Real;
		if (clz.equals(Boolean.class) || clz.equals(boolean.class))
			return Type.Bool;
		if (clz.isAssignableFrom(java.util.List.class)) {
			return new Type.ListType(null, Type.Any);
		}
		if (clz.isAssignableFrom(java.util.Set.class)) {
			return new Type.SetType(null, Type.Any);
		}
		if (clz.isAssignableFrom(java.util.Map.class)) {
			return new Type.MapType(null, Type.Any);
		}
		if (clz.isAssignableFrom(Map.Entry.class)) {
			return new Type.MapType(null, Type.Any);
		}
		if (clz.isAssignableFrom(Pointer.class)) {
			return new Type.RefType(null, Type.Any);
		}
		System.err.println("Type is " + clz);
		return Type.None;
	}

	@Override public boolean equals(Object o) {
		if(o instanceof Signature) {
			Signature other = (Signature)o;
			if(name != null && !name.equals(other.name)) return false;
			if(returnType != null && !returnType.equals(other.returnType)) return false;
			if(this.paramTypes.size() != other.paramTypes.size()) return false;
			try {
				for (Pair<Type, Type> types : Interpreter.zip(this.paramTypes, other.paramTypes)) {
					if(types.left != null && !types.left.equals(types.right)) {
						return false;
					}
				}
			} catch(Exception e) {
				return false;
			}
			return true;
		}
		return false;
	}
	
	@Override public int hashCode() {
		int code = 0;
		if(name != null) {
			code = name.hashCode();
		}
		if(returnType != null && !returnType.equals(Type.Any)) {
			code += returnType.hashCode(); 
		}
		for(Type type : paramTypes) {
			if(type != null && !type.equals(Type.Any)) {
				code += type.hashCode(); 
			}			
		}
		return code;
	}

	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		if(name != null) {
			sb.append(name);
		}
		sb.append(":");
		if(returnType != null) {
			sb.append(returnType.name);
		}
		sb.append(":(");
		for(Type type : paramTypes) {
			if(type != null) {
				sb.append(type.name).append(",");
			}
		}
		if(paramTypes.size() > 0) {
			// delete comma
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append(")");
		return sb.toString();
	}
}
