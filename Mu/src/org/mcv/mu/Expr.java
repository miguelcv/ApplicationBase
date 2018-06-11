package org.mcv.mu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class Expr {

	private static final String PUBLIC = "public";

	Type type = Type.None;
	int line;
	
	interface Visitor {
		/* decl */
		Result visitTemplateDef(TemplateDef expr);
		Result visitVarDef(Var expr);
		Result visitValDef(Val expr);
		Result visitTypeDefExpr(TypeDef expr);
		Result visitImportExpr(Import expr);
		
		/* print */
		Result visitPrintExpr(Print expr);
		Result visitAssertExpr(Assert expr);
		
		/* ctrl */
		Result visitIfExpr(If expr);
		Result visitSelectExpr(Select select);
		Result visitForExpr(For expr);
		Result visitWhileExpr(While expr);
		Result visitBreakExpr(Break expr);
		Result visitContinueExpr(Continue expr);
		Result visitReturnExpr(Return expr);
		Result visitAopExpr(Aop expr);
		Result visitGetterExpr(Getter expr);
		Result visitSetterExpr(Setter expr);
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

		Result accept(Visitor visitor) {
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

		Var(List<Token> names, Expr initializer, Expr.Map where, Attributes attributes) {
			this.names.addAll(names);
			this.initializer = initializer;
			this.attributes = attributes;
			this.where = where;
			this.line = names.get(0).line;
		}
		
		Var(Token name, Expr.Map where, Attributes attributes) {
			// for statement for var i in ...
			this.initializer = new Expr.Literal(name, 0);
			type = Type.Int;
			this.names.add(name);
			this.attributes = attributes;
			this.where = where;
			this.line = name.line;			
		}
		
		Var(Token name, Expr initializer, Expr.Map where, Attributes attributes) {
			this.names.add(name);
			this.initializer = initializer;
			this.attributes = attributes;
			this.where = where;
			this.line = name.line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitVarDef(this);
		}

		final List<Token> names = new ArrayList<>();
		final Expr initializer;
		final Attributes attributes;
		Expr.Map where;
		@Override
		public String toString() {
			return "var " + names + ":" + initializer;
		}
	}

	static class Val extends Expr {
		
		Val(List<Token> names, Expr initializer, Expr.Map where, Attributes attributes) {
			this.names.addAll(names);
			this.initializer = initializer;
			this.attributes = attributes;
			this.where = where;
			this.line = names.get(0).line;
		}
		
		Val(Token name, Expr initializer, Map where, Attributes attributes) {
			this.names.add(name);
			this.initializer = initializer;
			this.attributes = attributes;
			this.where = where;
			this.type = initializer.type;
			this.line = name.line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitValDef(this);
		}

		final List<Token> names = new ArrayList<>();
		final Expr initializer;
		Expr.Map where;
		final Attributes attributes;
		@Override
		public String toString() {
			return "val " + names + ":" + initializer;
		}
	}
	
	static class Aop extends Expr {
		Aop(Token name, Expr callable, Block block) {
			this.name = name;
			this.callable = callable;
			this.block = block;
			this.line = name.line;
		}

		Result accept(Visitor visitor) {
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

	static class Getter extends Expr {
		Getter(Token name, List<Token> ids, Block block) {
			this.name = name;
			this.variables = ids;
			this.block = block;
			this.line = name.line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitGetterExpr(this);
		}

		final Token name;
		final List<Token> variables;
		final Block block;
		@Override
		public String toString() {
			return name.lexeme + " " + variables + ":" + block;
		}
	}

	static class Setter extends Expr {
		Setter(Token name, List<Token> variables, Block block) {
			this.name = name;
			this.variables = variables;
			this.block = block;
			this.line = name.line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitSetterExpr(this);
		}

		final Token name;
		final List<Token> variables;
		final Block block;
		@Override
		public String toString() {
			return name.lexeme + " " + variables + ":" + block;
		}
	}

	static class Print extends Expr {
		Print(Token name, Expr expression) {
			this.expression = expression;
			type = Type.Void;
			this.line = name.line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitPrintExpr(this);
		}

		final Expr expression;
		@Override
		public String toString() {
			return "print " + expression;
		}
	}

	static class Assert extends Expr {
		Assert(Token name, Expr expression, String msg, Set criteria) {
			this.expression = expression;
			this.msg = msg;
			type = Type.Void;
			this.criteria = criteria;
			this.line = name.line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitAssertExpr(this);
		}

		final Expr expression;
		final String msg;
		Set criteria;
		@Override
		public String toString() {
			return "assert " + expression + " " + msg;
		}
	}

	static class Return extends Expr {
		Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
			type = value.type;
			this.line = keyword.line;
		}

		Result accept(Visitor visitor) {
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

		Result accept(Visitor visitor) {
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
		While(Token name, Expr condition, Expr body, Expr atEnd, Set criteria) {
			this.condition = condition;
			this.body = body;
			this.criteria = criteria;
			this.atEnd = atEnd;
			type = Type.Void;
			this.line = name.line;
			if(name.lexeme.equals(Keyword.UNTIL.name())) {
				invert = true;
			} else {
				invert = false;
			}
		}

		Result accept(Visitor visitor) {
			return visitor.visitWhileExpr(this);
		}

		final Expr condition;
		final boolean invert;
		final Expr body;
		Set criteria;
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

		Result accept(Visitor visitor) {
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

		Result accept(Visitor visitor) {
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

		Result accept(Visitor visitor) {
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
		
		Result accept(Visitor visitor) {
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
			type = new Type.MapType(Type.Any);
			this.line = name.line;
		}
		
		List<Expr> mappings;
		
		Result accept(Visitor visitor) {
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
		
		Result accept(Visitor visitor) {
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
		
		Result accept(Visitor visitor) {
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
		
		Result accept(Visitor visitor) {
			return visitor.visitMappingExpr(this);
		}
		@Override
		public String toString() {
			return key + " → " + value;
		}
	}

	static class Assign extends Expr {
		
		Assign(Token var, Expr value, Token op) {
			this.var = List.of(var);
			this.value = value;
			this.op = op;
			type = value.type;
			this.line = var.line;
		}

		Assign(List<Token> var, Expr value, Token op) {
			this.var = var;
			this.value = value;
			this.op = op;
			type = value.type;
			this.line = var.get(0).line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitAssignExpr(this);
		}

		final List<Token> var;
		final Expr value;
		final Token op;
		@Override
		public String toString() {
			return var + " ← " + value;
		}
	}

	static class Binary extends Expr {
		Binary(Expr left, Token operator, Expr right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
			if((isRelOp(this)) && isRelOp(right)) {
				//LEFT:	a 
				//      RELOP
				//RIGHT:(b RELOP c)  ==>
				//LEFT   a RELOP b
				//       AND
				//RIGHT  b RELOP c
				Token and = new Token(Soperator.AND, "&", "&", operator.line);
				this.left = new Binary(this.left, operator, ((Binary)right).left);
				this.operator = and;
				this.right = right;
			}
			type = left.type.equals(right.type) ? left.type :
				left.type.equals(Type.Any) ? right.type :
				right.type.equals(Type.Any) ? left.type:
				widen(left.type, right.type);
			this.line = operator.line;
		}
		
		private boolean isRelOp(Expr expr) {
			if(expr instanceof Expr.Binary) {
				Binary bin = (Binary)expr;
				if(isRelOperator(bin.operator)) {
					return true;
				}
				return isRelOp(bin.left) && isRelOp(bin.right);
			}
			return false;
		}

		private boolean isRelOperator(Token tok) {
			return tok.type == Soperator.LESS ||
					tok.type == Soperator.LESS_EQUAL ||
					tok.type == Soperator.GREATER ||
					tok.type == Soperator.GREATER_EQUAL ||
					tok.type == Soperator.EQUAL ||
					tok.type == Soperator.NOT_EQUAL ||
					tok.type == Soperator.EQEQ ||
					tok.type == Soperator.NEQEQ;
		}

		private Type widen(Type t1, Type t2) {
			
			Object func1 = t1.interfaces.get(new Signature("to" + t2, t2, t1));
			Object func2 = t2.interfaces.get(new Signature("to" + t1, t1, t2));

			if (func1 != null && func1 instanceof Method) {
				return Interpreter.typeFromClass(((Method)func1).getReturnType());
			} else if (func2 != null && func2 instanceof Method) {
				return Interpreter.typeFromClass(((Method)func2).getReturnType());
			} else {
				return Type.Void;
			}
		}

		Result accept(Visitor visitor) {
			return visitor.visitBinaryExpr(this);
		}

		Expr left;
		Token operator;
		Expr right;
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

		Result accept(Visitor visitor) {
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

		Result accept(Visitor visitor) {
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

		Result accept(Visitor visitor) {
			return visitor.visitBlockExpr(this);
		}		
		@Override
		public String toString() {
			return "block ()";
		}
	}

	static class If extends Expr {
		If(Token name, Expr condition, Expr thenBranch, Expr elseBranch, Set criteria) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
			this.criteria = criteria;
			type = Type.Void;
			this.line = name.line;
			if(name.lexeme.equals(Keyword.UNLESS.name())) {
				invert = true;
			} else {
				invert = false;
			}
		}

		Result accept(Visitor visitor) {
			return visitor.visitIfExpr(this);
		}

		final Expr condition;
		final boolean invert; 
		final Expr thenBranch;
		final Expr elseBranch;
		Set criteria;
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

		Result accept(Visitor visitor) {
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

		Result accept(Visitor visitor) {
			return visitor.visitLiteralExpr(this);
		}

		final Object value;
		@Override
		public String toString() {
			return String.valueOf(value);
		}
	}
	
	static class Variable extends Expr {
		Variable(Token name) {
			this.name = name;
			this.type = Type.Any;
			this.line = name.line;
		}

		public Variable(Token name, Type type) {
			this.name = name;
			this.type = type;
			this.line = name.line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitVariableExpr(this);
		}

		final Token name;
		@Override
		public String toString() {
			return name.lexeme;
		}
	}

	static class TypeLiteral extends Expr {
		TypeLiteral(Token name, Type type, Attributes attributes) {
			this.type = Type.Type;
			this.literal = type;
			this.attr = attributes;
			this.line = name.line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitTypeLiteralExpr(this);
		}
		
		final Type literal;
		Attributes attr;
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

		Result accept(Visitor visitor) {
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

		public Dot(Expr current, Expr next, boolean safe) {
			this.current = current;
			this.next = next;
			this.safe = safe;
		}
		
		Result accept(Visitor visitor) {
			return visitor.visitDotExpr(this);
		}

		Expr current;
		Expr next;
		Expr value;
		boolean safe;
		@Override
		public String toString() {
			return current + "." + next;
		}
	}

	public static class Subscript extends Expr {

		public Subscript(Expr seq, Expr sub, boolean safe) {
			this.seq = seq;
			this.sub = sub;
			this.safe = safe;
		}
		
		Result accept(Visitor visitor) {
			return visitor.visitSubscriptExpr(this);
		}

		Expr seq;
		Expr sub;
		// for assignments
		Expr value;
		boolean safe;
		@Override
		public String toString() {
			return seq + "[" + sub + "]";
		}
	}

	public static class Call extends Expr {

		public Call(Expr current, Expr.Map next, boolean safe) {
			this.current = current;
			this.next = next;
			this.safe = safe;
		}
		
		Result accept(Visitor visitor) {
			return visitor.visitCallExpr(this);
		}

		Expr current;
		Expr.Map next;
		boolean safe;
		@Override
		public String toString() {
			return current + "(" + next + ")";
		}
	}

	public static class Import extends Expr {

		public Import(Token name, String gitrepo, String filename, String qid, Map where, Attributes attributes) {
			this.name = name;
			this.gitrepo = gitrepo;
			this.filename = filename;
			this.qid = qid;
			this.where = where;
			this.attributes = attributes;
		}

		Token name;
		String gitrepo;
		String filename;
		String qid;
		Map where;
		Attributes attributes;
		
		Result accept(Visitor visitor) {
			return visitor.visitImportExpr(this);
		}

	}

	abstract Result accept(Visitor visitor);
}
