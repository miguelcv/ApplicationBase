package nl.novadoc.tools;

import lombok.Data;
import lombok.NonNull;

@Data
public class ParamFormal {
	
	public ParamFormal(@NonNull String id, @NonNull Class<?> type, Object defval) {
		this(id, type, defval, false);
	}
	
	public ParamFormal(@NonNull String id, @NonNull Class<?> type, Object defval, boolean nullable) {
		this.id = id;
		this.type = type;
		this.defval = defval;
		this.nullable = nullable;
	}

	final String id;
	final Class<?> type;
	final Object defval;
	final boolean nullable;
	boolean defined;
	Object val;
	
	@Override
	public String toString() {
		return String.format("(id: %s, type: %s, defval: %s, nullable: %s, value: %s)", 
				id, type.getSimpleName(), String.valueOf(defval), String.valueOf(nullable), String.valueOf(val));
	}
}
