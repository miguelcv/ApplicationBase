package org.mcv.mu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.mcv.mu.Expr.Map;
import org.mcv.mu.Interpreter.InterpreterError;
import lombok.Data;
import lombok.NonNull;

public class Params {
	
	@Data
	public static class ParamFormal {
		
		public ParamFormal(@NonNull String id, @NonNull Type type, Object defval, Map where, Attributes attr) {
			this.id = id;
			this.type = type;
			this.defval = defval;
			this.attributes = attr;
			this.where = where;
		}

		ParamFormal(@NonNull String id, @NonNull Type type, Object defval, Object value, Map where, Attributes attr, boolean defined) {
			this.id = id;
			this.type = type;
			this.defval = defval;
			this.defined = defined;
			this.val = value;
			this.attributes = attr;
			this.where = where;
		}

		final String id;
		Type type;
		Object defval;
		boolean defined;
		Object val;
		final Attributes attributes;
		Expr.Map where;
		
		@Override
		public String toString() {
			return String.format("(id: %s, type: %s, defval: %s, value: %s, defined :%s)", 
					id, type.toString(), String.valueOf(defval), String.valueOf(val), defined);
		}
	}

	ListMap<ParamFormal> listMap = new ListMap<>();
	ListMap<Object> rest = new ListMap<>();
	ParamFormal[] tmp = new ParamFormal[0];
	boolean vararg;
	boolean curry;
	boolean mutable;
	
	public Params() {		
	}

	public Params(boolean vararg) {
		this.vararg = vararg;
	}

	public Params(Params params) {
		this.curry = params.curry;
		this.vararg = params.vararg;
		this.listMap = new ListMap<>();
		this.rest = new ListMap<>();
		for(Entry<String, ParamFormal> entry : params.listMap.entrySet()) {
			ParamFormal val = entry.getValue();
			ParamFormal pf = new ParamFormal(val.id, val.type, val.defval, val.val, val.where, val.attributes, val.defined);
			this.listMap.put(entry.getKey(), pf);
		}
	}

	public void add(ParamFormal f) {
		listMap.put(f.getId(), f);
	}

	public Object get(int index) {
		ParamFormal p = listMap.get(index);
		if(p.defined) return p.val;
		else return p.defval;
	}

	public Object get(String index) {
		ParamFormal p = listMap.get(index);
		if(p.defined) return p.val;
		else return p.defval;
	}

	public void set(String index, Object value) {
		ParamFormal p = listMap.get(index);
		p.defined = true;
		p.val = value;
	}

	public List<Type> types() {
		List<Type> list = new ArrayList<>();
		for(ParamFormal pf : listMap.values()) {
			list.add(pf.type);
		}
		return list;
	}

	/* Map actual arguments to formal arguments */
	@SuppressWarnings("unchecked")
	public Params call(Interpreter interpreter, Callee callee, Expr.Map args) {

		curry = false;
		rest.clear();
		
		if(args == null) {
			return this;
		}
		
		for (Expr arg : args.mappings) {
			if (arg instanceof Expr.Unary) {
				boolean spread = interpreter.spread(arg);
				if(spread) {
					Result aggr = interpreter.evaluate(((Expr.Unary) arg).right);
					Token tok = ((Expr.Unary) arg).operator;
					if(aggr.value instanceof List) {
						for(Object next : (List<Object>)aggr.value) {
							Expr.Literal expr = new Expr.Literal(tok, next);
							setParam(callee, interpreter, expr);
						}
					}
					if(aggr.value instanceof Set) {
						for(Object next : (Set<Object>)aggr.value) {
							Expr.Literal expr = new Expr.Literal(tok, next);
							setParam(callee, interpreter, expr);
						}
					}
					if(aggr.value instanceof ListMap) {
						for(Entry<String, Object> next : ((ListMap<Object>)aggr.value).entrySet()) {
							Expr.Literal val = new Expr.Literal(tok, next.getValue());
							Expr.Mapping expr = new Expr.Mapping(tok, next.getKey(), val, false);
							setParam(callee, interpreter, expr);
						}
					}
				}
			} else {
				setParam(callee,interpreter, arg);
			}
		}
		
		for (ParamFormal formal : listMap.values()) {
			if (!formal.isDefined()) {
				if(formal.getDefval() != null) {
					setVal(callee, interpreter, formal, formal.getDefval());
				} else {
					curry = true;
				}
			}
		}
		return this;
	}

	private void setParam(Callee callee, Interpreter interpreter, Expr arg) {
		/* map actual to formal parameter by index or by keyword */
		ParamFormal formal = null;

		String key = null;
		if (!(arg instanceof Expr.Mapping)) {
			key = ListMap.gensym.nextSymbol();
			for (ParamFormal f : listMap.values()) {
				if (!f.isDefined()) {
					formal = f;
					break;
				}
			}
		} else {
			Expr.Mapping entry = (Expr.Mapping) arg;
			key = entry.key;
			formal = listMap.get(key);
		}
		
		/* if not found and if varargs allowed, create new parameter */
		if (formal == null && vararg) {
			formal = new ParamFormal(
					key,   		// the key or Gensym
					Type.Any,  	// the type 
					null,  		// default value
					null,       // where
					new Attributes()
			);
			// add the new param
			listMap.put(key, formal);
			rest.put(key, interpreter.evaluate(arg).value);
		} else if(formal == null)  {
			throw new InterpreterError("This call does not allow varargs");
		}
		
		/* set parameter value */
		if(formal.attributes.isThunk()) {
			setVal(callee, interpreter, formal, arg);
		} else {
			setVal(callee, interpreter, formal, interpreter.evaluate(arg).value);
		}		
	}
	
	@SuppressWarnings("unchecked")
	void setVal(Callee callee, Interpreter interpreter, ParamFormal formal, Object value) {
		if (!formal.isDefined()) {
			if(value == null) {
				if(formal.type != Type.Void && (formal.type instanceof Type.UnionType && 
						!((Type.UnionType)formal.type).types.contains(Type.Void))) {
					throw new InterpreterError(formal.getId() + " is not nullable!");
				}
			} else if(value instanceof Expr) {
				formal.val = value;
				formal.defined = true;				
			} else {
				/* check type */
				formal.type = Type.evaluate(formal.type, callee.closure);
				if (formal.getType().matches(Interpreter.typeFromValue(value))) {
					//System.out.println("Assigned " + value + " to " + formal.getId());
					formal.val = value;
					formal.defined = true;
				} else {
					throw new InterpreterError(value + " is not an instance of " + formal.getType());
				}
			}
		} else {
			throw new InterpreterError(formal.getId() + " has already been instantiated");
		}
		
		// add the actual parameter to the environment
		ListMap<String> where = new ListMap<>();
		if(formal.attributes.isMixin()) {
			if (formal.where != null) {
				where = (ListMap<String>) interpreter.evaluate(formal.where).value;
			}
			callee.mixins.add(new Mixin((Callee)formal.val, where));
		}
		if(formal.attributes.isProp()) {
			callee.parent.closure.define(
					formal.id, 
					new Result(
							new Property(mutable, formal.id, new Result(formal.val, formal.type)), 
							formal.type),
					mutable, true);
		} else {
			callee.closure.define(formal.id, new Result(formal.val, formal.type), mutable, false);			
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Params {\n");
		for(int i=0; i < listMap.size(); i++) {
			
			Object value = this.get(i);
			sb.append("\t").append(String.valueOf(value)).append("\n");
		}
		sb.append("}");
		return sb.toString();
	}

	public ListMap<Object> toMap() {
		ListMap<Object> ret = new ListMap<>();
		for(int i=0; i < listMap.size(); i++) {
			ParamFormal value = listMap.get(i);
			ret.put(value.id, value.val);
		}
		return ret;
	}

	public void clear() {
		for (ParamFormal formal : listMap.values()) {
			formal.defined = false;
			formal.val = null;
		}
	}
}
