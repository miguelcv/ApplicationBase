package org.mcv.mu;

import java.util.Map.Entry;

import lombok.Data;
import lombok.NonNull;

public class Params {
	
	@Data
	public static class ParamFormal {
		
		public ParamFormal(@NonNull String id, @NonNull Type type, Object defval) {
			this.id = id;
			this.type = type;
			this.defval = defval;
		}

		final String id;
		final Type type;
		final Object defval;
		boolean defined;
		Object val;
		
		@Override
		public String toString() {
			return String.format("(id: %s, type: %s, defval: %s, nullable: %s, value: %s)", 
					id, type.toString(), String.valueOf(defval), String.valueOf(val));
		}
	}

	ListMap<ParamFormal> listMap = new ListMap<>();
	ParamFormal[] tmp = new ParamFormal[0];
	boolean vararg;
	
	public Params() {		
	}

	public Params(boolean vararg) {
		this.vararg = vararg;
	}

	public void add(ParamFormal f) {
		listMap.put(f.getId(), f);
	}
	
	/* implement List */
	public ParamFormal get(int index) {
		return listMap.get(index);
	}

	/* Map actual arguments to formal arguments */
	public Params call(ListMap<Object> args) {
		int index = 0;

		for (Entry<String, Object> arg : args.entrySet()) {

			/* map actual to formal parameter by index or by keyword */
			ParamFormal formal = arg.getKey() == null ? listMap.get(index++) : listMap.get(arg.getKey());
			
			/* if not found and if varargs allowed, create new parameter */
			if (formal == null && vararg) {
				String key = arg.getKey();
				formal = new ParamFormal(
						key,   	// the key or Gensym
						// FIXME
						new Type("Any"),  // the type 
						null  	// default value
				);
				// add the new param
				listMap.put(key, formal);
			} else if(formal == null)  {
				throw new Error("This call does not allow varargs");
			}
			
			/* set parameter value */
			setVal(formal, arg.getValue());
		}
		
		for (ParamFormal formal : listMap.values()) {
			if (!formal.isDefined()) {
				formal.setVal(formal.getDefval());
				formal.setDefined(true);
			}
			if (formal.getVal() == null) {
				throw new Error(formal.getId() + " is not nullable!");
			}
		}
		return this;
	}

	void setVal(ParamFormal formal, Object value) {
		if (!formal.isDefined()) {
			if (value != null) {
				/* check type */
				if (formal.getType().compatibleWith(value)) {
					System.out.println("Assigned " + value + " to " + formal.getId());
					formal.val = value;
					formal.defined = true;
				} else {
					throw new Error(value + " is not an instance of " + formal.getType());
				}
			} else {
				throw new Error(formal.getId() + " is not nullable!");
			}
		} else {
			throw new Error(formal.getId() + " has already been instantiated");
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Params {\n");
		for(int i=0; i < listMap.size(); i++) {
			ParamFormal formal = this.get(i);
			sb.append("\t").append(formal.toString()).append("\n");
		}
		sb.append("}");
		return sb.toString();
	}
}
