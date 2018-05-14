package nl.novadoc.tools;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Variable {
	final String name;
	final Type type;
	Value value;
	public void setValue(Value v) {
		if(!mutable) {
			throw new TypeError("Variable %s is immutable", name);
		}
		if(!type.check(v)) {
			throw new TypeError("Value %v is incompatible with type %s for variable %s", v, type, name);
		}
		initialized = true;
		value = v;
	}
	final boolean nullable;
	@Getter(AccessLevel.NONE) @Setter(AccessLevel.NONE) boolean initialized;
	final boolean mutable;
	final boolean exported;
}
