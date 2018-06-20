package org.mcv.mu;

import java.util.ArrayList;
import java.util.List;

public class Property {
	
	int line;
	
	public Property(int line, boolean mutable, String name, Result var) {
		this.var = var;
		this.name = name;
		this.line = line;
		get = mkGetter();
		if(mutable) {
			set = mkSetter();
		}
	}
	
	String name;
	Result var;
	Expr.Block get;
	Expr.Block set;
	
	Expr.Block mkGetter() {
		List<Expr> expressions = new ArrayList<>();
		expressions.add(new Expr.Variable("$value", line));
		return new Expr.Block(line, expressions);
	}

	Expr.Block mkSetter() {
		List<Expr> expressions = new ArrayList<>();
		Token opToken = new Token(Soperator.ASSIGN, ":=", ":=", line);
		expressions.add(new Expr.Assign("$value", line, new Expr.Variable("$new", line), opToken));
		return new Expr.Block(line, expressions);
	}
}
