package nl.novadoc.tools;

public class Params {
	
	ListMap<String, ParamFormal> listMap = new ListMap<>();
	ParamFormal[] tmp = new ParamFormal[0];
	static Gensym gensym = new Gensym();
	
	
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
	public Params call(ParamActual... args) {
		int index = 0;

		for (ParamActual arg : args) {

			/* map actual to formal parameter by index or by keyword */
			ParamFormal formal = arg.getKeyword() == null ? listMap.get(index++) : listMap.get(arg.getKeyword());
			
			/* if not found and if varargs allowed, create new parameter */
			if (formal == null && vararg) {
				String key = arg.getKeyword();
				if (key == null) {
					/* Gensym keyword if needed */
					key = gensym.nextSymbol();
				}
				formal = new ParamFormal(
						key,   	// the key or Gensym
						arg.getVal().getClass(),  // the type 
						null,  	// default value
						true	// nullable?
				);
				// add the new param
				listMap.put(key, formal);
			} else if(formal == null)  {
				throw new TypeError("This call does not allow varargs");
			}
			
			/* set parameter value */
			setVal(formal, arg.getVal());
		}
		
		for (ParamFormal formal : listMap.values()) {
			if (!formal.isDefined()) {
				formal.setVal(formal.getDefval());
				formal.setDefined(true);
			}
			if (!formal.isNullable() && formal.getVal() == null) {
				throw new TypeError(formal.getId() + " is not nullable!");
			}
		}
		return this;
	}

	void setVal(ParamFormal formal, Object value) {
		if (!formal.isDefined()) {
			if (value != null) {
				/* check type */
				if (formal.getType().isAssignableFrom(value.getClass())) {
					System.out.println("Assigned " + value + " to " + formal.getId());
					formal.val = value;
					formal.defined = true;
				} else {
					throw new TypeError(value + " is not an instance of " + formal.getType());
				}
			} else {
				if (formal.isNullable()) {
					System.out.println("Assigned NIL to " + formal.getId());
					formal.val = null;
					formal.defined = true;
				} else {
					throw new TypeError(formal.getId() + " is not nullable!");
				}
			}
		} else {
			throw new TypeError(formal.getId() + " has already been instantiated");
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
	
	public static void main(String... args) {
		Params params = new Params(true);
		params.add(new ParamFormal("param1", String.class, null));
		params.add(new ParamFormal("param2", Integer.class, 2));
		params.add(new ParamFormal("param3", Double.class, null));

		params.call(new ParamActual("param3", 4.9), new ParamActual("hello"), new ParamActual(2));
		System.out.println(params);
	}

}
