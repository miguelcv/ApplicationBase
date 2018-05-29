package org.mcv.mu;

import static org.mcv.mu.Keyword.*;
import static org.mcv.mu.Soperator.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mcv.mu.Expr.Block;
import org.mcv.mu.Expr.Dot;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Params.ParamFormal;
import org.mcv.mu.Scanner.ScannerError;

class Parser {

	private static final String VAL_MUST_HAVE_INITIALIZER = "val must have initializer: %s";
	private static final String BAD_TYPE = "Bad type %s";
	//private static final String INVALID_TEMPLATE_DECLARATION = "Invalid template declaration";
	private static final String CLASS_NAME_SHOULD_START_WITH_CAPITAL_LETTER = "Class name should start with capital letter";

	private static final String EXPECT_EXPRESSION = "Expect expression.";
	private static final String EXPECT_VARIABLE_NAME = "Expect variable name.";
	private static final String EXPECT_COMMA = "Expect ,";
	private static final String EXPECT_COLON = "Expect colon";
	private static final String EXPECT_RPAREN = "Expect )";
	private static final String EXPECT_LPAREN = "Expect (";
	private static final String EXPECT_TYPE = "Expect type";
	private static final String EXPECT_TYPE_NAME = "Expect type name.";
	private static final String EXPECT_NAME = "Expect name.";
	private static final Gensym lambdaGen = new Gensym("LAMBDA_");
	
	private final List<Token> tokens;
	@SuppressWarnings("unused")
	private Environment main;
	private int current = 0;
	private int looplevel = 0;

	static class ParserError extends ScannerError {
		private static final long serialVersionUID = 1L;
		protected transient Token tok;

		public ParserError(String msg) {
			super(msg);
		}

		public ParserError(String msg, Object... args) {
			super(String.format(msg, args));
		}

		public ParserError(Exception e) {
			super(e);
		}
	}

	Parser(List<Token> tokens, Environment main) {
		this.tokens = tokens;
		this.main = main;
	}

	List<Expr> parse() {
		List<Expr> statements = new ArrayList<>();
		while (!isAtEnd()) {
			try {
				Expr declaration = declaration();
				if (declaration != null) {
					statements.add(declaration);
				}
			} catch (Exception e) {
				ParserError pe = null;
				if(e instanceof ParserError) {
					pe = (ParserError)e;
				} else {
					pe = new ParserError(e);
				}
				pe.tok = tokens.get(current);
				pe.line = pe.tok.line;
				Mu.error(pe);
				if (pe.tok.type.equals(EOF)) {
					break;
				}
				synchronize();
			}
		}
		return statements;
	}

	/* DECLARATIONS */
	private Expr declaration() {
		Attributes attributes = new Attributes();
		while (isAttribute()) {
			attribute(attributes);
		}
		if (match(MODULE)) {
			return moduleDeclaration(attributes);
		} else if (match(TYPE)) {
			return typeDeclaration(attributes);
		} else if (match(CLASS)) {
			return templateDeclaration(CLASS.name().toLowerCase(), attributes, false);
		} else if (match(FUN)) {
			return templateDeclaration(FUN.name().toLowerCase(), attributes, false);
		} else if (match(VAR)) {
			return varDeclaration(attributes);
		} else if (match(VAL)) {
			return valDeclaration(attributes);
		} else {
			return statement();
		}
	}

	private boolean isAttribute() {
		try {
			Attribute.valueOf(peek().lexeme.toUpperCase());
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private void attribute(Attributes attributes) {
		Token attr = advance();
		if(attr.lexeme.equalsIgnoreCase(Attribute.DOC.name())) {
			if(match(LEFT_PAREN, LPAREN_CALL)) {
				Token lit = advance();
				if(match(RIGHT_PAREN)) {
					if(lit.type.equals(STRING)) {
						attributes.put("doc", lit.literal);
					}
				}
			}
			throw new ParserError("Bad docstring");
		}
		attributes.put(attr.lexeme, true);
	}

	private Expr templateDeclaration(String kind, Attributes attributes, boolean isTypedef) {
		// [attr ...] fun [ID] ( params ) [=> T]: ( block )
		// [attr ...] class [ID] ( params ): ( block )
		// TYPEDEF:
		// fun ( params ) [=> T]
		// class ( params )

		Token name = new Token(kind.equalsIgnoreCase("fun") ? ID : TID, 
				ListMap.gensym.nextSymbol(), "", tokens.get(current).line);

		Params params = null;
		Type returnType = null;
		Expr.Block body = null;

		if(!isTypedef) {
			if (kind.equalsIgnoreCase(FUN.name())) {
				if (check(ID)) {
					name = consume(ID, EXPECT_NAME);
				} else {
					// anonymous lambda
					name = new Token(ID, lambdaGen.nextSymbol(), "", tokens.get(current).line);
				}
			} else if (kind.equalsIgnoreCase(CLASS.name())) {
				if (check(TID)) {
					name = consume(TID, EXPECT_TYPE_NAME);
				} else if (check(ID)) {
					throw new ParserError(CLASS_NAME_SHOULD_START_WITH_CAPITAL_LETTER);
				} else {
					// anonymous class
					name = new Token(ID, ListMap.gensym.nextSymbol(), "", tokens.get(current).line);
				}
			}
		}

		if (match(LEFT_PAREN, LPAREN_CALL)) {
			// val params
			params = params();
		} else {
			throw new ParserError("Must supply formal parameter list");
		}

		if (match(ARROW)) {
			returnType = typeLiteral();
		}

		if(!isTypedef) {
			consume(COLON, String.format("Must supply a %s body after colon", kind.toLowerCase()));
			if (match(LEFT_PAREN)) {
					body = (Block) block();
			} else {
				throw new ParserError("Must supply a %s body", kind.toLowerCase());
			}
		}
		
		if (kind.equalsIgnoreCase(CLASS.name())) {
			return new Expr.TemplateDef(name, kind, params, Type.Any, body, attributes);
		} else if (kind.equalsIgnoreCase(FUN.name())) {
			// check if return type not null
			if (returnType == null) {
				returnType = Type.Void;
			}
			return new Expr.TemplateDef(name, kind, params, returnType, body, attributes);
		}
		return null;
	}

	private Params params() {
		// ( =>type name [: defval], ..., [*])
		Params params = new Params();
		do {
			/* empty parameter list!! */
			if (peek().type.equals(RIGHT_PAREN)) {
				break;
			}

			boolean typeParam = false;

			if (match(STAR)) {
				params.vararg = true;
				break;
			}

			Attributes attributes = new Attributes();

			while (isAttribute()) {
				attribute(attributes);
			}

			Type type = typeLiteral();
			if (type == null) {
				throw new ParserError(EXPECT_TYPE);
			}
			Token name = null;

			if (check(ID)) {
				name = consume(ID, EXPECT_NAME);
			} else if (type.name.equals("Type") && check(TID)) {
				typeParam = true;
				name = consume(ID, EXPECT_TYPE_NAME);
			} else {
				//throw new ParserError(EXPECT_NAME);
				// anonymous lambda 
				name = new Token(ID, ListMap.gensym.nextSymbol(), null, tokens.get(current).line);
			}

			Expr defval = null;
			if (match(COLON)) {
				// type
				if (typeParam) {
					defval = new Expr.TypeLiteral(null, typeLiteral(), new Attributes());
				} else {
					defval = expression();
				}
			}
			ParamFormal formal = new ParamFormal(name.lexeme, type, defval, attributes);
			params.add(formal);
		} while (match(COMMA));
		consume(RIGHT_PAREN, EXPECT_RPAREN);
		return params;
	}

	private Expr moduleDeclaration(Attributes attributes) {
		Expr.Dot qname = (Dot) selector();
		return new Expr.Module(((Expr.Variable) qname.next).name, attributes);
	}

	private Expr typeDeclaration(Attributes attributes) {
		Token name = consume(TID, EXPECT_TYPE_NAME);
		consume(COLON, EXPECT_COLON);
		return new Expr.TypeDef(name, typeLiteral(), attributes);
	}

	private Type typeLiteral() {

		Type left = type();

		/* union */
		if (match(OR)) {
			return new Type.UnionType(left, typeLiteral());
		}

		/* intersection */
		if (match(AND)) {
			return new Type.IntersectionType(left, typeLiteral());
		}

		return left;
	}

	private Type type() {
		/* signature */
		if (match(FUN, CLASS)) {
			return signature(previous().lexeme);
		}

		if (match(TID)) {
			/* simple type name or alias */
			return new Type(previous().lexeme);
		}

		if (match(ENUM)) {
			if (match(LEFT_BRACE)) {
				String name = tokens.get(current - 3).lexeme;
				return new Type.SetEnum(name, new HashSet<String>(eval(set())));
			} else {
				boolean hasBrk = false;
				if (match(LEFT_BRK))
					hasBrk = true;
				String name = tokens.get(current - 3).lexeme;
				return new Type.ListEnum(name, eval(list(hasBrk)));
			}

		}

		if (match(LIST)) {
			consume(LPAREN_CALL, EXPECT_LPAREN);
			Type type = typeLiteral();
			consume(RIGHT_PAREN, EXPECT_RPAREN);
			return new Type.ListType(type);
		}

		if (match(SET)) {
			consume(LPAREN_CALL, EXPECT_LPAREN);
			Type type = typeLiteral();
			consume(RIGHT_PAREN, EXPECT_RPAREN);
			return new Type.SetType(type);
		}

		if (match(REF)) {
			consume(LPAREN_CALL, EXPECT_LPAREN);
			Type type = typeLiteral();
			consume(RIGHT_PAREN, EXPECT_RPAREN);
			return new Type.RefType(type);
		}

		/* map K->V */
		if (match(MAP)) {
			consume(LPAREN_CALL, EXPECT_LPAREN);
			Type key = typeLiteral();
			consume(COMMA, EXPECT_COMMA);
			Type val = typeLiteral();
			consume(RIGHT_PAREN, EXPECT_RPAREN);
			return new Type.MapType(key, val);
		}

		return null;
	}

	private Type signature(String kind) {
		Expr tmpl = templateDeclaration(kind, new Attributes(), true);
		TemplateDef def = (TemplateDef) tmpl;
		return new Type.SignatureType(def);
	}

	private List<String> eval(Expr se) {
		List<String> ret = new ArrayList<>();
		if (se instanceof Expr.Set) {
			Expr.Set set = (Expr.Set) se;
			for (Expr expr : set.exprs) {
				if (expr instanceof Expr.Variable) {
					ret.add(((Expr.Variable) expr).name.lexeme);
				}
			}
		} else if (se instanceof Expr.Seq) {
			Expr.Seq seq = (Expr.Seq) se;
			for (Expr expr : seq.exprs) {
				if (expr instanceof Expr.Variable) {
					ret.add(((Expr.Variable) expr).name.lexeme);
				}
			}
		} else {
			throw new ParserError(String.format(BAD_TYPE, se.toString()));
		}
		return ret;
	}

	private Expr varDeclaration(Attributes attributes) {
		Token name = consume(ID, EXPECT_VARIABLE_NAME);

		Expr initializer = null;
		if (match(COLON)) {
			try {
				initializer = expression();
				if (initializer.type.name.equals(Type.Type.name))
					initializer = new Expr.TypeLiteral(name, initializer.type, attributes);
			} catch (Exception e) {
				Type type = typeLiteral();
				initializer = new Expr.TypeLiteral(name, type, attributes);
			}
		}
		if (match(ASSIGN, EQUAL)) {
			throw new ParserError("Cannot use assignment or = as initializer (use : instead)");
		}
		return new Expr.Var(name, initializer, attributes);
	}

	private Expr valDeclaration(Attributes attributes) {
		Token name = consume(ID, EXPECT_VARIABLE_NAME);

		Expr initializer = null;
		if (match(COLON)) {
			initializer = expression();
		}

		if (initializer == null) {
			throw new ParserError(String.format(VAL_MUST_HAVE_INITIALIZER, name.lexeme));
		}

		return new Expr.Val(name, initializer, attributes);
	}

	/* STATEMENTS */
	private Expr statement() {
		if (match(PRINT))
			return printStatement(previous());
		if (match(TYPEOF))
			return printTypeStatement(previous());
		if (match(RETURN))
			return returnStatement(previous());
		if (match(BREAK))
			return breakStatement(previous());
		if (match(CONTINUE))
			return continueStatement(previous());
		if (match(AROUND, BEFORE, AFTER, ERROR, ALWAYS))
			return aopStatement(previous());
		return expression();
	}

	private Expr printStatement(Token name) {
		Expr value = expression();
		return new Expr.Print(name, value);
	}

	private Expr printTypeStatement(Token name) {
		Expr value = declaration();
		return new Expr.PrintType(name, value);
	}

	private Expr returnStatement(Token token) {
		Expr value = declaration();
		return new Expr.Return(token, value);
	}

	private Expr breakStatement(Token name) {
		if (looplevel <= 0)
			throw new ParserError("Break statement must be inside a loop.");
		match(SEMICOLON);
		return new Expr.Break(name);
	}

	private Expr continueStatement(Token name) {
		if (looplevel <= 0)
			throw new ParserError("Continue statement must be inside a loop.");
		match(SEMICOLON);
		return new Expr.Continue(name);
	}
	
	/* AOP */
	private Expr aopStatement(Token tok) {
		/* before <callable> : <block> */
		if (!callable()) {
			throw new ParserError("Argument to AOP statement must be callable");
		}
		Expr expr = expression();
		consume(COLON, EXPECT_COLON);
		Block block = (Block)block();
		return new Expr.Aop(tok, expr, block);
	}

	/* EXPRESSIONS */
	private Expr expression() {
		return range();
	}

	private Expr range() {
		Token prev = previous();
		Expr expr = mapping(prev);
		if (prev.type.equals(LEFT_BRK) || prev.type.equals(LEFT_PAREN) || prev.type.equals(LPAREN_CALL)) {
			if (match(DOTDOT)) {
				Expr to = mapping(previous());
				boolean startIncl = prev.type == LEFT_BRK;
				boolean endIncl = false;
				if (match(RIGHT_PAREN)) {
					//
				} else if (match(RIGHT_BRK)) {
					endIncl = true;
				}
				return new Expr.Range(prev, expr, to, startIncl, endIncl);
			}
		}
		return expr;
	}

	private Expr mapping(Token name) {
		Expr expr = assignment();
		if (match(ARROW)) {
			Expr val = assignment();
			return new Expr.Mapping(name, ((Expr.Variable) expr).name.lexeme, val, true);
		}
		if (match(COLON)) {
			Expr val = assignment();
			return new Expr.Mapping(name, ((Expr.Variable) expr).name.lexeme, val, false);
		}
		return expr;
	}

	private Expr assignment() {

		Expr expr = conditionOrLoop();

		if (match(ASSIGN, PLUSIS, MINIS, STARIS, SLASHIS, POWIS, PERCENTIS, ANDIS, ORIS, LSHIFTIS, RSHIFTIS)) {
			Token op = previous();
			Expr value = assignment();
			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name;
				return new Expr.Assign(name, value, op);
			} else if (expr instanceof Expr.Dot) {
				Expr.Dot get = (Expr.Dot) expr;
				get.value = value;
				return get;
			} else if (expr instanceof Expr.Subscript) {
				Expr.Subscript sub = (Expr.Subscript) expr;
				sub.value = value;
				return sub;
			}
			throw new ParserError("Invalid assignment target.");
		}
		return expr;
	}

	private Expr conditionOrLoop() {
		if (match(IF))
			return ifExpression(previous());
		if (match(UNLESS))
			return ifExpression(previous());
		if (match(FOR))
			return forExpression(previous());
		if (match(DO))
			return doWhileExpression(previous());
		if (match(WHILE))
			return whileExpression(previous());
		if (match(UNTIL))
			return whileExpression(previous());
		if (match(SELECT))
			return selectExpression(previous());
		return or();
	}

	private Expr forExpression(Token name) {
		// for var i in range block
		Expr.Var var = null;
		if (match(VAR)) {
			// optional
		}
		var = (Expr.Var) varDeclaration(new Attributes());
		consume(IN, "EXPECT IN");
		// range or list or set or map
		Expr range = null;

		if (match(ID)) {
			range = new Expr.Variable(previous());
		} else if (check(LEFT_PAREN)) {
			range = range();
		} else if (check(LEFT_BRACE)) {
			range = set();
		} else if (check(LEFT_BRK)) {
			range = list();
		}
		Expr.Block block = (Expr.Block) block();
		Expr.Block atEnd = null;
		if (match(AFTER)) {
			atEnd = (Block) block();
		}
		return new Expr.For(name, var, range, block, atEnd);
	}

	private Expr ifExpression(Token name) {
		Expr condition = expression();
		Expr thenBranch = block();
		Expr elseBranch = null;
		if (match(ELSE)) {
			elseBranch = block();
		}
		return new Expr.If(name, condition, thenBranch, elseBranch);
	}

	private Expr doWhileExpression(Token name) {
		try {
			looplevel++;
			Expr.Block body = (Block) block();
			consume(WHILE, "Expect 'while' in a do-while loop.");
			Expr condition = expression();
			body.expressions.add(new Expr.While(name, condition, body, null));
			return new Expr.Block(name, body.expressions);
		} catch(Exception e) {
			throw new ParserError("Unexpected error");
		} finally {
			looplevel--;
		}
	}

	private Expr whileExpression(Token name) {
		Expr condition = expression();
		try {
			looplevel++;
			Expr body = block();
			Expr atEnd = null;
			if (match(AFTER)) {
				atEnd = block();
			}
			return new Expr.While(name, condition, body, atEnd);
		} catch(Exception e) {
			throw new ParserError("Unexpected error");
		} finally {
			looplevel--;
		}
	}

	private Expr selectExpression(Token name) {
		Expr condition = expression();
		consume(LEFT_PAREN, EXPECT_LPAREN);

		List<Expr> whenExpressions = new ArrayList<>();
		List<Expr> whenBranches = new ArrayList<>();

		match(SEMICOLON);
		while (match(WHEN)) {
			whenExpressions.add(expression());
			whenBranches.add(block());
			match(SEMICOLON);
		}
		Expr elseBranch = null;
		if (match(ELSE)) {
			elseBranch = block();
			match(SEMICOLON);
		}
		consume(RIGHT_PAREN, EXPECT_RPAREN);
		return new Expr.Select(name, condition, whenExpressions, whenBranches, elseBranch);
	}

	private Expr or() {
		Expr expr = and();
		while (match(OR)) {
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

		while (match(NOT_EQUAL, EQUAL, EQEQ, NEQEQ)) {
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
		if (match(BACKSLASH)) {
			Token operator = previous();
			Expr right = unary();
			match(BACKSLASH);
			return new Expr.Unary(operator, right);
		}
		return exponent();
	}

	private Expr exponent() {
		Expr expr = prefix();
		if (match(POW, XOR)) {
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
		if (match(GCD, MAX, MIN)) {
			Token operator = previous();
			Expr right = selector();
			return new Expr.Binary(expr, operator, right);
		}
		return expr;
	}

	private Expr selector() {
		/* aggregate literals here */

		Expr last = primary();

/*		while (match(LEFT_BRK)) {
			last = maybeSubscript(last);
		}
		while (match(LPAREN_CALL)) {
			last = maybeCall(last);
		}
		while (match(DOT)) {
			Token name = consume(ID, "Expect property name after '.'.");
			last = new Expr.Dot(last, new Expr.Variable(name));
		}
*/
		while (match(LEFT_BRK, LPAREN_CALL, DOT)) {
			if(previous().type.equals(LEFT_BRK)) {
				/* LIST or RANGE or SUBSCRIPT */
				last = maybeSubscript(last);
			}
			if(previous().type.equals(LPAREN_CALL)) {
				/* CALL */
				last = maybeCall(last);
			}
			if(previous().type.equals(DOT)) {
				/* DOT */
				Token name = consume(ID, "Expect property name after '.'.");
				last = new Expr.Dot(last, new Expr.Variable(name));
			}
		}

		return last;
	}

	private Expr primary() {
		if (match(FALSE))
			return new Expr.Literal(previous(), false);
		if (match(TRUE))
			return new Expr.Literal(previous(), true);
		if (match(NIL))
			return new Expr.Literal(previous(), null);
		if (match(INF))
			return new Expr.Literal(previous(), Double.POSITIVE_INFINITY);
		if (match(NAN))
			return new Expr.Literal(previous(), Double.NaN);

		if (match(INT, REAL, STRING, CHAR)) {
			return new Expr.Literal(previous(), previous().literal);
		}
		if (match(RSTRING)) {
			return new Expr.Literal(previous(), new RString(previous().literal.toString()));
		}

		if (match(ID)) {
			return new Expr.Variable(previous());
		}
		if (match(TID)) {
			return new Expr.Variable(previous());
		}

		if (match(EMPTY_SET)) {
			return new Expr.Set(previous(), new HashSet<Expr>());
		}

		if (match(LEFT_BRACE)) {
			/* SET */
			return set();
		}

		if (match(LEFT_BRK)) {
			/* LIST or RANGE literal */
			return list();
		}
		if (match(LEFT_PAREN)) {
			/* expression or map literal */
			return block();
		}

		if (match(SEMICOLON)) {
			return null;
		}
		if (match(EOF)) {
			return null;
		}

		throw new ParserError(EXPECT_EXPRESSION);
	}

	private Expr list() {
		return list(true);
	}

	private Expr list(boolean expectRB) {
		List<Expr> exprs = new ArrayList<>();
		if (match(RIGHT_BRK)) {
			/* empty list */
			return new Expr.Seq(previous(), exprs);
		}
		do {
			Expr expr = null;
			while (match(SEMICOLON)) {
				/* ignore */}
			expr = expression();
			if (expr instanceof Expr.Range) {
				/* it's a range [] or [) */
				match(RIGHT_BRK);
				match(RIGHT_PAREN);
				return expr;
			}
			exprs.add(expr);
			/* ignore newlines */
			match(SEMICOLON);
		} while (match(COMMA));
		if (expectRB)
			consume(RIGHT_BRK, "EXPECT ']'");
		return new Expr.Seq(previous(), exprs);
	}

	private Expr set() {
		Set<Expr> exprs = new HashSet<>();
		if (match(RIGHT_BRACE)) {
			/* empty set */
			return new Expr.Set(previous(), exprs);
		}
		do {
			Expr expr = null;
			while (match(SEMICOLON)) {
				/* ignore */}
			expr = expression();
			exprs.add(expr);
			match(SEMICOLON);
		} while (match(COMMA));
		consume(RIGHT_BRACE, "EXPECT '}'");
		return new Expr.Set(previous(), exprs);
	}

	private Expr block() {
		Token tok = previous();
		/* if not consumed yet */
		match(LEFT_PAREN);
		boolean isMap = false;
		List<Expr> exprs = new ArrayList<>();
		while (!match(RIGHT_PAREN)) {
			while(match(SEMICOLON)) { /* ignore */ }
			Expr decl = declaration();
			if(decl == null) continue;
			exprs.add(decl);
			if (exprs.size() == 1) {
				Expr.Range rng = getRange(exprs);
				if (rng != null) {
					/* it's a range [] or [) */
					match(RIGHT_BRK);
					match(RIGHT_PAREN);
					return rng;
				}
			}
			if (match(SEMICOLON)) {
				// it's a block
			}
			if (match(COMMA)) {
				// it's a map/tuple/call
				isMap = true;
			}
		}
		match(SEMICOLON);
		if (isMap) {
			return new Expr.Map(tok, exprs);
		}
		return new Expr.Block(tok, exprs);
	}

	private Expr maybeCall(Expr last) {
		Token tok = previous();
		if (!callable())
			return last;
		List<Expr> exprs = new ArrayList<>();
		while (!match(RIGHT_PAREN)) {
			Expr expr = declaration();
			if (expr == null)
				continue;
			exprs.add(expr);
			match(COMMA);
			match(SEMICOLON);
		}
		return new Expr.Call(last, new Expr.Map(tok, exprs));
	}

	private boolean callable() {
		for (int i = current; i < tokens.size(); i++) {
			Token tok = tokens.get(i);
			if (tok.type.equals(RIGHT_PAREN)) {
				return true;
			}
			// if bracketed, ignore
			if (isOpenBracket(tok)) {
				for (; i < tokens.size(); i++) {
					if (!isCloseBracket(tokens.get(i))) {
						break;
					}
				}
			}
			// if newline not after comma => block
			if (tok.type.equals(SEMICOLON)) {
				if (!tokens.get(i - 1).type.equals(COMMA)) {
					return false;
				}
			}
			// if ARROW => map
			if (tok.type.equals(ARROW)) {
				return false;
			}
			// if DOTDOT => range
			if (tok.type.equals(DOTDOT)) {
				return false;
			}
		}
		return false;
	}

	private boolean isOpenBracket(Token tok) {
		return tok.type.equals(LEFT_BRACE) || tok.type.equals(LEFT_BRK) || tok.type.equals(LEFT_PAREN);
	}

	private boolean isCloseBracket(Token tok) {
		return tok.type.equals(RIGHT_BRACE) || tok.type.equals(RIGHT_BRK) || tok.type.equals(RIGHT_PAREN);
	}

	private Expr maybeSubscript(Expr last) {
		if (!indexable())
			return last;
		List<Expr> exprs = new ArrayList<>();
		Expr expr = null;
		while ((expr = expression()) == null) {
			/* ignore */ }

		if (expr instanceof Expr.Range) {
			/* it's a range [] or [) */
			match(RIGHT_BRK);
			match(RIGHT_PAREN);
			return new Expr.Subscript(last, expr);
		}
		exprs.add(expr);
		/* ignore newlines */
		match(SEMICOLON);
		consume(RIGHT_BRK, "EXPECT ']'");
		return new Expr.Subscript(last, new Expr.Seq(previous(), exprs));
	}

	private boolean indexable() {
		for (int i = current; i < tokens.size(); i++) {
			Token tok = tokens.get(i);
			if (tok.type.equals(RIGHT_BRK)) {
				return true;
			}
			if (tok.type.equals(COMMA)) {
				return false;
			}
			// if bracketed, ignore
			if (isOpenBracket(tok)) {
				for (; i < tokens.size(); i++) {
					if (!isCloseBracket(tokens.get(i))) {
						break;
					}
				}
			}
		}
		return false;
	}

	private Expr.Range getRange(List<Expr> exprs) {
		if (exprs.get(0) instanceof Expr.Range) {
			return (Expr.Range) exprs.get(0);
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
		throw new ParserError(message);
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
		if (current < tokens.size())
			return tokens.get(current);
		return new Token(EOF, "", null, 0);
	}

	private Token previous() {
		if (current > 0) {
			return tokens.get(current - 1);
		}
		return tokens.get(0);
	}

	private void synchronize() {
		advance();
		while (!isAtEnd()) {
			if (previous().type == SEMICOLON) {
				return;
			}
			if (peek().type instanceof Keyword) {
				return;
			} else {
				advance();
			}
		}
	}
}