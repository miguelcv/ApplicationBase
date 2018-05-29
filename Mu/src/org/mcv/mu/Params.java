package org.mcv.mu;

import java.util.Map.Entry;
import org.mcv.mu.Interpreter.InterpreterError;
import lombok.Data;
import lombok.NonNull;

public class Params {
	
	@Data
	public static class ParamFormal {
		
		public ParamFormal(@NonNull String id, @NonNull Type type, Object defval, Attributes attr) {
			this.id = id;
			this.type = type;
			this.defval = defval;
			this.attributes = attr;
		}

		ParamFormal(@NonNull String id, @NonNull Type type, Object defval, Object value, Attributes attr, boolean defined) {
			this.id = id;
			this.type = type;
			this.defval = defval;
			this.defined = defined;
			this.val = value;
			this.attributes = attr;
		}

		final String id;
		final Type type;
		Object defval;
		boolean defined;
		Object val;
		final Attributes attributes;
		
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
			ParamFormal pf = new ParamFormal(val.id, val.type, val.defval, val.val, val.attributes, val.defined);
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

	/* Map actual arguments to formal arguments */
	/* CURRY */
	public Params call(ListMap<Object> args) {

		curry = false;
		rest.clear();
		
		for (Entry<String, Object> arg : args.entrySet()) {

			/* map actual to formal parameter by index or by keyword */
			ParamFormal formal = null;
			
			if(isGensym(arg.getKey())) {
				for (ParamFormal f : listMap.values()) {
					if (!f.isDefined()) {
						formal = f;
						break;
					}
				}
			} else {
				formal = listMap.get(arg.getKey());
			}
			
			/* if not found and if varargs allowed, create new parameter */
			if (formal == null && vararg) {
				String key = arg.getKey();
				formal = new ParamFormal(
						key,   		// the key or Gensym
						Type.Any,  	// the type 
						null,  		// default value
						new Attributes()
				);
				// add the new param
				listMap.put(key, formal);
				rest.put(key, arg.getValue());
			} else if(formal == null)  {
				throw new InterpreterError("This call does not allow varargs");
			}
			
			/* set parameter value */
			setVal(formal, arg.getValue());
		}
		
		for (ParamFormal formal : listMap.values()) {
			if (!formal.isDefined()) {
				if(formal.getDefval() != null) {
					setVal(formal, formal.getDefval());
				} else {
					curry = true;
				}
			}
		}
		return this;
	}

	private boolean isGensym(String key) {
		return key.startsWith(ListMap.gensym.prefix);
	}

	void setVal(ParamFormal formal, Object value) {
		if (!formal.isDefined()) {
			if (value != null) {
				/* check type */
				if (formal.getType().matches(Interpreter.typeFromValue(value))) {
					//System.out.println("Assigned " + value + " to " + formal.getId());
					formal.val = value;
					formal.defined = true;
				} else {
					throw new InterpreterError(value + " is not an instance of " + formal.getType());
				}
			} else {
				throw new InterpreterError(formal.getId() + " is not nullable!");
			}
		} else {
			throw new InterpreterError(formal.getId() + " has already been instantiated");
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

	public void clear() {
		for (ParamFormal formal : listMap.values()) {
			formal.defined = false;
			formal.val = null;
		}
	}
}
