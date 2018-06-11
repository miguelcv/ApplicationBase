package org.mcv.mu;

import org.mcv.mu.Expr.TemplateDef;

public class Setter extends Result {
	
	public Setter(String name, Type type) {
		Expr.TemplateDef expr = (TemplateDef) new Mu("-eval").parse("fun set_" + name + "(" + type + " value): ( " + Getter.scramble(name) + " := value )");
		this.value = expr;
		this.type = Interpreter.typeFromValue(expr);
	}

}
