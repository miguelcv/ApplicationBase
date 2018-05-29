package org.mcv.mu;

public enum Attribute implements TokenType {
	NONE,
	/* funcdefs */
	LOCAL, 	OVERRIDE,
	/* var/valdefs */
	/* create [overridable] getter [var: setter] */
	PROP,
	/* class/own variable */
	OWN,
	/* doc comment */
	DOC
	/* perhaps later... */
	//EXTERN func, ABSTRACT func ,VOLATILE var, ASYNC func, TRANSIENT var,
}
