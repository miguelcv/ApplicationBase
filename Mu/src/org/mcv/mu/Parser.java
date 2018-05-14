package org.mcv.mu;

import static org.mcv.mu.Soperator.*;
import static org.mcv.mu.Keyword.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mcv.mu.Expr.Block;

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
		try {
			if(match(MODULE)) {
				lst.add(moduleDeclaration());
			} else if(match(IMPORT)) {
				lst.add(importDeclaration());
			} else if (match(TYPE)) {
				lst.add(typeDeclaration());
			} else if (check(FUN) && checkNext(ID)) {
				consume(FUN, null);
				lst.add(function("function"));
			} else if (match(VAR)) {
				lst.addAll(varDeclarations());
			} else if (match(VAL)) {
				lst.addAll(valDeclarations());
			} else {
				lst.add(statement());
			}
			return lst;
		} catch (Exception error) {
			synchronize();
			return null;
		}
	}

	private Expr moduleDeclaration() {
		Token name = consume(ID, "Expect module name.");
		return new Expr.Module(name);
	}

	private Expr importDeclaration() {
		// TODO
		return null;
	}

	// TODO
	private Expr typeDeclaration() {
		Token name = consume(ID, "Expect type name.");
		List<Expr.Function> methods = new ArrayList<>();
		List<Expr.Function> classMethods = new ArrayList<>();
		consume(LEFT_PAREN, "Expect '(' before type body.");
		while (!check(RIGHT_PAREN) && !isAtEnd()) {
			// boolean isClassMethod = match(CLASS);
			// (isClassMethod ? classMethods : methods).add(function("method"));
		}
		consume(RIGHT_PAREN, "Expect ')' after type body.");
		return new Expr.ClassDef(name, null, methods, classMethods);
	}

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
		return expression();
	}

	private List<Expr> varDeclarations() {
		List<Expr> lst = new ArrayList<>();
		Token name = consume(ID, "Expect variable name.");

		Expr initializer = null;
		if (match(COLON)) {
			initializer = expression();
		}
		lst.add(new Expr.Var(name, initializer));
		while (match(COMMA)) {
			name = consume(ID, "Expect variable name.");
			consume(COLON, "Expect initializer in multiple variable declaration");
			initializer = expression();
			lst.add(new Expr.Var(name, initializer));
		}
		// consume(SEMICOLON, "Expect ';' after variable declaration.");
		return lst;
	}

	private List<Expr> valDeclarations() {
		List<Expr> lst = new ArrayList<>();
		Token name = consume(ID, "Expect variable name.");

		Expr initializer = null;
		if (match(COLON)) {
			initializer = expression();
		}
		
		if (initializer == null) {
			throw error(name, "val must have initializer: %s", name.lexeme);
		}
		
		lst.add(new Expr.Val(name, initializer));
		while (match(COMMA)) {
			name = consume(ID, "Expect variable name.");
			consume(COLON, "Expect initializer in multiple variable declaration");
			initializer = expression();
			lst.add(new Expr.Var(name, initializer));
		}
		// consume(SEMICOLON, "Expect ';' after variable declaration.");
		return lst;
	}

	private Expr.Function function(String kind) {
		// TODO
		//Token name = consume(ID, "Expect " + kind + " name.");
		// params ??
		// body ??
		// type ??
		return new Expr.Function(null, null, "None");
		//name, functionBody(kind));
	}

	private Expr.Function functionBody() {
		List<Token> parameters = null;
		if (check(LEFT_PAREN)) {
			consume(LEFT_PAREN, "Expect parameter object");
			parameters = new ArrayList<>();
			if (!check(RIGHT_PAREN)) {
				do {
					parameters.add(consume(ID, "Expect parameter name"));
				} while (match(COMMA));
			}
		}
		consume(RIGHT_PAREN, "Expect ')' after parameters");
		Block blk = (Block) block();
		List<Expr> body = blk.expressions;
		return new Expr.Function(parameters, body, blk.last.type);
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
			var = (Expr.Var)varDeclarations().get(0);
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

		while (match(NOT_EQUAL, EQUAL)) {
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

	// check
	private Expr builtins() {
		if (match(ABS)) {
			Token operator = previous();
			Expr right = primary();
			return new Expr.Unary(operator, right);			
		}
		Expr expr = primary();
		if(match(GCD, MAX, MIN)) {
			Token operator = previous();
			Expr right = primary();
			return new Expr.Binary(expr, operator, right);
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

		if (match(INT, REAL, STRING, CHAR)) {
			return new Expr.Literal(previous().literal);
		}
		if (match(ID)) {
			return new Expr.Variable(previous());
		}
		if (match(TID)) {
			return new Expr.Type(previous());
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
		if (check(FUN) && !checkNext(ID)) {
			advance();
			return functionBody();
		}

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