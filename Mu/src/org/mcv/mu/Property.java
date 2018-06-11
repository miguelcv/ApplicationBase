package org.mcv.mu;

import java.util.ArrayList;
import java.util.List;

public class Property {
	
	Token valueToken = new Token(Soperator.ID, "$value", "$value", -1);
	Token newToken = new Token(Soperator.ID, "$new", "$new", -1);
	Token opToken = new Token(Soperator.ASSIGN, ":=", ":=", -1);
	
	public Property(boolean mutable, String name, Result var) {
		this.var = var;
		this.name = name;
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
		Token nameToken = new Token(Soperator.ID, name, name, -1);
		List<Expr> expressions = new ArrayList<>();
		expressions.add(new Expr.Variable(valueToken));
		return new Expr.Block(nameToken, expressions);
	}

	Expr.Block mkSetter() {
		Token nameToken = new Token(Soperator.ID, name, name, -1);
		List<Expr> expressions = new ArrayList<>();
		expressions.add(new Expr.Assign(valueToken, new Expr.Variable(newToken), opToken));
		return new Expr.Block(nameToken, expressions);
	}
}
