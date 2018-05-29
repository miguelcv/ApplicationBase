package org.mcv.mu;

import java.util.ArrayList;
import java.util.List;

import org.mcv.mu.Params.ParamFormal;

public class Property extends Template {
	
	public Property(Token id, Type type, Environment env) {
		this.name = id.lexeme;
		this.id = id;
		kind = "fun";
		attributes = new Attributes();
		returnType = type;
		closure = env;
	}
	
	Token id;
	boolean ro;
	Type type;
	
	Property mkSetter(Object value) {
		Property setter = new Property(id, type, closure);
		ParamFormal f = new ParamFormal("value", Interpreter.typeFromValue(value), null, attributes);
		setter.params = new Params();	
		setter.params.add(f);
		setter.returnType = Type.Void;
		List<Expr> exprs = new ArrayList<>();
		Expr lit = new Expr.Literal(id, value);
		Expr assign = new Expr.Assign(id, lit, new Token(Soperator.ASSIGN, ":=", null, -1));
		exprs.add(assign);		
		setter.body = new Expr.Block(Token.DUMMY, exprs);
		setter.def = new Expr.TemplateDef(id, "fun", setter.params, Type.Void, setter.body, attributes);
		return setter;
	}

	Property mkGetter() {
		Property getter = new Property(id, type, closure);
		getter.params = new Params();
		List<Expr> exprs = new ArrayList<>();
		Expr var = new Expr.Variable(id);
		exprs.add(var);		
		getter.body = new Expr.Block(Token.DUMMY, exprs);
		getter.def = new Expr.TemplateDef(id, "fun", getter.params, returnType, getter.body, attributes);
		return getter;
	}

	@Override public String toString() {
		return "property " + name;
	}

}
