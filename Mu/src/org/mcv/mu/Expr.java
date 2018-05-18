package org.mcv.mu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class Expr {

	private static final String SHARED = "shared";

	Type type = Type.None;
	
	interface Visitor<R> {
		/* decl */
		R visitModuleDef(Module expr);
		R visitClassDef(ClassDef expr);
		R visitInterfaceDef(InterfaceDef expr);
		R visitFuncDef(FuncDef expr);
		R visitIterDef(IterDef expr);
		R visitVarDef(Var expr);
		R visitValDef(Val expr);
		
		/* print */
		R visitPrintExpr(Print expr);
		
		/* ctrl */
		R visitIfExpr(If expr);
		R visitSelectExpr(Select select);
		R visitForExpr(For expr);
		R visitWhileExpr(While expr);
		R visitBreakExpr(Break expr);
		R visitContinueExpr(Continue expr);
		R visitReturnExpr(Return expr);
		
		/* expr */
		R visitAssignExpr(Assign expr);
		R visitBinaryExpr(Binary expr);
		R visitThisExpr(This expr);
		R visitGetterExpr(Getter expr);
		R visitSetterExpr(Setter expr);
		R visitBlockExpr(Block expr);
		R visitLiteralExpr(Literal expr);
		R visitUnaryExpr(Unary expr);
		R visitPostfixExpr(Postfix expr);
		R visitVariableExpr(Variable expr);
		R visitSeqExpr(Seq expr);
		R visitSetExpr(Set expr);
		R visitRangeExpr(Range expr);
		R visitMappingExpr(Mapping expr);
		R visitMapExpr(Map expr);
		R visitTypeLiteralExpr(TypeLiteral expr);
	}
	
	// Nested Expr classes here...

	/* DECLARATIONS */
	static class Module extends Expr {
		Module(Token name, HashMap<String, Object> attributes) {
			// NOPE
			this.type = Type.Type;
			this.name = name;
			this.attributes = attributes;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitModuleDef(this);
		}

		final Token name;
		HashMap<String, Object> attributes;
		public boolean isShared() {
			return (boolean)attributes.getOrDefault(SHARED, false);
		}
		// public environment??
	}

	public static class ClassDef extends Expr {
		ClassDef(Token name, Params params, java.util.Set<String> interfaces, Block body, HashMap<String, Object> attributes) {
			this.type = Type.Type;
			this.params = params;
			this.name = name;
			this.interfaces = interfaces;
			this.body = body;
			this.attributes = attributes;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitClassDef(this);
		}

		final Token name;
		Params params;
		java.util.Set<String> interfaces;
		Block body;
		HashMap<String, Object> attributes;
		public boolean isShared() {
			return (boolean)attributes.getOrDefault(SHARED, false);
		}
	}

	public static class IterDef extends Expr {

		public IterDef(Token name, Params params, Type returnType, Block body, HashMap<String, Object> attributes) {
			this.type = Type.Type;
			this.params = params;
			this.name = name;
			this.returnType = returnType;
			this.body = body;
			this.attributes = attributes;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIterDef(this);
		}
		final Token name;
		Params params;
		Type returnType;
		Block body;
		HashMap<String, Object> attributes;
		public boolean isShared() {
			return (boolean)attributes.getOrDefault(SHARED, false);
		}
	}

	public static class FuncDef extends Expr {

		public FuncDef(Token name, Params params, Type returnType, Block body, HashMap<String, Object> attributes) {
			this.type = Type.Type;
			this.params = params;
			this.name = name;
			this.returnType = returnType;
			this.body = body;
			this.attributes = attributes;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFuncDef(this);
		}
		
		final Token name;
		Params params;
		Type returnType;
		Block body;
		HashMap<String, Object> attributes;
		public boolean isShared() {
			return (boolean)attributes.getOrDefault(SHARED, false);
		}
	}

	public static class InterfaceDef extends Expr {

		public InterfaceDef(Token name, Params params, java.util.Set<String> interfaces, Block body, HashMap<String, Object> attributes) {
			this.type = Type.Type;
			this.params = params;
			this.name = name;
			this.interfaces = interfaces;
			this.body = body;
			this.attributes = attributes;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitInterfaceDef(this);
		}
		final Token name;
		Params params;
		java.util.Set<String> interfaces;
		Block body;
		HashMap<String, Object> attributes;
		public boolean isShared() {
			return (boolean)attributes.getOrDefault(SHARED, false);
		}
	}

	static class Var extends Expr {
		Var(Token name, Expr initializer, boolean shared) {
			this.name = name;
			if(initializer instanceof TypeLiteral) {
				this.initializer = initializer;
				type = ((TypeLiteral)initializer).literal;
			} else {
				this.initializer = initializer;
				type = initializer.type;
			}
			attributes.put(SHARED, shared);
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarDef(this);
		}

		final Token name;
		final Expr initializer;
		final HashMap<String, Object> attributes = new HashMap<>();
		public boolean isShared() {
			return (boolean)attributes.getOrDefault(SHARED, false);
		}
	}

	static class Val extends Expr {
		Val(Token name, Expr initializer, boolean shared) {
			this.name = name;
			this.initializer = initializer;
			attributes.put(SHARED, shared);
			type = initializer.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitValDef(this);
		}

		final Token name;
		final Expr initializer;
		final HashMap<String, Object> attributes = new HashMap<>();
		public boolean isShared() {
			return (boolean)attributes.getOrDefault(SHARED, false);
		}
	}
	
	static class Print extends Expr {
		Print(Expr expression) {
			this.expression = expression;
			type = Type.Void;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintExpr(this);
		}

		final Expr expression;
	}

	static class Return extends Expr {
		Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
			type = value.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitReturnExpr(this);
		}

		final Token keyword;
		final Expr value;
	}

	static class While extends Expr {
		While(Expr condition, Expr body) {
			this.condition = condition;
			this.body = body;
			type = Type.Void;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitWhileExpr(this);
		}

		final Expr condition;
		final Expr body;
	}

	static class For extends Expr {
		For(Expr.Var var, Range range, Block body) {
			this.var = var;
			this.range = range;
			this.body = body;
			type = Type.Void;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitForExpr(this);
		}

		final Var var;
		final Range range;
		final Block body;
	}

	static class Break extends Expr {
		Break() {
			type = Type.Void;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBreakExpr(this);
		}
	}

	static class Continue extends Expr {
		Continue() {
			type = Type.Void;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitContinueExpr(this);
		}
	}

	static class Seq extends Expr {
		Seq(List<Expr> exprs) {
			this.exprs = exprs;
			eltType = exprs.isEmpty() ? Type.Any : exprs.get(0).type;
			type = new Type.ListType(eltType);
		}
		
		List<Expr> exprs;
		final Type eltType;
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSeqExpr(this);
		}
	}

	static class Map extends Expr {
		Map(List<Expr> exprs) {
			this.mappings = exprs;
			type = new Type("Map");
		}
		
		List<Expr> mappings;
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitMapExpr(this);
		}
	}

	static class Set extends Expr {
		Set(java.util.Set<Expr> exprs) {
			this.exprs = exprs;
			eltType = exprs.isEmpty() ? new Type("Any") : exprs.iterator().next().type;
			type = new Type.SetType(eltType);
		}
		
		java.util.Set<Expr> exprs;
		final Type eltType;
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSetExpr(this);
		}
	}

	static class Range extends Expr {
		Range(Expr start, Expr end, boolean startIncl, boolean endIncl) {
			this.start = start;
			this.startIncl = startIncl;
			this.end = end;
			this.endIncl = endIncl;
			// nope
			type = new Type("Range");
		}
		
		Expr start;
		Expr end;
		boolean startIncl;
		boolean endIncl;
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitRangeExpr(this);
		}
	}

	static class Mapping extends Expr {
		Mapping(String  key, Expr value) {
			this.key = key;
			this.value = value;
			type = value.type;
		}
		
		String key;
		Expr value;
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitMappingExpr(this);
		}
	}

	static class Assign extends Expr {
		Assign(Token name, Expr value, Token op) {
			this.name = name;
			this.value = value;
			this.op = op;
			type = value.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitAssignExpr(this);
		}

		final Token name;
		final Expr value;
		final Token op;
	}

	static class Binary extends Expr {
		Binary(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
			type = left.type.equals(right.type) ? left.type : new Type.IntersectionType(left.type, right.type);
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBinaryExpr(this);
		}

		final Expr left;
		final Token operator;
		final Expr right;
	}

	static class Unary extends Expr {
		Unary(Token operator, Expr right) {
			this.operator = operator;
			this.right = right;
			type = right.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitUnaryExpr(this);
		}

		final Token operator;
		final Expr right;
	}

	static class Postfix extends Expr {
		Postfix(Expr left, Token operator) {
			this.operator = operator;
			this.left = left;
			type = left.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPostfixExpr(this);
		}

		final Token operator;
		final Expr left;
	}

	static class Block extends Expr {
		
		List<Expr> expressions = new ArrayList<>();
		Expr last;
		
		Block(List<Expr> expressions) {
			this.expressions = expressions;
			last = expressions.get(expressions.size() - 1);
			type = last.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockExpr(this);
		}		
	}

	static class If extends Expr {
		If(Expr condition, Expr thenBranch, Expr elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
			type = new Type("Void");
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIfExpr(this);
		}

		final Expr condition;
		final Expr thenBranch;
		final Expr elseBranch;
	}

	static class Select extends Expr {
		Select(Expr condition, List<Expr> whenExpressions, List<Expr> whenBranches, Expr elseBranch) {
			this.condition = condition;
			this.whenExpressions = whenExpressions;
			this.whenBranches = whenBranches;
			this.elseBranch = elseBranch;
			type = new Type("Void");
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSelectExpr(this);
		}

		final Expr condition;
		final List<Expr> whenExpressions;
		final List<Expr> whenBranches;
		final Expr elseBranch;
	}

	static class Getter extends Expr {
		Getter(Expr object, Token name) {
			this.object = object;
			this.name = name;
			// IS IT??
			type = object.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitGetterExpr(this);
		}

		final Expr object;
		final Token name;
	}

	static class Setter extends Expr {
		Setter(Expr object, Token name, Expr value) {
			this.object = object;
			this.name = name;
			this.value = value;
			type = new Type("Void");
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSetterExpr(this);
		}

		final Expr object;
		final Token name;
		final Expr value;
	}

	static class This extends Expr {
		This(Token keyword) {
			this.keyword = keyword;
			// resolve later
			this.type = Type.Any;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitThisExpr(this);
		}

		final Token keyword;
	}

	static class Literal extends Expr {
		Literal(Object value) {
			this.value = value;
			type = Interpreter.typeFromValue(value);
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitLiteralExpr(this);
		}

		final Object value;
	}
	
	static class Variable extends Expr {
		Variable(Token name) {
			this.name = name;
			this.type = Type.Any;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVariableExpr(this);
		}

		final Token name;
	}

	static class TypeLiteral extends Expr {
		TypeLiteral(Token name, Type type, java.util.Map<String, Object> attributes) {
			this.name = name;
			this.type = Type.Type;
			this.literal = type;
			this.attributes = attributes;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitTypeLiteralExpr(this);
		}
		
		final Token name;
		final Type literal;
		java.util.Map<String, Object> attributes = new HashMap<>();
		public boolean isShared() {
			return (boolean)attributes.getOrDefault(SHARED, false);
		}
	}

	abstract <R> R accept(Visitor<R> visitor);
}
