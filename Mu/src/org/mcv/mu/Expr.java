package org.mcv.mu;

import java.util.ArrayList;

abstract class Expr {

	String type = "None";
	
	interface Visitor<R> {
		R visitAssignExpr(Assign expr);
		R visitBinaryExpr(Binary expr);
		R visitFunctionExpr(Function expr);
		R visitCallExpr(Call expr);
		R visitIfExpr(If expr);
		R visitThisExpr(This expr);
		R visitGetExpr(Getter expr);
		R visitSetterExpr(Setter expr);
		R visitBlockExpr(Block expr);
		R visitLiteralExpr(Literal expr);
		R visitSuperExpr(Super expr);
		R visitUnaryExpr(Unary expr);
		R visitPostfixExpr(Postfix expr);
		R visitVariableExpr(Variable expr);
		R visitListExpr(List expr);
		R visitSetExpr(Set expr);
		R visitRangeExpr(Range expr);
		R visitMappingExpr(Mapping expr);
		R visitMapExpr(Map expr);
		R visitTypeExpr(Type expr);
	}

	// Nested Expr classes here...

	static class List extends Expr {
		List(java.util.List<Expr> exprs) {
			this.exprs = exprs;
			type = "List";
			eltType = exprs.isEmpty() ? "Any" : exprs.get(0).type;
		}
		
		java.util.List<Expr> exprs;
		final String eltType;
		
		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visitListExpr(this);
		}
	}

	static class Map extends Expr {
		Map(java.util.List<Stmt> exprs) {
			this.mappings = exprs;
			type = "Map";
		}
		
		java.util.List<Stmt> mappings;
		
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
		
		java.util.List<Stmt> statements = new ArrayList<>();
		Expr last;
		
		Block(java.util.List<Stmt> statements) {
			this.statements = statements;
			Stmt lastStmt = statements.get(statements.size() - 1);
			if (lastStmt instanceof Stmt.Expression) {
				last = ((Stmt.Expression) lastStmt).expr;
			} else {
				last = new Expr.Literal(null);
			}
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
			return visitor.visitGetExpr(this);
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

	static class Function extends Expr {
		
		Function(java.util.List<Token> parameters, java.util.List<Stmt> body, String type) {
			this.parameters = parameters;
			this.body = body;
			this.type = type;
	    }
	    <R> R accept(Visitor<R> visitor) {
	    	return visitor.visitFunctionExpr(this);
	    }
	    final java.util.List<Token> parameters;
	    final java.util.List<Stmt> body;
	}

	abstract <R> R accept(Visitor<R> visitor);
}
