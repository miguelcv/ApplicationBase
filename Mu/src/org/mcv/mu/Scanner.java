package org.mcv.mu;

import static org.mcv.mu.Keyword.*;
import static org.mcv.mu.Soperator.*;
import org.mcv.math.BigInteger;
import org.mcv.uom.Bag;
import org.mcv.uom.Unit;
import org.mcv.uom.UnitValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

class Scanner {
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0;
	private int current = 0;
	private int line = 1;
	private static final int BOM = 0xFEFF;
	private int maxTerminator;
	private Mu handler;
	
	static class ScannerError extends RuntimeException {
		private static final long serialVersionUID = 1L;
		protected int cp;
		protected int line;

		public ScannerError(String msg) {
			super(msg);
		}
		
		public ScannerError(Exception e) {
			super(e);
		}
	}

	Scanner(String source, Environment main, Mu mu) {
		this.source = source;
		this.handler = mu;
		maxTerminator = Integer.parseInt(Mu.cfg.getString("maxRawStringTerminator", "8"));
	}

	List<Token> scanTokens() {
		try {
			while (!isAtEnd()) {
				start = current;
				try {
					scanToken();
				} catch(Exception e) {
					ScannerError se = new ScannerError(e);
					se.cp = isAtEnd() ? 0 : source.codePointAt(current);
					se.line = line;
					handler.error(se);
				}
			}
			tokens.add(new Token(EOF, "", null, line));
			python();
			return postprocess(tokens);
		} catch (ScannerError e) {
			e.cp = source.codePointAt(current);
			e.line = line;
			handler.error(e);
			return new ArrayList<>();
		}
	}

	private List<Token> postprocess(List<Token> tokens) {
		List<Token> sub = new ArrayList<>();
		int ix;
		
		/* simplify redundant combinations:
		 * ( NL IND		(;
		 * NL DED )		;)
		*/
		sub.clear();
		sub.add(new Token(LEFT_PAREN));
		sub.add(new Token(NEWLINE));
		sub.add(new Token(INDENT));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.remove(ix + 2);
		}
		
		sub.clear();
		sub.add(new Token(NEWLINE));
		sub.add(new Token(DEDENT));
		sub.add(new Token(RIGHT_PAREN));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.remove(ix + 1);
		}

		/* 
		 * [ NL IND		[;
		 * NL DED ]		;]
		 */
		sub.clear();
		sub.add(new Token(LEFT_BRK));
		sub.add(new Token(NEWLINE));
		sub.add(new Token(INDENT));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.remove(ix + 2);
		}
		
		sub.clear();
		sub.add(new Token(NEWLINE));
		sub.add(new Token(DEDENT));
		sub.add(new Token(RIGHT_BRK));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.remove(ix + 1);
		}

		/*
		 * { NL IND		{;
		 * NL DED }		;}
		 */
		sub.clear();
		sub.add(new Token(LEFT_BRACE));
		sub.add(new Token(NEWLINE));
		sub.add(new Token(INDENT));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.remove(ix + 2);
		}
		
		sub.clear();
		sub.add(new Token(NEWLINE));
		sub.add(new Token(DEDENT));
		sub.add(new Token(RIGHT_BRACE));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.remove(ix + 1);
		}

		/* replace all NL INDENT with (; */	
		sub.clear();
		sub.add(new Token(NEWLINE));
		sub.add(new Token(INDENT));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.get(ix).type = LEFT_PAREN;
			tokens.get(ix+1).type = SEMICOLON;
		}
		
		/*
		 * ; NL			;
		 */
		sub.clear();
		sub.add(new Token(SEMICOLON));
		sub.add(new Token(NEWLINE));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.remove(ix + 1);
		}
		
		/* replace all NEWLINE with SEMICOLON */
		sub.clear();
		sub.add(new Token(NEWLINE));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.get(ix).type = SEMICOLON;
		}
		
		/* replace multiple semicolons */
		sub.clear();
		sub.add(new Token(SEMICOLON));
		sub.add(new Token(SEMICOLON));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.remove(ix);
		}

		/* replace all INDENT with ( */
		sub.clear();
		sub.add(new Token(INDENT));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.get(ix).type = LEFT_PAREN;
		}

		/* replace all DEDENT with ) */
		sub.clear();
		sub.add(new Token(DEDENT));
		while((ix = Collections.indexOfSubList(tokens, sub)) >= 0) {
			tokens.get(ix).type = RIGHT_PAREN;
		}
		
		return tokens;
	}

	private boolean isAtEnd() {
		return current >= source.length();
	}

	private void scanToken() {
		int c = advance();
		switch (c) {
		case BOM:
			// ignore BOM
			break;
		case ' ':
		case '\r':
		case '\t':
			if (match('('))
				addToken(LEFT_PAREN);
			/* else ignore */
			break;
		case '\n':
			addToken(NEWLINE);
			if (match('('))
				addToken(LEFT_PAREN);
			line++;
			if(peek() != '\n') python();
			break;
		case ';':
			addToken(SEMICOLON);
			break;
		case '(':
			if (current == 1) {
				/* at beginning of file */
				addToken(LEFT_PAREN);
			} else {
				addToken(LPAREN_CALL);
			}
			break;
		case ')':
			addToken(RIGHT_PAREN);
			break;
		case '{':
			addToken(LEFT_BRACE);
			break;
		case '}':
			addToken(RIGHT_BRACE);
			break;
		case '[':
			addToken(LEFT_BRK);
			break;
		case ']':
			addToken(RIGHT_BRK);
			break;
		case '`':
			++start;
			identifier(true);
			break;
		case '\'':
			charlit();
			break;
		case '"':
			string();
			break;
		default:
			maybeOperator(c);
			break;
		}
	}
	
	void maybeOperator(int c) {
		TokenType tok = Soperator.NONE;
		
		switch(c) {
		/* potential operator symbols: */
		case '$':
			if(tokstk.isEmpty()) identifier();
			else tok = DOLLAR;
			break;
		case '_':
			if(tokstk.isEmpty()) identifier();
			else tok = UNDERSCORE;
			break;
		case '@':
			tok = ATSIGN;
			break;
		case '#':
			tok = SHARP;
			break;
		case '\\':
			tok = BACKSLASH;
			break;
		case '?':
			tok = match('.') ? SAFENAV : match('[') ? SAFESUB : match('(') ? SAFECALL : QUESTION;
			break;
		case ',':
			tok = COMMA;
			break;
		case '.':
			tok = match('.') ? DOTDOT : DOT;
			break;
		case ':':
			tok = match('=') ? ASSIGN : match('^') ? UPARROW : COLON;
			break;
		case '←':
			tok = ASSIGN;
			break;
		case '→':
			tok = ARROW;
			break;
		case '↑':
			tok = UPARROW;
			break;
		case 'Ø':
		case '∅':
			tok = EMPTY_SET;
			break;
		case '∈':
			tok = IN;
			break;
		case '∞':
			tok = INF;
			break;
		case '÷':
			tok = match('=') ? SLASHIS : SLASH;
			break;
		case '/':
			if (match('/')) {
				/* C++ comment */
				while (peek() != '\n' && !isAtEnd())
					advance();
			} else if (match('*')) {
				/* C Comment */
				ccomment();
			} else {
				tok = match('=') ? SLASHIS : SLASH;
			}
			break;
		case '%':
			tok = match('=') ? PERCENTIS : PERCENT;
			break;
		case '^':
			tok = match('=') ? POWIS : POW;
			break;
		case '⊕':
		case '⊻':
			tok = match('=') ? XORIS : XOR;
			break;
		case '!':
			tok = BANG;
			break;
		case '-':
			tok = match('>') ? ARROW : match('-') ? MINMIN : match('=') ? MINIS : MINUS;
			break;
		case '+':
			tok = match('+') ? PLUSPLUS : match('=') ? PLUSIS : PLUS;
			break;
		case '&':
			tok = match('=') ? ANDIS : AND;
			break;
		case '|':
			tok = match('=') ? ORIS : OR;
			break;
		case '*':
		case '×':
			tok = match('=') ? STARIS : STAR;
			break;
		case '~':
		case '¬':
			tok = match('=') ? (match('=') ? NEQEQ : NOT_EQUAL) : NOT;
			break;
		case '√':
			tok = SQRT;
			break;
		/* RELOPS */
		case '=':
			tok = match('>') ? ARROW : match('=') ? EQEQ : EQUAL;
			break;
		case '<':
			tok = match('=') ? LESS_EQUAL
					: match('-') ? ASSIGN : match('<') ? (match('=') ? LSHIFTIS : LEFTSHIFT) : LESS;
			break;
		case '≤':
			tok = LESS_EQUAL;
			break;
		case '>':
			tok = match('=') ? GREATER_EQUAL : match('>') ? (match('=') ? RSHIFTIS : RIGHTSHIFT) : GREATER;
			break;
		case '≥':
			tok = GREATER_EQUAL;
			break;

		default:
			if(isAlpha(c)) {
				if(tokstk.isEmpty()) {
					identifier();
				} else {
					tok = OPERATOR;
				}
			} else if (tokstk.isEmpty() && isDigit(c)) {
				if(tokstk.isEmpty()) {
					number();
				} else {
					tok = OPERATOR;
				}
			} else if (isOperatorStart(c)) {
				tok = OPERATOR;
			} else {
				System.err.println("Unexpected character " + (char)c);
			}
			break;
		}
		if(tok.equals(Soperator.NONE)) {
			// comments, identifiers, numbers
			return;
		}
		// else have provisional token
		// see if next token is operator token
		boolean allowAscii = false;
		if(tokstk.isEmpty()) allowAscii = tok.equals(COLON);
		else allowAscii = tokstk.get(0).equals(COLON);
		if(isOperatorNext(peek(), allowAscii)) {
			tokstk.push(tok);
			maybeOperator(advance());
		} else {
			if(tokstk.isEmpty()) {
				addToken(tok);
			} else {
				tokstk.clear();
				addToken(OPERATOR);
			}
		}
	}

	private boolean isOperatorNext(int c, boolean allowAscii) {
		if(!allowAscii && c < 128) return false;
		String asciiOps = "~!@#$%^&*-_=+;:\\|<,>.?/";
		if(allowAscii && asciiOps.indexOf((char)c) >= 0) return true;
		if(allowAscii && Character.isAlphabetic(c)) return true;
		int type = Character.getType(c);
		return type == Character.MATH_SYMBOL ||
				type == Character.CURRENCY_SYMBOL ||
				type == Character.MODIFIER_SYMBOL ||
				type == Character.OTHER_SYMBOL;
	}

	private boolean isOperatorStart(int c) {
		int type = Character.getType(c);
		return type == Character.MATH_SYMBOL ||
				type == Character.CURRENCY_SYMBOL ||
				type == Character.MODIFIER_SYMBOL ||
				type == Character.OTHER_SYMBOL;
	}

	Stack<TokenType> tokstk = new Stack<>();
	Stack<Integer> pystk;

	private void python() {
		if (pystk == null) {
			pystk = new Stack<>();
			pystk.push(0);
		}
		int indent = calcIndent();
		if (indent > pystk.peek()) {
			pystk.push(indent);
			addToken(INDENT);
		} else if (indent < pystk.peek()) {
			while (pystk.peek() > indent) {
				pystk.pop();
				addToken(DEDENT);
			}
			if (pystk.peek() != indent) {
				throw new ScannerError("Incorrect indentation");
			}
		}
	}

	private int calcIndent() {
		if (isAtEnd())
			return 0;
		int ret = 0;
		int spacesToATab = Integer.parseInt(Mu.cfg.getString("spacesToATab", "4"));
		for (int i = current; i < source.length(); i++) {
			int c = source.codePointAt(i);
			if (c == '\t') {
				ret = (((ret + spacesToATab) / spacesToATab) * spacesToATab);
			} else if (c == ' ') {
				++ret;
			} else {
				break;
			}
		}
		return ret;
	}

	private void ccomment() {
		boolean terminated = false;
		advance();
		int nesting = 1;
		loop: while (true) {
			switch (peek()) {
			case '*':
				if (peekNext() == '/') {
					--nesting;
					if (nesting == 0) {
						advance();
						advance();
						terminated = true;
						break loop;
					}
				}
				break;
			case '/':
				if (peekNext() == '*') {
					advance();
					advance();
					++nesting;
				}
				break;
			case '\n':
				++line;
				break;
			default:
				if (isAtEnd())
					break loop;
			}
			advance();
		}
		if(!terminated) {
			throw new ScannerError("Unterminated comment");
		}
	}

	private int advance() {
		current++;
		return source.codePointAt(current - 1);
	}

	private void addToken(TokenType type) {
		addToken(type, null);
	}

	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line));
	}

	private void addFullToken(TokenType type, String text) {
		tokens.add(new Token(type, text, text, line));
	}

	private boolean match(int expected) {
		if (isAtEnd())
			return false;
		if (source.codePointAt(current) != expected)
			return false;
		current++;
		return true;
	}

	private int peek() {
		if (isAtEnd())
			return '\0';
		return source.codePointAt(current);
	}

	private void charlit() {
		StringBuilder sb = new StringBuilder();
		if (peekNext() == '\'') {
			sb.appendCodePoint(peek());
			advance();
		}
		while (peek() != '\'') {
			if (isAtEnd())
				break;
			sb.appendCodePoint(peek());
			advance();
		}
		if (!isAtEnd())
			advance();

		String decoded = substCodes(sb.toString());
		if (decoded.codePoints().count() == 1) {
			addToken(CHAR, decoded.codePointAt(0));
			return;
		}
		throw new ScannerError("Bad character literal.");
	}

	private void string() {
		String terminator = "";
		String terminatorQ;
		boolean raw = false;
		StringBuilder sb = new StringBuilder();

		if (peek() == '<' && peekNext() == '{') {
			terminator = collect(maxTerminator, '}');
			if(terminator.length() > 1) {
				/* skip { */
				terminator = terminator.substring(1);
				++current;
			}
			raw = terminator.length() > 0;
			if(!raw) {
				System.err.println("Warning: bad raw string literal");
			}
		}
		terminatorQ = terminator + "\"";
		current += terminator.length();

		while (!isAtEnd()) {
			if (collect(terminatorQ.length(), '"').equals(terminatorQ)) {
				break;
			}
			if (peek() == '\n') {
				line++;
				sb.append('\n');
				advance();
				if (!raw) {
					while (Character.isWhitespace(peek())) {
						advance();
					}
				}
			} else {
				sb.appendCodePoint(peek());
				advance();
			}
		}

		// Unterminated string.
		if (isAtEnd()) {
			throw new ScannerError("Unterminated string.");
		}

		if (!raw) {
			// Unicode, HTML and LaTeX Escapes
			String value = substCodes(sb.toString());
			addToken(STRING, value);
		} else {
			addToken(RSTRING, sb.toString());
		}

		for (int i = 0; i < terminatorQ.length(); i++) {
			advance();
		}
	}

	private static String substCodes(String string) {
		StringBuilder sb = new StringBuilder();
		int ix1 = string.indexOf("@{");
		int ix2 = string.indexOf('}', ix1);
		if (ix1 >= 0 && ix2 > ix1) {
			String name = string.substring(ix1 + 2, ix2);
			String pre = string.substring(0, ix1);
			String post = string.substring(ix2 + 1);
			int cp = Encoding.decode(name);
			if (cp == -1) {
				return sb.append(string.substring(0, ix2 + 1)).append(substCodes(post)).toString();
			} else {
				sb.append(pre).appendCodePoint(cp);
				return sb.append(substCodes(post)).toString();
			}
		} else {
			return string;
		}
	}

	private String collect(int max, int end) {
		StringBuilder sb = new StringBuilder();
		for (int i = current; i < current + max; i++) {
			if (i >= source.length()) {
				return "";
			}
			int c = source.codePointAt(i);
			if (c == end) {
				sb.appendCodePoint(c);
				return sb.toString();
			}
			sb.appendCodePoint(c);
		}
		return "";
	}

	private boolean isDigit(int c) {
		return c >= '0' && c <= '9' || c == '_';
	}

	private boolean isDigit(int c, int radix) {
		if (radix <= 10) {
			return c >= '0' && c <= '0' + radix - 1 || c == '_';
		} else {
			c = Character.toUpperCase(c);
			return c >= '0' && c <= '9' || c >= 'A' && c <= 'A' + radix - 10 || c == '_';
		}
	}

	private void number() {

		while (isDigit(peek()))
			advance();

		// radix
		if (peek() == 'r') {
			int radix = Integer.parseInt(source.substring(start, current));
			if (radix < 2 || radix > 36) {
				throw new ScannerError(String.format("Radix %d is not valid: must be 2 ≤ r ≤ 36", radix));
			}
			start = current + 1;
			advance();
			while (isDigit(peek(), radix))
				advance();
			addTokenWithUnit(INT, new BigInteger(strip(source.substring(start, current)), radix));
		}
		// Look for a fractional/exponent part.
		else if (peek() == '.' && isDigit(peekNext())) {
			// Consume the "."
			advance();
			while (isDigit(peek()))
				advance();
			if (peek() == 'e' || peek() == 'E') {
				if (peekNext() == '+' || peekNext() == '-') {
					advance();
				}
				advance();
				while (isDigit(peek()))
					advance();
			}
			addTokenWithUnit(REAL, Double.valueOf(strip(source.substring(start, current))));
		} else if (peek() == 'e' || peek() == 'E') {
			if (peekNext() == '+' || peekNext() == '-') {
				advance();
			}
			advance();
			while (isDigit(peek()))
				advance();
			addTokenWithUnit(REAL, Double.valueOf(strip(source.substring(start, current))));
		} else {
			addTokenWithUnit(INT, new BigInteger(strip(source.substring(start, current))));
		}
	}

	private void addTokenWithUnit(Soperator kind, Object value) {
		/* Check if Int/Real literal is followed by UnitSpec */
		/* e.g. 3_km/h */
		if(previous() == '_') {
			Bag units = unitSpec();
			if(units != null) {
				Double dvalue = 0.0;
				if(value instanceof BigInteger) {
					dvalue = ((BigInteger)value).doubleValue();
				} else if(value instanceof Double) {
					dvalue = (Double)value;
				}
				UnitValue uval = new UnitValue(dvalue, units);
				addToken(UNITLIT, uval);
				return;
			}
		}
		addToken(kind, value);
	}

	public Bag unitSpec() {
		// prefixunit[^power][(/|*)prefixunit[^power]]...
		try {
			Bag units = new Bag();

			Unit unit = parseUnit();
			if(unit == null) return null;

			units.add(unit);

			loop: for (;;) {

				int c = peek();
				switch (c) {
				case '/':
					advance();
					unit = parseUnit();
					unit.pow = -unit.pow;
					units.add(unit);
					break;
				case '*':
					advance();
					unit = parseUnit();
					units.add(unit);
					break;
				case -1:
				case ' ':
				case '\n':
				case '\r':
				default:
					advance();
					break loop;
				}
			}			
			return units;
			
		} catch (Exception e) {
			throw new MuException("Bad unit literal: %s", e);
		}
	}

	Unit parseUnit() {
		try {
			StringBuilder sb = new StringBuilder();
			for (;;) {
				int c = peek();
				//25..28
				if (Character.isAlphabetic(c) || Character.isDigit(c) || 
						// Unicode SYMBOLs
						Character.getType(c) >= 25 && Character.getType(c) <= 28) {
					sb.append((char)c);
					advance();
				} else {
					break;
				}
			}
			String prefixedUnit = sb.toString();
			if(prefixedUnit.length() == 0) {
				return null;
			}
			int pow = 1;
			if(prefixedUnit.contains("^")) {
				String[] ss = prefixedUnit.split("\\^");
				prefixedUnit = ss[0];
				pow = Integer.parseInt(ss[1]);
			}
			return new Unit(prefixedUnit, pow);
		} catch (Exception e) {
			throw new MuException("Bad unit literal: %s", e);
		}
	}

	private int previous() {
		return source.codePointAt(current -1);
	}

	private String strip(String s) {
		return s.replace("_", "");
	}

	private int peekNext() {
		if (current + 1 >= source.length())
			return '\0';
		return source.codePointAt(current + 1);
	}

	private boolean isAlpha(int c) {
		return Character.isJavaIdentifierStart(c);
	}

	private boolean isAlphaNumeric(int c) {
		return Character.isAlphabetic(c) || isDigit(c);
	}

	private void identifier() {
		identifier(false);
	}
	
	private void identifier(boolean escaped) {
		while (isAlphaNumeric(peek()))
			advance();
		// See if the identifier is a reserved word.
		String text = source.substring(start, current);

		TokenType type;

		if (!escaped && isKeyword(text)) {
			addToken(Keyword.valueOf(text.toUpperCase()));
		} else if (!escaped && isAttribute(text)) {
			addToken(Attribute.valueOf(text.toUpperCase()));
		} else {
			if (Character.isUpperCase(text.codePointAt(0))) {
				type = TID;
				if(peek()=='?') {
					advance();
					addFullToken(TID, text);
					addToken(OR);
					addFullToken(TID, "Void");
					return;
				}
			} else {
				type = ID;
			}
			addToken(type);
		}
	}

	private boolean isKeyword(String id) {
		try {
			return Keyword.valueOf(id.toUpperCase()).name().toLowerCase().equals(id);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private boolean isAttribute(String id) {
		try {
			Attribute.valueOf(id.toUpperCase());
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

}