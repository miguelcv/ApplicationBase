package nl.novadoc.tools;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Expression {
	final Type type;
	Map<String, Variable> symTable;
	List<Term> terms;
	
	Value eval() {
		Value v;
		for(Term term : terms) {
			v = term.eval();
			return v;
		}
		return null;
	}
}
