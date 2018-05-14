package org.mcv.mu;

import java.util.List;

interface MuCallable {
	int arity();
	Object call(Interpreter interpreter, List<Object> arguments);
}
