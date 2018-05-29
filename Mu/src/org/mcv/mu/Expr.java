package org.mcv.mu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class Expr {

	private static final String PUBLIC = "public";

	Type type = Type.None;
	int line;
	
	interface Visitor<R> {
		/* decl */
		Result visitModuleDef(Module expr);
		Result visitTemplateDef(TemplateDef expr);
		Result visitVarDef(Var expr);
		Result visitValDef(Val expr);
		Result visitTypeDefExpr(TypeDef expr);
		
		/* print */
		Result visitPrintExpr(Print expr);
		Result visitPrintTypeExpr(PrintType expr);
		
		/* ctrl */
		Result visitIfExpr(If expr);
		Result visitSelectExpr(Select select);
		Result visitForExpr(For expr);
		Result visitWhileExpr(While expr);
		Result visitBreakExpr(Break expr);
		Result visitContinueExpr(Continue expr);
		Result visitReturnExpr(Return expr);
		Result visitAopExpr(Aop expr);
		Result visitThrowExpr(Throw expr);
		
		/* expr */
		Result visitAssignExpr(Assign expr);
		Result visitBinaryExpr(Binary expr);
		Result visitBlockExpr(Block expr);
		Result visitLiteralExpr(Literal expr);
		Result visitUnaryExpr(Unary expr);
		Result visitPostfixExpr(Postfix expr);
		Result visitVariableExpr(Variable expr);
		Result visitSeqExpr(Seq expr);
		Result visitSetExpr(Set expr);
		Result visitRangeExpr(Range expr);
		Result visitMappingExpr(Mapping expr);
		Result visitMapExpr(Map expr);
		Result visitTypeLiteralExpr(TypeLiteral expr);
		
		/* specials */
		Result visitCallExpr(Call expr);
		Result visitDotExpr(Dot expr);
		Result visitSubscriptExpr(Subscript expr);
		
	}
	
	// Nested Expr classes here...

	/* DECLARATIONS */
	static class Module extends Expr {
		Module(Token name, Attributes attributes) {
			this.type = Type.Any;
			this.name = name;
			this.line = name.line;
			this.attributes = attributes;
			if(attributes.isLocal(false)) {
				attributes.put(PUBLIC, false);
			} else {
				attributes.put(PUBLIC, true);
			}
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitModuleDef(this);
		}

		final Token name;
		Attributes attributes;
		// public environment??
		@Override
		public String toString() {
			return "module " + name.lexeme;
		}
	}

	public static class TemplateDef extends Expr {
		TemplateDef(Token name, String kind, Params params, Type returnType, Block body, Attributes attributes) {
			this.type = Type.Type;
			this.kind = kind;
			this.params = params;
			this.returnType = returnType;
			this.name = name;
			this.body = body;
			this.attributes = attributes;
			if(attributes.isLocal(false)) {
				attributes.put(PUBLIC, false);
			} else {
				attributes.put(PUBLIC, true);				
			}
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitTemplateDef(this);
		}

		final Token name;
		String kind;
		Params params;
		Type returnType;
		Block body;
		Attributes attributes;
		@Override
		public String toString() {
			return kind + " " + name.lexeme;
		}
	}
	
	static class Var extends Expr {
		Var(Token name, Expr initializer, Attributes attributes) {
			this.name = name;
			if(initializer == null) {
				// for statement for var i in ...
				this.initializer = new Expr.Literal(name, 0);
				type = Type.Int;
			} else if(initializer instanceof TypeLiteral) {
				this.initializer = initializer;
				type = ((TypeLiteral)initializer).literal;
			} else {
				this.initializer = initializer;
				type = initializer.type;
			}
			this.attributes = attributes;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitVarDef(this);
		}

		final Token name;
		final Expr initializer;
		final Attributes attributes;
		@Override
		public String toString() {
			return "var " + name.lexeme + ":" + initializer;
		}
	}

	static class Val extends Expr {
		Val(Token name, Expr initializer, Attributes attributes) {
			this.name = name;
			this.initializer = initializer;
			this.attributes = attributes;
			type = initializer.type;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitValDef(this);
		}

		final Token name;
		final Expr initializer;
		final Attributes attributes;
		@Override
		public String toString() {
			return "val " + name.lexeme + ":" + initializer;
		}
	}
	
	static class Aop extends Expr {
		Aop(Token name, Expr callable, Block block) {
			this.name = name;
			this.callable = callable;
			this.block = block;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitAopExpr(this);
		}

		final Token name;
		final Expr callable;
		final Block block;
		@Override
		public String toString() {
			return "AOP " + name.lexeme + " " + callable + ":" + block;
		}
	}
	
	static class Print extends Expr {
		Print(Token name, Expr expression) {
			this.expression = expression;
			type = Type.Void;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitPrintExpr(this);
		}

		final Expr expression;
		@Override
		public String toString() {
			return "print " + expression;
		}
	}

	static class PrintType extends Expr {
		PrintType(Token name, Expr expression) {
			this.expression = expression;
			type = Type.Void;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitPrintTypeExpr(this);
		}

		final Expr expression;
		@Override
		public String toString() {
			return "typeof " + expression;
		}
	}

	static class Return extends Expr {
		Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
			type = value.type;
			this.line = keyword.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitReturnExpr(this);
		}

		final Token keyword;
		final Expr value;
		@Override
		public String toString() {
			return "return " + value;
		}
	}

	static class Throw extends Expr {
		Throw(Token name, Expr thrown) {
			this.name = name;
			this.thrown = thrown;
			type = thrown.type;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitThrowExpr(this);
		}

		final Token name;
		final Expr thrown;
		@Override
		public String toString() {
			return "throw " + thrown;
		}
	}

	static class While extends Expr {
		While(Token name, Expr condition, Expr body, Expr atEnd) {
			this.condition = condition;
			this.body = body;
			this.atEnd = atEnd;
			type = Type.Void;
			this.line = name.line;
			if(name.lexeme.equals(Keyword.UNTIL.name())) {
				invert = true;
			} else {
				invert = false;
			}
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitWhileExpr(this);
		}

		final Expr condition;
		final boolean invert;
		final Expr body;
		final Expr atEnd;
		@Override
		public String toString() {
			return (invert?"until ":"while ") + condition + " " + body;
		}
	}

	static class For extends Expr {
		For(Token name, Expr.Var var, Expr range, Block body, Block atEnd) {
			this.var = var;
			this.range = range;
			this.body = body;
			type = Type.Void;
			this.line = name.line;
			this.atEnd = atEnd;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitForExpr(this);
		}

		final Var var;
		final Expr range;
		final Block body;
		final Expr atEnd;
		@Override
		public String toString() {
			return "for " + var + " in " + body;
		}
	}

	static class Break extends Expr {
		Break(Token name) {
			type = Type.Void;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitBreakExpr(this);
		}
		@Override
		public String toString() {
			return "break";
		}
	}

	static class Continue extends Expr {
		Continue(Token name) {
			type = Type.Void;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitContinueExpr(this);
		}
		@Override
		public String toString() {
			return "continue";
		}
	}

	static class Seq extends Expr {
		Seq(Token name, List<Expr> exprs) {
			this.exprs = exprs;
			eltType = exprs.isEmpty() ? Type.Any : exprs.get(0).type;
			type = new Type.ListType(eltType);
			this.line = name.line;
		}
		
		List<Expr> exprs;
		final Type eltType;
		
		@Override
		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitSeqExpr(this);
		}
		@Override
		public String toString() {
			return "list []";
		}
	}

	static class Map extends Expr {
		Map(Token name, List<Expr> exprs) {
			this.mappings = exprs;
			type = new Type.MapType(Type.String, Type.Any);
			this.line = name.line;
		}
		
		List<Expr> mappings;
		
		@Override
		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitMapExpr(this);
		}
		@Override
		public String toString() {
			return "map ()";
		}
	}

	static class Set extends Expr {
		Set(Token name, java.util.Set<Expr> exprs) {
			this.exprs = exprs;
			eltType = exprs.isEmpty() ? Type.Any : exprs.iterator().next().type;
			type = new Type.SetType(eltType);
			this.line = name.line;
		}
		
		java.util.Set<Expr> exprs;
		final Type eltType;
		
		@Override
		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitSetExpr(this);
		}
		@Override
		public String toString() {
			return "set {}";
		}
	}

	static class Range extends Expr {
		Range(Token name, Expr start, Expr end, boolean startIncl, boolean endIncl) {
			this.start = start;
			this.startIncl = startIncl;
			this.end = end;
			this.endIncl = endIncl;
			type = new Type.Range("Range", start, end);
			this.line = name.line;
		}
		
		Expr start;
		Expr end;
		boolean startIncl;
		boolean endIncl;
		
		@Override
		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitRangeExpr(this);
		}
		@Override
		public String toString() {
			return start + ".." + end;
		}
	}

	static class Mapping extends Expr {
		Mapping(Token name, String  key, Expr value, boolean arrow) {
			this.key = key;
			this.value = value;
			type = value.type;
			this.line = name.line;
			this.arrow = arrow;
		}
		
		String key;
		Expr value;
		boolean arrow;
		
		@Override
		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitMappingExpr(this);
		}
		@Override
		public String toString() {
			return key + "=>" + value;
		}
	}

	static class Assign extends Expr {
		Assign(Token name, Expr value, Token op) {
			this.name = name;
			this.value = value;
			this.op = op;
			type = value.type;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitAssignExpr(this);
		}

		final Token name;
		final Expr value;
		final Token op;
		@Override
		public String toString() {
			return name.lexeme + " := " + value;
		}
	}

	static class Binary extends Expr {
		Binary(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
			type = left.type.equals(right.type) ? left.type :
				left.type.equals(Type.Any) ? right.type :
				right.type.equals(Type.Any) ? left.type:
				widen(left.type, right.type);
			this.line = operator.line;
		}

		private Type widen(Type t1, Type t2) {
			
			Object func1 = t1.interfaces.get("to" + t2);
			Object func2 = t2.interfaces.get("to" + t1);

			if (func1 != null && func1 instanceof Method) {
				return Interpreter.typeFromClass(((Method)func1).getReturnType());
			} else if (func2 != null && func2 instanceof Method) {
				return Interpreter.typeFromClass(((Method)func2).getReturnType());
			} else {
				//System.err.println(String.format("No conversion between types %s and %s", t1, t2));
				return Type.Void;
			}
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitBinaryExpr(this);
		}

		final Expr left;
		final Token operator;
		final Expr right;
		@Override
		public String toString() {
			return left + operator.lexeme +  right;
		}
	}

	static class Unary extends Expr {
		Unary(Token operator, Expr right) {
			this.operator = operator;
			this.right = right;
			type = right.type;
			this.line = operator.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitUnaryExpr(this);
		}

		final Token operator;
		final Expr right;
		@Override
		public String toString() {
			return operator.lexeme + right;
		}
	}

	static class Postfix extends Expr {
		Postfix(Expr left, Token operator) {
			this.operator = operator;
			this.left = left;
			type = left.type;
			this.line = operator.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitPostfixExpr(this);
		}

		final Token operator;
		final Expr left;
		@Override
		public String toString() {
			return left + operator.lexeme;
		}
	}

	static class Block extends Expr {
		
		List<Expr> expressions = new ArrayList<>();
		Expr last;
		
		Block(Token name, List<Expr> expressions) {
			if(expressions.isEmpty()) {
				expressions.add(new Expr.Literal(name,null));
			}
			this.expressions = expressions;
			last = expressions.get(expressions.size() - 1);
			type = last.type;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitBlockExpr(this);
		}		
		@Override
		public String toString() {
			return "block ()";
		}
	}

	static class If extends Expr {
		If(Token name, Expr condition, Expr thenBranch, Expr elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
			type = Type.Void;
			this.line = name.line;
			if(name.lexeme.equals(Keyword.UNLESS.name())) {
				invert = true;
			} else {
				invert = false;
			}
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitIfExpr(this);
		}

		final Expr condition;
		final boolean invert; 
		final Expr thenBranch;
		final Expr elseBranch;
		@Override
		public String toString() {
			return (invert? "unless " : "if ") + condition + " " + thenBranch + " else " + elseBranch;
		}
	}

	static class Select extends Expr {
		Select(Token name, Expr condition, List<Expr> whenExpressions, List<Expr> whenBranches, Expr elseBranch) {
			this.condition = condition;
			this.whenExpressions = whenExpressions;
			this.whenBranches = whenBranches;
			this.elseBranch = elseBranch;
			type = Type.Void;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitSelectExpr(this);
		}

		final Expr condition;
		final List<Expr> whenExpressions;
		final List<Expr> whenBranches;
		final Expr elseBranch;
		@Override
		public String toString() {
			return "select " + condition + " ...";
		}
	}

	static class Literal extends Expr {
		Literal(Token name, Object value) {
			this.value = value;
			type = Interpreter.typeFromValue(value);
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitLiteralExpr(this);
		}

		final Object value;
		@Override
		public String toString() {
			return value.toString();
		}
	}
	
	static class Variable extends Expr {
		Variable(Token name) {
			this.name = name;
			this.type = Type.Any;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitVariableExpr(this);
		}

		final Token name;
		@Override
		public String toString() {
			return "var " + name.lexeme;
		}
	}

	static class TypeLiteral extends Expr {
		TypeLiteral(Token name, Type type, Attributes attributes) {
			this.type = Type.Type;
			this.literal = type;
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitTypeLiteralExpr(this);
		}
		
		final Type literal;

		@Override
		public String toString() {
			return "type " + literal;
		}
	}

	static class TypeDef extends Expr {
		TypeDef(Token name, Type type, Attributes attributes) {
			this.name = name;
			this.type = Type.Type;
			this.literal = type;
			this.attributes = attributes;
			if(attributes.isLocal(false)) {
				attributes.put(PUBLIC, false);
			} else {
				attributes.put(PUBLIC, true);
			}
			this.line = name.line;
		}

		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitTypeDefExpr(this);
		}
		
		final Token name;
		final Type literal;
		Attributes attributes = new Attributes();

		@Override
		public String toString() {
			return "type " + literal;
		}
	}

	public static class Dot extends Expr {

		public Dot(Expr current, Expr next) {
			this.current = current;
			this.next = next;
		}
		
		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitDotExpr(this);
		}

		Expr current;
		Expr next;
		Expr value;
		@Override
		public String toString() {
			return current + "." + next;
		}
	}

	public static class Subscript extends Expr {

		public Subscript(Expr seq, Expr sub) {
			this.seq = seq;
			this.sub = sub;
		}
		
		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitSubscriptExpr(this);
		}

		Expr seq;
		Expr sub;
		// for assignments
		Expr value;
		@Override
		public String toString() {
			return seq + "[" + sub + "]";
		}
	}

	public static class Call extends Expr {

		public Call(Expr current, Expr next) {
			this.current = current;
			this.next = next;
		}
		
		<R> Result accept(Visitor<R> visitor) {
			return visitor.visitCallExpr(this);
		}

		Expr current;
		Expr next;
		@Override
		public String toString() {
			return current + "(" + next + ")";
		}
	}

	abstract <R> Result accept(Visitor<R> visitor);
}
