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
	/* mixin var/params */
	MIXIN,
	/* doc comment */
	DOC,
	/* thunk */
	THUNK
}
