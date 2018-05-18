package org.mcv.mu;

import static org.mcv.mu.Keyword.*;
import static org.mcv.mu.Soperator.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

class Scanner {
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0;
	private int current = 0;
	private int line = 1;
	private static final int MAX_TERMINATOR = 8;
	private static final int BOM = 0xFEFF;

	Scanner(String source) {
		this.source = source;
	}

	List<Token> scanTokens() {
		while (!isAtEnd()) {
			// We are at the beginning of the next lexeme.
			start = current;
			scanToken();
		}
		tokens.add(new Token(EOF, "", null, line));
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
			// Ignore whitespace.
			break;
		case '\n':
			addToken(SEMICOLON);
			line++;
			break;
		case ';':
			addToken(SEMICOLON);
			break;
		case '(':
			addToken(LEFT_PAREN);
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
			addToken(BACKTICK);
			break;
		case '\'':
			charlit();
			break;
		case '"':
			string();
			break;
		case '@':
			addToken(ATSIGN);
			break;
		case '#':
			addToken(SHARP);
			break;
		case '$':
			addToken(DOLLAR);
			break;
		case '_':
			addToken(UNDERSCORE);
			break;
		case '\\':
			addToken(BACKSLASH);
			break;
		case '?':
			addToken(QUESTION);
			break;
		case ',':
			addToken(COMMA);
			break;
		case '.':
			addToken(match('.') ? DOTDOT : DOT);
			break;
		case ':':
			addToken(match('=') ? ASSIGN : COLON);
			break;
		case '←':
			addToken(ASSIGN);
			break;
		case '→':
			addToken(ARROW);
			break;
		case 'Ø':
		case '∅':
			addToken(EMPTY_SET);
			break;
		case '∈':
			addToken(IN);
			break;
		case '∞':
			addToken(INF);
			break;
			
		/* OPERATORS */
		case '÷':
			addToken(match('=') ? SLASHIS : SLASH);
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
				addToken(match('=') ? SLASHIS : SLASH);
			}
			break;
		case '%':
			addToken(match('=') ? PERCENTIS : PERCENT);
			break;
		case '^':
			addToken(match('=') ? POWIS : POW);
			break;
		case '⊕':
			addToken(match('=') ? XORIS : XOR);
			break;		
		case '!':
			addToken(BANG);
			break;
		case '-':
			addToken(match('>') ? ARROW : match('-') ? MINMIN : match('=') ? MINIS : MINUS);
			break;
		case '+':
			addToken(match('+') ? PLUSPLUS : match('=') ? PLUSIS : PLUS);
			break;
		case '&':
			addToken(match('=') ? ANDIS : AND);
			break;
		case '|':
			addToken(match('=') ? ORIS: OR);
			break;
		case '*':
		case '×':
			addToken(match('=') ? STARIS : STAR);
			break;
		case '~':
		case '¬':
			addToken(match('=') ? NOT_EQUAL : NOT);
			break;
		case '√':
			addToken(SQRT);
			break;
			
		/* RELOPS */
		case '=':
			addToken(match('>') ? ARROW : match('=')? EQEQ : EQUAL);
			break;
		case '<':
			addToken(match('=') ? LESS_EQUAL :
			match('-') ? ASSIGN : 
			match('<') ? 
				(match('=') ? LSHIFTIS : LEFTSHIFT) 
				: LESS);
			break;
		case '≤':
			addToken(LESS_EQUAL);
			break;
		case '>':
			addToken(match('=') ? GREATER_EQUAL :
				match('>') ?
						( match('=') ? RSHIFTIS : RIGHTSHIFT)
				: GREATER);
			break;
		case '≥':
			addToken(GREATER_EQUAL);
			break;
			
		default:
			if (isDigit(c)) {
				number();
			} else if (isAlpha(c)) {
				identifier();
			} else {
				Mu.error(line, "Unexpected character " + (char)c);
			}
			break;
		}
	}

	private void ccomment() {
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
				if(isAtEnd()) break loop;
			}
			advance();
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
		if(peekNext() == '\'') {
			sb.appendCodePoint(peek());
			advance();
		}
		while(peek() != '\'') {
			if(isAtEnd()) break;
			sb.appendCodePoint(peek());
			advance();
		}
		if(!isAtEnd()) advance();
		
		String decoded = substCodes(sb.toString());
		if(decoded.length() == 1) {
			addToken(CHAR, decoded.codePointAt(0));
			return;
		}
		Mu.error(line, "Bad character literal.");
	}

	private void string() {
		String terminator = "";
		String terminatorQ;

		if (peek() == '(') {
			terminator = collect(MAX_TERMINATOR, ')');
		}
		terminatorQ = terminator + "\"";
		start += terminator.length() + 1;
		current += terminator.length();

		while (!isAtEnd()) {
			if (collect(terminatorQ.length(), '"').equals(terminatorQ)) {
				break;
			}
			if (peek() == '\n') {
				line++;
			}
			advance();
		}

		// Unterminated string.
		if (isAtEnd()) {
			Mu.error(line, "Unterminated string.");
			return;
		}

		// Unicode, HTML and LaTeX Escapes
		String value = substCodes(source.substring(start, current));
		addToken(STRING, value);

		for (int i = 0; i < terminatorQ.length(); i++) {
			advance();
		}
	}

	private static String substCodes(String string) {
		StringBuilder sb = new StringBuilder();
		int ix1 = string.indexOf("@{");
		int ix2 = string.indexOf('}');
		if(ix1 >= 0 && ix2 > ix1) {
			String name = string.substring(ix1+2, ix2);
			String pre = string.substring(0, ix1);
			String post = string.substring(ix2+1);
			int cp = Encoding.decode(name);
			if(cp == -1) {
				return sb.append(string.substring(0, ix2+1)).append(substCodes(post)).toString();
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
		if(isAtEnd()) return false;
		return c >= '0' && c <= '9' || c == '_';
	}

	private boolean isDigit(int c, int radix) {
		if(isAtEnd()) return false;
		if(radix <= 10) {
			return c >= '0' && c <= '0' + radix-1 || c == '_';
		} else {
			c = Character.toUpperCase(c);
			return c >= '0' && c <= '9' || c >= 'A' && c <= 'A' + radix-10 || c == '_';
		}
	}

	private void number() {
		
		while (isDigit(peek()))
			advance();

		// radix
		if(peek() == 'r') {
			int radix = Integer.parseInt(source.substring(start, current));
			if(radix < 2 || radix > 36) {
				Mu.runtimeError("Radix %d is not valid: must be 2 ≤ r ≤ 36", radix);
			}
			start = current+1;
			advance();
			while (isDigit(peek(), radix))
				advance();
			addToken(INT, new BigInteger(strip_(source.substring(start, current)), radix));
		}
		// Look for a fractional/exponent part.
		else if (peek() == '.' && isDigit(peekNext())) {
			// Consume the "."
			advance();
			while (isDigit(peek()))
				advance();
			if(peek() == 'e' || peek() == 'E') {
				if(peekNext() == '+' || peekNext() == '-') {
					advance();
				}
				advance();
				while (isDigit(peek()))
					advance();
			}
			addToken(REAL, Double.valueOf(strip_(source.substring(start, current))));
		} else if(peek() == 'e' || peek() == 'E') {
			if(peekNext() == '+' || peekNext() == '-') {
				advance();
			}
			advance();
			while (isDigit(peek()))
				advance();
			addToken(REAL, Double.valueOf(strip_(source.substring(start, current))));
		} else {
			addToken(INT, new BigInteger(strip_(source.substring(start, current))));
		}
	}
	
	private String strip_(String s) {
		return s.replace("_", "");
	}

	private int peekNext() {
		if (current + 1 >= source.length())
			return '\0';
		return source.codePointAt(current + 1);
	}

	private boolean isAlpha(int c) {
		if(isAtEnd()) 
			return false;
		return Character.isJavaIdentifierStart(c);
	}

	private boolean isAlphaNumeric(int c) {
		if(isAtEnd()) return false;
		return Character.isJavaIdentifierPart(c);
	}

	private void identifier() {
		while (isAlphaNumeric(peek()))
			advance();
		// See if the identifier is a reserved word.
		String text = source.substring(start, current);
		
		TokenType type;
		
		if(isKeyword(text)) {
			addToken(Keyword.valueOf(text.toUpperCase()));
		} else if(isAttribute(text)) {
			addToken(Attribute.valueOf(text.toUpperCase()));
		} else {
			if(Character.isUpperCase(text.codePointAt(0))) {
				type = TID;
			} else {
				type = ID;
			}
			addToken(type);
		}	
	}
	
	private boolean isKeyword(String id) {
		try {
			Keyword.valueOf(id.toUpperCase());
			return true;
		} catch(IllegalArgumentException e) {
			return false;
		}
	}
	
	private boolean isAttribute(String id) {
		try {
			Attribute.valueOf(id.toUpperCase());
			return true;
		} catch(IllegalArgumentException e) {
			return false;
		}
	}

}