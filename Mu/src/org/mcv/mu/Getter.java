package org.mcv.mu;

import org.mcv.mu.Expr.TemplateDef;

public class Getter extends Result {

	public static String scramble(String name) {
		return "__"+name;
	}

	public Getter(String name, Type type) {
		Expr.TemplateDef expr = (TemplateDef) new Mu("-eval").parse("fun get_" + name + "() => "+ type +": ( " + scramble(name) + " )");
		this.value = expr;
		this.type = Interpreter.typeFromValue(expr);	
	}

}
