package org.mcv.mu;

import java.util.Map;
import java.util.Set;

abstract class Pattern {
	
	/* common interface for classes, interfaces, functions and iterators */
	Map<String, Object> attributes;
	String name;
	Set<String> typeParams;
	Params valParams;
	/* CLASS and INERFACE only */
	Set<String> interfaces;
	/* FUNCTION and ITERATOR only */
	String returnType;
	Expr.Block body;

	/* DECLARE::
	 *
	 * [attributes]* "func"|"iter"|"class"|"interface" [id] ["{" typeparams, "}"] "(" [valparams,] ")" "=>" "{" [interfaces,] "}" | "(" types, ")" ":" "(" [exprs;] ")"
	 * 
	 */
	
	abstract Object call(Interpreter interpreter, ListMap<Object> arguments);
}
