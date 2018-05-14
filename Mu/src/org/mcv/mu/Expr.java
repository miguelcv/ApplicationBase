package org.mcv.mu;

import java.util.ArrayList;
import java.util.List;

abstract class Expr {

	String type = "None";
	
	interface Visitor<R> {
		/* decl */
		R visitModuleExpr(Module expr);
		R visitClassExpr(ClassDef expr);
		R visitVarExpr(Var expr);
		R visitValExpr(Val expr);
		/* print */
		R visitPrintExpr(Print expr);
		/* ctrl */
		R visitForExpr(For expr);
		R visitWhileExpr(While expr);
		R visitBreakExpr(Break expr);
		R visitContinueExpr(Continue expr);
		R visitReturnExpr(Return expr);
		/* expr */
		R visitAssignExpr(Assign expr);
		R visitBinaryExpr(Binary expr);
		R visitFunctionExpr(Function expr);
		R visitCallExpr(Call expr);
		R visitIfExpr(If expr);
		R visitThisExpr(This expr);
		R visitGetterExpr(Getter expr);
		R visitSetterExpr(Setter expr);
		R visitBlockExpr(Block expr);
		R visitLiteralExpr(Literal expr);
		R visitSuperExpr(Super expr);
		R visitUnaryExpr(Unary expr);
		R visitPostfixExpr(Postfix expr);
		R visitVariableExpr(Variable expr);
		R visitSeqExpr(Seq expr);
		R visitSetExpr(Set expr);
		R visitRangeExpr(Range expr);
		R visitMappingExpr(Mapping expr);
		R visitMapExpr(Map expr);
		R visitTypeExpr(Type expr);
	}
	
	// Nested Expr classes here...

	static class Module extends Expr {
		Module(Token name) {
			this.type = "Type";
			this.name = name;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitModuleExpr(this);
		}

		final Token name;
	}

	static class ClassDef extends Expr {
		ClassDef(Token name, Expr.Variable superclass, List<Expr.Function> methods, List<Function> classMethods) {
			this.type = "Type";
			this.name = name;
			this.superclass = superclass;
			this.methods = methods;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitClassExpr(this);
		}

		final Token name;
		final Expr.Variable superclass;
		final List<Expr.Function> methods;
	}

	static class Function extends Expr {
		Function(java.util.List<Token> parameters, java.util.List<Expr> body, String type) {
			this.parameters = parameters;
			this.body = body;
			this.type = type;
	    }
	    <R> R accept(Visitor<R> visitor) {
	    	return visitor.visitFunctionExpr(this);
	    }
	    final java.util.List<Token> parameters;
	    final java.util.List<Expr> body;
	}

	static class Print extends Expr {
		Print(Expr expression) {
			this.expression = expression;
			type = "Void";
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

	static class Var extends Expr {
		Var(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
			if(initializer != null) type = initializer.type;
			else type = "Any";
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarExpr(this);
		}

		final Token name;
		final Expr initializer;
	}

	static class Val extends Expr {
		Val(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
			type = initializer.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitValExpr(this);
		}

		final Token name;
		final Expr initializer;
	}

	static class While extends Expr {
		While(Expr condition, Expr body) {
			this.condition = condition;
			this.body = body;
			type = "Void";
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
			type = "Void";
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
			type = "Void";
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBreakExpr(this);
		}

	}

	static class Continue extends Expr {
		Continue() {
			type = "Void";
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitContinueExpr(this);
		}

	}

	static class Seq extends Expr {
		Seq(List<Expr> exprs) {
			this.exprs = exprs;
			type = "List";
			eltType = exprs.isEmpty() ? "Any" : exprs.get(0).type;
		}
		
		List<Expr> exprs;
		final String eltType;
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSeqExpr(this);
		}
	}

	static class Map extends Expr {
		Map(List<Expr> exprs) {
			this.mappings = exprs;
			type = "Map";
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
			type = "Set";
			eltType = exprs.isEmpty() ? "Any" : exprs.iterator().next().type;
		}
		
		java.util.Set<Expr> exprs;
		final String eltType;
		
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
			type = "Range";
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
			type = left.type.equals(right.type) ? left.type : left.type + "&" + right.type;
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

	static class Call extends Expr {
		Call(Expr callee, Token paren, java.util.List<Expr> arguments) {
			this.callee = callee;
			this.paren = paren;
			this.arguments = arguments;
			type = callee.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitCallExpr(this);
		}

		final Expr callee;
		final Token paren;
		final java.util.List<Expr> arguments;
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
			type = "Void";
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitIfExpr(this);
		}

		final Expr condition;
		final Expr thenBranch;
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
			type = "Void";
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSetterExpr(this);
		}

		final Expr object;
		final Token name;
		final Expr value;
	}

	static class Super extends Expr {
		Super(Token keyword, Token method) {
			this.keyword = keyword;
			this.method = method;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitSuperExpr(this);
		}

		final Token keyword;
		final Token method;
	}

	static class This extends Expr {
		This(Token keyword) {
			this.keyword = keyword;
			// resolve later
			this.type = "Any";
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
			this.type = "Any";
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVariableExpr(this);
		}

		final Token name;
	}

	static class Type extends Expr {
		Type(Token name) {
			this.name = name;
			this.type = name.lexeme;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitTypeExpr(this);
		}
		final Token name;
	}

	abstract <R> R accept(Visitor<R> visitor);
}
