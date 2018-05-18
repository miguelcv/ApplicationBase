package org.mcv.mu;

import static org.mcv.mu.Soperator.*;
import static org.mcv.mu.Keyword.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mcv.mu.Expr.Block;
import org.mcv.mu.Expr.ClassDef;
import org.mcv.mu.Expr.FuncDef;
import org.mcv.mu.Expr.Getter;
import org.mcv.mu.Expr.InterfaceDef;
import org.mcv.mu.Expr.IterDef;
import org.mcv.mu.Params.ParamFormal;

class Parser {
	
	private static class ParseError extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	private final List<Token> tokens;
	private int current = 0;

	private int looplevel = 0;

	Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	List<Expr> parse() {
		List<Expr> statements = new ArrayList<>();
		while (!isAtEnd()) {
			List<Expr> declarations = declarations();
			if (declarations != null)
				statements.addAll(declarations);
		}
		return statements;
	}

	private List<Expr> declarations() {
		List<Expr> lst = new ArrayList<>();
		HashMap<String, Object> attributes = new HashMap<>();
		try {
			while(isAttribute()) {
				attribute(attributes);
			}
			if(match(MODULE)) {
				lst.add(moduleDeclaration(attributes));
			} else if(match(IMPORT)) {
				lst.add(importDeclaration(attributes));
			} else if (match(TYPE)) {
				lst.add(typeDeclaration(attributes));
			} else if (match(CLASS)) {
				lst.add(patternDeclaration(CLASS.name(), attributes));
			} else if (match(INTERFACE)) {
				lst.add(patternDeclaration(INTERFACE.name(), attributes));
			} else if (match(FUN)) {
				lst.add(patternDeclaration(FUN.name(), attributes));
			} else if (match(ITER)) {
				lst.add(patternDeclaration(ITER.name(), attributes));
			} else if (match(VAR)) {
				lst.addAll(varDeclarations(attributes));
			} else if (match(VAL)) {
				lst.addAll(valDeclarations(attributes));
			} else {
				lst.add(statement());
			}
			return lst;
		} catch (Exception error) {
			synchronize();
			return null;
		}
	}

	private boolean isAttribute() {
		try {
			Attribute.valueOf(peek().lexeme.toUpperCase());
			return true;
		} catch(IllegalArgumentException e) {
			return false;
		}
	}

	private void attribute(Map<String, Object> attributes) {
		/* for now, values are only booleans */
		Token attr = advance();
		attributes.put(attr.lexeme, true);		
	}

	/* DECLARATIONS */
	private Expr patternDeclaration(String kind, HashMap<String, Object> attributes) {
		// [attr ...] kind [ID] ( params ) => {Super, ...} | [T, ...] : ( block )
		
		Token name = null;
		Params params = null;
		Set<String> interfaces = null;
		Type returnType = null;
		Expr.Block body = null;
		
		if(kind.equals(FUN.name()) || kind.equals(ITER.name())) {
			if(match(ID))
				name = consume(ID, "Expect name.");
		} else if(kind.equals(CLASS.name()) || kind.equals(INTERFACE.name())) {
			if(match(ID))
				name = consume(TID, "Expect type name.");
		} else {
			Mu.error(tokens.get(current).line, "Invalid pattern declaration");
		}
		
		if(match(LEFT_PAREN)) {
			// val params
			params = params();
		}
		
		consume(ARROW, "Expected arrow");
		
		if(match(LEFT_BRACE)) {
			interfaces = new HashSet<>(eval(set()));
		} else {
			returnType = typeLiteral();
		}

		if(match(COLON)) {
			consume(COLON, "Expected :");
			if(match(LEFT_PAREN)) {
				body = (Block) block();
			}
		}
		
		if(kind.equals(CLASS.name())) {
			return new Expr.ClassDef(name, params, interfaces, body, attributes);
		} else if(kind.equals(INTERFACE.name())) {
			return new Expr.InterfaceDef(name, params, interfaces, body, attributes);
		} else if (kind.equals(FUN.name())) {
			return new Expr.FuncDef(name, params, returnType, body, attributes);
		} else if(kind.equals(ITER.name())) {
			return new Expr.IterDef(name, params, returnType, body, attributes);
		}
		return null;
	}

	private Params params() {
		// ( =>type name [: defval], ..., [*])
		Params params = new Params();
		do {
			
			boolean typeParam = false;
			
			if(match(STAR)) {
				params.vararg = true;
				break;
			}
			
			Map<String, Object> attributes = new HashMap<>();
			
			while(isAttribute()) {
				attribute(attributes);
			}

			Type type = typeLiteral();
			if(type == null) {
				Mu.error(tokens.get(current).line, "Expected type");
			}
			Token name = null;
			
			if(check(ID)) {
				name = consume(ID, "Expect name.");
			}
			if(type.name.equals("Type") && check(TID)) {
				typeParam = true;
				name = consume(ID, "Expect type name.");
			}
			
			Expr defval = null;
			if(match(COLON)) {
				// type
				if(typeParam) {
					defval = new Expr.TypeLiteral(null, typeLiteral(), attributes);
				} else {
					defval = expression();
				}
			}
			ParamFormal formal = new ParamFormal(name.lexeme, type, defval);
			params.add(formal);
		} while(match(COMMA));
		consume(RIGHT_PAREN, "Expected )");
		return params;
	}

	private Expr moduleDeclaration(HashMap<String, Object> attributes) {
		Expr.Getter qname = (Getter) selector();
		return new Expr.Module(qname.name, attributes);
	}

	private Expr importDeclaration(Map<String, Object> attributes) {
		// TODO
		// import QID [ where ( map ) ]
		return null;
	}

	private Expr typeDeclaration(Map<String, Object> attributes) {
		// type ID: { ID, ... } | [ ID, ... ] | TID | list(...) | set(...) | ref(...) | [T, ...]
		
		Token name = consume(TID, "Expect type name.");
		consume(COLON, "Expected colon");		
		return new Expr.TypeLiteral(name, typeLiteral(), attributes);
	}

	private Type typeLiteral() {

		Type left = type();
		
		/* union */
		if(match(OR)) {
			return new Type.UnionType(left, typeLiteral());
		}
		
		/* intersection */
		if(match(AND)) {
			return new Type.IntersectionType(left, typeLiteral());
		}
		
		return left;
	}
	
	private Type type() {
		/* signature */
		if(match(FUN, ITER, CLASS, INTERFACE)) {
			return signature(previous().lexeme);
		}
		
		if(match(TID)) {
			/* simple type name or alias */
			return new Type(previous().lexeme);
		}
		
		if(match(LEFT_BRK)) {
			String name = tokens.get(current - 3).lexeme;
			return new Type.ListEnum(name, eval(list()));
		}

		if(match(LEFT_BRACE)) {
			String name = tokens.get(current - 3).lexeme;
			return new Type.SetEnum(name, new HashSet<String>(eval(set())));
		}
		
		if(match(LIST)) {
			consume(LEFT_PAREN, "Expected (");
			Type type = typeLiteral();
			consume(RIGHT_PAREN, "Expected )");
			return new Type.ListType(type);
		}
		
		if(match(SET)) {
			consume(LEFT_PAREN, "Expected (");
			Type type = typeLiteral();
			consume(RIGHT_PAREN, "Expected )");
			return new Type.SetType(type);
		}
		
		if(match(REF)) {
			consume(LEFT_PAREN, "Expected (");
			Type type = typeLiteral();
			consume(RIGHT_PAREN, "Expected )");
			return new Type.RefType(type);
		}
		
		/* struct ~ tuple */
		if(match(STRUCT)) {
			consume(LEFT_PAREN, "Expected (");
			List<Type> types = new ArrayList<>();
			do {
				types.add(typeLiteral());
			} while(match(COMMA));
			consume(RIGHT_PAREN, "Expected )");
			return new Type.StructType(types);
		}		

		/* map K->V */
		if(match(MAP)) {
			consume(LEFT_PAREN, "Expected (");
			Type key = typeLiteral();
			consume(COMMA, "Expected ,");
			Type val = typeLiteral();
			consume(RIGHT_PAREN, "Expected )");
			return new Type.MapType(key, val);
		}
		
		if(match(TYPE)) {
			if(match(LEFT_PAREN)) {
				advance();
			}
			List<String> interfaces = new ArrayList<>();
			do {
				interfaces.add(consume(TID, "expected type ID").lexeme);
			} while(match(COMMA));
			if(match(RIGHT_PAREN))
				advance();
			return new Type.TypeType(interfaces);
		}		

		return null;
	}
	
	private Type signature(String kind) {
		Expr pattern = patternDeclaration(kind, new HashMap<>());
		if(kind.equals(CLASS.name())) {
			ClassDef def = (ClassDef)pattern;
			return new Type.SignatureType(null, kind, def.params, def.interfaces, null);
		} else if(kind.equals(INTERFACE.name())) {
			InterfaceDef def = (InterfaceDef)pattern;
			return new Type.SignatureType(null, kind, def.params, def.interfaces, null);
		} else if (kind.equals(FUN.name())) {
			FuncDef def = (FuncDef)pattern;
			return new Type.SignatureType(null, kind, def.params, null, def.returnType);
		} else if(kind.equals(ITER.name())) {
			IterDef def = (IterDef)pattern;
			return new Type.SignatureType(null, kind, def.params, null, def.returnType);
		}
		return null;
	}

	private List<String> eval(Expr se) {
		List<String> ret = new ArrayList<>();
		if(se instanceof Expr.Set) {
			Expr.Set set = (Expr.Set)se;
			for(Expr expr : set.exprs) {
				if(expr instanceof Expr.Variable) {
					ret.add(((Expr.Variable)expr).name.lexeme);
				}
			}
		} else if(se instanceof Expr.Seq) {
			Expr.Seq seq = (Expr.Seq)se;
			for(Expr expr : seq.exprs) {
				if(expr instanceof Expr.Variable) {
					ret.add(((Expr.Variable)expr).name.lexeme);
				}
			}
		} else {
			Mu.runtimeError("Bad type %s", se.toString());
		}
		return ret;
	}
	
	private List<Expr> varDeclarations(Map<String, Object> attributes) {
		List<Expr> lst = new ArrayList<>();
		Token name = consume(ID, "Expect variable name.");

		Expr initializer = null;
		if (match(COLON)) {
			Type type = typeLiteral();
			if(type == null) {
				initializer = expression();
			} else {
				initializer = new Expr.TypeLiteral(null, type, attributes);
			}
		}
		lst.add(new Expr.Var(name, initializer, (boolean)attributes.getOrDefault("shared", false)));
		while (match(COMMA)) {
			name = consume(ID, "Expect variable name.");
			consume(COLON, "Expect initializer in multiple variable declaration");
			initializer = expression();
			lst.add(new Expr.Var(name, initializer, (boolean)attributes.getOrDefault("shared", false)));
		}
		return lst;
	}

	private List<Expr> valDeclarations(Map<String, Object> attributes) {
		List<Expr> lst = new ArrayList<>();
		Token name = consume(ID, "Expect variable name.");

		Expr initializer = null;
		if (match(COLON)) {
			initializer = expression();
		}
		
		if (initializer == null) {
			throw error(name, "val must have initializer: %s", name.lexeme);
		}
		
		lst.add(new Expr.Val(name, initializer, (boolean)attributes.getOrDefault("shared", false)));
		while (match(COMMA)) {
			name = consume(ID, "Expect variable name.");
			consume(COLON, "Expect initializer in multiple variable declaration");
			initializer = expression();
			lst.add(new Expr.Var(name, initializer, (boolean)attributes.getOrDefault("shared", false)));
		}
		// consume(SEMICOLON, "Expect ';' after variable declaration.");
		return lst;
	}

	/* STATEMENTS */
	private Expr statement() {
		if (match(FOR))
			return forStatement();
		if (match(PRINT))
			return printStatement();
		if (match(RETURN))
			return returnStatement();
		if (match(DO))
			return doWhileStatement();
		if (match(WHILE))
			return whileStatement();
		if (match(BREAK))
			return breakStatement();
		if (match(CONTINUE))
			return continueStatement();
		if (match(SELECT))
			return selectStatement();
		return expression();
	}

	private Expr printStatement() {
		Expr value = expression();
		// consume(SEMICOLON, "Expect ';' after value.");
		return new Expr.Print(value);
	}

	private Expr forStatement() {
		// for var i in range block
		Expr.Var var = null;
		if(match(VAR)) {
			var = (Expr.Var)varDeclarations(Map.of()).get(0);
		}
		consume(IN, "Expected IN");
		Expr.Range range = (Expr.Range) range();
		Expr.Block block = (Expr.Block) block();
		return new Expr.For(var, range, block);
	}

	private Expr ifExpression() {
		Expr condition = expression();
		Expr thenBranch = block();
		Expr elseBranch = null;
		if (match(ELSE)) {
			elseBranch = block();
		}
		return new Expr.If(condition, thenBranch, elseBranch);
	}

	private Expr returnStatement() {
		Token keyword = previous();
		Expr value = expression();
		return new Expr.Return(keyword, value);
	}

	private Expr doWhileStatement() {
		try {
			looplevel++;
			Expr.Block body = (Block) block();
			consume(WHILE, "Expect 'while' in a do-while loop.");
			//consume(LEFT_PAREN, "Expect '(' after 'while'.");
			Expr condition = expression();
			//consume(RIGHT_PAREN, "Expect ')' after condition.");
			//consume(SEMICOLON, "Expect ';' after do-while statement.");

			body.expressions.add(new Expr.While(condition, body));
			body = new Expr.Block(body.expressions);

			return body;
		} finally {
			looplevel--;
		}
	}

	private Expr whileStatement() {
		Expr condition = expression();

		try {
			looplevel++;
			Expr body = block();
			return new Expr.While(condition, body);
		} finally {
			looplevel--;
		}
	}

	private Expr breakStatement() {
		if (looplevel <= 0)
			throw error(previous(), "Break statement must be inside a loop.");
		consume(SEMICOLON, "Expect ';' after 'break' statement.");
		return new Expr.Break();
	}

	private Expr continueStatement() {
		if (looplevel <= 0)
			throw error(previous(), "Continue statement must be inside a loop.");
		consume(SEMICOLON, "Expect ';' after 'continue' statement.");
		return new Expr.Continue();
	}

	private Expr selectStatement() {
		// 	SELECT expr (
		//		WHEN expr (block)
		//		...
		//		ELSE (block)
		// 	)
		Expr condition = expression();
		consume(LEFT_PAREN, "Exepct '('");
		
		List<Expr> whenExpressions = new ArrayList<Expr>();
		List<Expr> whenBranches = new ArrayList<Expr>();
		
		while(match(WHEN)) {
			whenExpressions.add(expression());
			whenBranches.add(block());
		}
		Expr elseBranch = null;
		if (match(ELSE)) {
			elseBranch = block();
		}
		consume(RIGHT_PAREN, "Expect ')'");
		return new Expr.Select(condition, whenExpressions, whenBranches, elseBranch);
	}
	
	/* EXPRESSIONS */
	private Expr expression() {
		return range();
	}

	private Expr range() {
		Token prev = previous();
		Expr expr = mapping();
		if(match(DOTDOT)) {
			Expr to = mapping();
			boolean startIncl = prev.type == LEFT_BRK;
			boolean endIncl = false;
			if(match(RIGHT_PAREN)) {
				// 
			} else if(match(RIGHT_BRK)) {
				endIncl = true;
			}
			return new Expr.Range(expr, to, startIncl, endIncl);
			
		}
		return expr;
	}

	private Expr mapping() {
		Expr expr = assignment();
		if(match(COLON)) {
			Expr val = assignment();
			return new Expr.Mapping(((Expr.Variable)expr).name.lexeme, val);
		}
		return expr;
	}

	private Expr assignment() {

		Expr expr = conditional();

		if (match(ASSIGN, PLUSIS, MINIS, STARIS, SLASHIS, POWIS)) {
			Token op = previous();
			Expr value = assignment();
			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name;
				return new Expr.Assign(name, value, op);
			} else if (expr instanceof Expr.Getter) {
				Expr.Getter get = (Expr.Getter) expr;
				return new Expr.Setter(get.object, get.name, value);
			}
			error(op, "Invalid assignment target.");
		}
		return expr;
	}

	private Expr conditional() {
		if (match(IF))
			return ifExpression();
		return or();
	}

	private Expr or() {
		Expr expr = and();
		while (match(OR, XOR)) {
			Token operator = previous();
			Expr right = and();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr and() {
		Expr expr = equality();
		while (match(AND)) {
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr equality() {
		Expr expr = comparison();

		while (match(NOT_EQUAL, EQUAL, EQEQ)) {
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr comparison() {
		Expr expr = shift();

		while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr shift() {
		Expr expr = term();
		while (match(LEFTSHIFT, RIGHTSHIFT)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}
	
	private Expr term() {
		Expr expr = sqrt();
		while (match(MINUS, PLUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr sqrt() {
		if (match(SQRT)) {
			Token operator = previous();
			Expr right = factor();
			return new Expr.Unary(operator, right);
		}
		return factor();
	}
	
	private Expr factor() {
		Expr expr = unary();

		while (match(SLASH, STAR, PERCENT)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr unary() {
		if (match(NOT, MINUS, PLUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}
		if(match(BACKSLASH)) {
			Token operator = previous();
			Expr right = unary();
			match(BACKSLASH);
			return new Expr.Unary(operator, right);
		}
		return exponent();
	}

	private Expr exponent() {
		Expr expr = prefix();
		if (match(POW)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr prefix() {
		if (match(PLUSPLUS, MINMIN)) {
			Token operator = previous();
			Expr right = primary();
			return new Expr.Unary(operator, right);
		}
		return postfix();
	}

	private Expr postfix() {
		if (matchNext(PLUSPLUS, MINMIN, BANG)) {
			Token operator = peek();
			current--;
			Expr left = primary();
			advance();
			return new Expr.Postfix(left, operator);
		}
		return builtins();
	}

	private Expr builtins() {
		if (match(ABS)) {
			Token operator = previous();
			Expr right = selector();
			return new Expr.Unary(operator, right);			
		}
		Expr expr = selector();
		if(match(GCD, MAX, MIN)) {
			Token operator = previous();
			Expr right = selector();
			return new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr selector() {
		Expr expr = primary();
		while(match(DOT)) {
			Token name = consume(ID, "Expect property name after '.'.");
		    expr = new Expr.Getter(expr, name);
		}
		return expr;
	}
	
	private Expr primary() {
		if (match(FALSE))
			return new Expr.Literal(false);
		if (match(TRUE))
			return new Expr.Literal(true);
		if (match(NIL))
			return new Expr.Literal(null);
		if (match(THIS))
			return new Expr.This(previous());
		if (match(INF))
			return new Expr.Literal(Double.POSITIVE_INFINITY);
		if (match(NAN))
			return new Expr.Literal(Double.NaN);

		if (match(INT, REAL, STRING, CHAR)) {
			return new Expr.Literal(previous().literal);
		}
		if (match(ID)) {
			return new Expr.Variable(previous());
		}
		if (match(TID)) {
			return new Expr.Variable(previous());
		}
		
		/* aggregate literals here */
		
		if(match(LEFT_BRACE)) {
			/* SET */
			return set();
		}
		if(match(LEFT_BRK)) {
			/* LIST or RANGE */
			return list();
		}
		if (match(LEFT_PAREN)) {
			/* RANGE or STRUCT */
			return block();
		}

		// Lambda
		//if (check(FUN) && !checkNext(ID)) {
		//	advance();
		//	return functionBody();
		//}

		if (match(SEMICOLON)) {
			return new Expr.Literal(null);
		}
		if (match(EOF)) {
			return new Expr.Literal(null);
		}
		throw error(peek(), "Expect expression.");
	}
	
	private Expr list() {
		List<Expr> exprs = new ArrayList<>();
		if(match(RIGHT_BRK)) {
			/* empty list */
			return new Expr.Seq(exprs);
		}
		do {
			Expr expr = expression();
			if(expr instanceof Expr.Range) {
				/* it's a range [] or [) */
				match(RIGHT_BRK);
				match(RIGHT_PAREN);
				return expr;
			}
			exprs.add(expr);
			/* ignore newlines */
			match(SEMICOLON);
		} while(match(COMMA));
		consume(RIGHT_BRK, "Expected ']'");
		return new Expr.Seq(exprs);
	}

	private Expr set() {
		Set<Expr> exprs = new HashSet<>();
		do {
			exprs.add(expression());
			match(SEMICOLON);
		} while(match(COMMA));
		consume(RIGHT_BRACE, "Expected '}'");
		return new Expr.Set(exprs);
	}

	private Expr block() {
		/* if not consumed yet */
		match(LEFT_PAREN);
		boolean isMap = false;
		List<Expr> exprs = new ArrayList<>();
		while(!match(RIGHT_PAREN)) {
			exprs.addAll(declarations());
			Expr.Range rng = getRange(exprs);
			if(rng != null) {
				/* it's a range [] or [) */
				match(RIGHT_BRK);
				match(RIGHT_PAREN);
				return rng;
			}
			if(match(SEMICOLON)) {
				// it's a block 
			}
			if(match(COMMA)) {
				// it's a map/tuple/call
				isMap = true;
			}
		}
		//consume(RIGHT_PAREN, "Expect ')' after block.");
		if(isMap) return new Expr.Map(exprs);
		return new Expr.Block(exprs);
	}

	private Expr.Range getRange(List<Expr> statements) {
		for(Expr expr : statements) {
			if(expr instanceof Expr.Range) {
				return (Expr.Range)expr;
			}
		}
		return null;
	}

	/* Parser UTIL */
	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}
		return false;
	}

	private boolean matchNext(TokenType... types) {
		for (TokenType type : types) {
			if (checkNext(type)) {
				advance();
				return true;
			}
		}
		return false;
	}

	private Token consume(TokenType type, String message) {
		if (check(type))
			return advance();
		throw error(peek(), message);
	}

	private boolean check(TokenType tokenType) {
		if (isAtEnd())
			return false;
		return peek().type == tokenType;
	}

	private boolean checkNext(TokenType tokenType) {
		if (isAtEnd())
			return false;
		if (tokens.get(current + 1).type == EOF)
			return false;
		return tokens.get(current + 1).type == tokenType;
	}

	private Token advance() {
		if (!isAtEnd()) {
			current++;
		}
		return previous();
	}

	private boolean isAtEnd() {
		return peek().type == EOF;
	}

	private Token peek() {
		return tokens.get(current);
	}

	private Token previous() {
		return tokens.get(current - 1);
	}

	private ParseError error(Token token, String message, Object... args) {
		Mu.error(token, message, args);
		return new ParseError();
	}

	private void synchronize() {
		advance();
		while (!isAtEnd()) {
			if (previous().type == SEMICOLON) {
				return;
			}
			if(peek().type instanceof Keyword) {
				return;
			} else {
				advance();
			}
		}
	}
}