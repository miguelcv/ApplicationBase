package org.mcv.mu;

import java.util.List;

abstract class Stmt {

	String type = "None";
	
	interface Visitor<R> {
		/* decl */
		R visitModuleStmt(Module stmt);
		R visitClassStmt(Class stmt);
		R visitFunctionStmt(Function stmt);
		R visitVarStmt(Var stmt);
		R visitValStmt(Val val);
		/* expr */
		R visitExpressionStmt(Expression stmt);
		/* print */
		R visitPrintStmt(Print stmt);
		/* ctrl */
		R visitWhileStmt(While stmt);
		R visitBreakStmt(Break stmt);
		R visitContinueStmt(Continue stmt);
		R visitReturnStmt(Return stmt);
	}

	// Nested Stmt classes here...

	static class Module extends Stmt {
		Module(Token name) {
			this.type = "Type";
			this.name = name;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitModuleStmt(this);
		}

		final Token name;
	}

	static class Class extends Stmt {
		Class(Token name, Expr.Variable superclass, List<Stmt.Function> methods, List<Function> classMethods) {
			this.type = "Type";
			this.name = name;
			this.superclass = superclass;
			this.methods = methods;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitClassStmt(this);
		}

		final Token name;
		final Expr.Variable superclass;
		final List<Stmt.Function> methods;
	}

	static class Expression extends Stmt {
		Expression(Expr expression) {
			this.expr = expression;
			this.type = expression.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}

		final Expr expr;
	}

	static class Function extends Stmt {
		Function(Token name, Expr.Function body) {
			this.name = name;
			this.body = body;
			this.type = "Func";
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitFunctionStmt(this);
		}

		final Token name;
		final Expr.Function body;
	}

	static class Print extends Stmt {
		Print(Expr expression) {
			this.expression = expression;
			type = "Void";
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}

		final Expr expression;
	}

	static class Return extends Stmt {
		Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
			type = value.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitReturnStmt(this);
		}

		final Token keyword;
		final Expr value;
	}

	static class Var extends Stmt {
		Var(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
			type = initializer.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitVarStmt(this);
		}

		final Token name;
		final Expr initializer;
	}

	static class Val extends Stmt {
		Val(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
			type = initializer.type;
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitValStmt(this);
		}

		final Token name;
		final Expr initializer;
	}

	static class While extends Stmt {
		While(Expr condition, Expr body) {
			this.condition = condition;
			this.body = body;
			type = "Void";
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitWhileStmt(this);
		}

		final Expr condition;
		final Expr body;
	}

	static class Break extends Stmt {
		Break() {
			type = "Void";
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitBreakStmt(this);
		}

	}

	static class Continue extends Stmt {
		Continue() {
			type = "Void";
		}

		<R> R accept(Visitor<R> visitor) {
			return visitor.visitContinueStmt(this);
		}

	}

	abstract <R> R accept(Visitor<R> visitor);
}
