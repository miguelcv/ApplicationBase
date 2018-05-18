package org.mcv.mu;

public enum Keyword implements TokenType {
	NONE,
	MODULE, IMPORT,
	TYPE, CLASS, INTERFACE,
	FUN, ITER, OBJECT,
	RETURN, YIELD, 
	PRINT,
	THIS, 
	VAR, VAL, 
	IF, ELSE,
	SELECT, WHEN,
	DO, WHILE,
	FOR, IN,
	BREAK, CONTINUE,
	FALSE, TRUE, NIL,
	SQRT, GCD, ABS, MAX, MIN,
	MOD, REM, INF, NAN,
	// type operators
	REF, LIST, SET, MAP, STRUCT //, TYPE
}
