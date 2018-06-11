package org.mcv.mu;

class Token {
	
	public static final Token DUMMY = new Token(Keyword.NONE, "", null, -1);

	TokenType type;
	final String lexeme;
	final Object literal;
	final int line;

	Token(TokenType type) {
		this.type = type;
		this.lexeme = "";
		this.literal = "";
		this.line = -1;
	}
	
	Token(TokenType type, String lexeme, Object literal, int line) {
		this.type = type;
		this.lexeme = lexeme;
		this.literal = literal;
		this.line = line;
	}

	public String toString() {
		return type.toString() + " " + lexeme;
	}
	
	@Override public boolean equals(Object other) {
		if(other instanceof Token) {
			return type.equals(((Token)other).type);
		}
		return false;
	}
	
	@Override public int hashCode() {
		return type.hashCode();
	}
}