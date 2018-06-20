package org.mcv.mu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Operators {

	Map<Signature, List<String>> funlist = new HashMap<>();
	Map<String, Signature> oplist = new HashMap<>();

	public void makeOperator(Signature fun, String... op) {
		List<String> ops = funlist.get(fun);
		if (ops == null) {
			ops = new ArrayList<>();
		}
		ops.addAll(Arrays.asList(op));
		funlist.put(fun, ops);
		for (String operator : op) {
			/* do not overwrite existing entries!! */
			if(oplist.get(operator) == null) {
				oplist.put(operator, fun);
			}
		}
	}

	public Signature getFunction(String op, Type... types) {
		try {
			Signature sig = new Signature(oplist.get(op));
			Type[] subst = new Type[] { Type.A, Type.B, Type.C };
			int current = 0;
			for (Type formal : subst) {
				if (current >= types.length) {
					break;
				}
				Type actual = types[current++];
				for (int i = 0; i < sig.paramTypes.size(); i++) {
					if (sig.paramTypes.get(i).equals(formal)) {
						sig.paramTypes.set(i, actual);
					}
				}
				if (sig.returnType.equals(formal)) {
					sig.returnType = actual;
				}
			}
			return sig;
		} catch (Exception e) {
			throw new Interpreter.InterpreterError(e);
		}
	}
}
