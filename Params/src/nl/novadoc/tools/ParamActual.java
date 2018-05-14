package nl.novadoc.tools;

import lombok.Data;
import lombok.NonNull;

@Data
public class ParamActual {
	
	public ParamActual(@NonNull Object value) {
		this.val = value;
	}
	public ParamActual(@NonNull String keyword, @NonNull Object value) {
		this.keyword = keyword; 
		this.val = value;
	}
	String keyword;
	final Object val;
}
