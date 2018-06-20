package org.mcv.mu;

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
		Result visitUnitDefExpr(UnitDefExpr expr);
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
		TemplateDef(String name, int line, String kind, Params params, Type returnType, Block body, Attributes attributes) {
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
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitTemplateDef(this);
		}

		final String name;
		String kind;
		Params params;
		Type returnType;
		Block body;
		Attributes attributes = new Attributes();
		@Override
		public String toString() {
			return kind + " " + name + params.types() + " => " + returnType;
		}
	}

	public static class UnitDefExpr extends Expr {
		
		UnitDefExpr(String name, int line, boolean si, String unit, String units, Expr offset, Expr factor, Attributes attrs) {
			this.name = name;
			this.si = si;
			this.type = Type.Unit;
			this.unit = unit;
			this.units = units;
			this.attributes = attrs;
			this.offset = offset;
			this.factor = factor;
			if(attributes.isLocal(false)) {
				attributes.put(PUBLIC, false);
			} else {
				attributes.put(PUBLIC, true);
			}
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitUnitDefExpr(this);
		}
			
		final String name;
		final String unit;
		Expr offset;
		Expr factor;
		String units;
		boolean si;
		Attributes attributes = new Attributes();

		@Override
		public String toString() {
			return "unit " + name;
		}
	}

	static class Var extends Expr {

		Var(List<String> names, int line, Expr initializer, Expr.Map where, Attributes attributes) {
			this.names.addAll(names);
			this.initializer = initializer;
			this.attributes = attributes;
			this.where = where;
			this.line = line;
		}
		
		Var(String name, int line, Expr.Map where, Attributes attributes) {
			// for statement for var i in ...
			this.initializer = new Expr.Literal(line, 0);
			type = Type.Int;
			this.names.add(name);
			this.attributes = attributes;
			this.where = where;
			this.line = line;			
		}
		
		Var(String name, int line, Expr initializer, Expr.Map where, Attributes attributes) {
			this.names.add(name);
			this.initializer = initializer;
			this.attributes = attributes;
			this.where = where;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitVarDef(this);
		}

		final List<String> names = new ArrayList<>();
		final Expr initializer;
		Attributes attributes = new Attributes();
		Expr.Map where;
		@Override
		public String toString() {
			return "var " + names + ":" + initializer;
		}
	}

	static class Val extends Expr {
		
		Val(List<String> names, int line, Expr initializer, Expr.Map where, Attributes attributes) {
			this.names.addAll(names);
			this.initializer = initializer;
			this.attributes = attributes;
			this.where = where;
			this.line = line;
		}
		
		Val(String name, int line, Expr initializer, Map where, Attributes attributes) {
			this.names.add(name);
			this.initializer = initializer;
			this.attributes = attributes;
			this.where = where;
			this.type = initializer.type;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitValDef(this);
		}

		final List<String> names = new ArrayList<>();
		final Expr initializer;
		Expr.Map where;
		Attributes attributes = new Attributes();
		@Override
		public String toString() {
			return "val " + names + ":" + initializer;
		}
	}
	
	static class Aop extends Expr {
		Aop(String name, int line, Expr callable, Block block) {
			this.name = name;
			this.callable = callable;
			this.block = block;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitAopExpr(this);
		}

		final String name;
		final Expr callable;
		final Block block;
		@Override
		public String toString() {
			return "AOP " + name + " " + callable + ":" + block;
		}
	}

	static class Getter extends Expr {
		Getter(String name, int line, List<Token> ids, Block block) {
			this.name = name;
			this.variables = ids;
			this.block = block;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitGetterExpr(this);
		}

		final String name;
		final List<Token> variables;
		final Block block;
		@Override
		public String toString() {
			return name + " " + variables + ":" + block;
		}
	}

	static class Setter extends Expr {
		Setter(String name, int line, List<Token> variables, Block block) {
			this.name = name;
			this.variables = variables;
			this.block = block;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitSetterExpr(this);
		}

		final String name;
		final List<Token> variables;
		final Block block;
		@Override
		public String toString() {
			return name + " " + variables + ":" + block;
		}
	}

	static class Print extends Expr {
		Print(int line, Expr expression) {
			this.expression = expression;
			type = Type.Void;
			this.line = line;
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
		Assert(int line, Expr expression, String msg, Set criteria) {
			this.expression = expression;
			this.msg = msg;
			type = Type.Void;
			this.criteria = criteria;
			this.line = line;
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
		Return(int line, Expr value) {
			this.value = value;
			type = value.type;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitReturnExpr(this);
		}

		final Expr value;
		@Override
		public String toString() {
			return "return " + value;
		}
	}

	static class Throw extends Expr {
		Throw(int line, Expr thrown) {
			this.thrown = thrown;
			type = thrown.type;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitThrowExpr(this);
		}

		final Expr thrown;
		@Override
		public String toString() {
			return "throw " + thrown;
		}
	}

	static class While extends Expr {
		While(String name, int line, Expr condition, Expr body, Expr atEnd, Set criteria) {
			this.condition = condition;
			this.body = body;
			this.criteria = criteria;
			this.atEnd = atEnd;
			type = Type.Void;
			this.line = line;
			if(name.equals(Keyword.UNTIL.name())) {
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
		For(int line, Expr.Var var, Expr range, Block body, Block atEnd) {
			this.var = var;
			this.range = range;
			this.body = body;
			type = Type.Void;
			this.line = line;
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
		Break(int line) {
			type = Type.Void;
			this.line = line;
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
		Continue(int line) {
			type = Type.Void;
			this.line = line;
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
		Seq(int line, List<Expr> exprs) {
			this.exprs = exprs;
			eltType = exprs.isEmpty() ? Type.Any : exprs.get(0).type;
			type = new Type.ListType(eltType);
			this.line = line;
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
		Map(int line, List<Expr> exprs) {
			this.mappings = exprs;
			type = new Type.MapType(Type.Any);
			this.line = line;
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
		Set(int line, java.util.Set<Expr> exprs) {
			this.exprs = exprs;
			eltType = exprs.isEmpty() ? Type.Any : exprs.iterator().next().type;
			type = new Type.SetType(eltType);
			this.line = line;
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
		Range(int line, Expr start, Expr end, boolean startIncl, boolean endIncl) {
			this.start = start;
			this.startIncl = startIncl;
			this.end = end;
			this.endIncl = endIncl;
			type = new Type.Range("Range", start, end);
			this.line = line;
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
		Mapping(int line, String  key, Expr value, boolean arrow) {
			this.key = key;
			this.value = value;
			type = value.type;
			this.line = line;
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
		
		Assign(String var, int line, Expr value, Token op) {
			this.var = new ArrayList<>();
			this.var.add(var);
			this.value = value;
			this.op = op;
			type = value.type;
			this.line = line;
		}

		Assign(List<String> var, int line, Expr value, Token op) {
			this.var = var;
			this.value = value;
			this.op = op;
			type = value.type;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitAssignExpr(this);
		}

		final List<String> var;
		final Expr value;
		final Token op;
		@Override
		public String toString() {
			return var + " ← " + value;
		}
	}

	static class Binary extends Expr {
		Binary(int line, Expr left, String operator, Expr right) {
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
				this.left = new Binary(line, this.left, operator, ((Binary)right).left);
				this.operator = "&";
				this.right = right;
			}
			this.type = this.left.type;
			this.line = line;
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

		private boolean isRelOperator(String op) {
			return op.equals("<") ||
					op.equals("<=") ||
					op.equals(">") ||
					op.equals(">=") ||
					op.equals("=") ||
					op.equals("~=") ||
					op.equals("==") ||
					op.equals("~==");
		}

		Result accept(Visitor visitor) {
			return visitor.visitBinaryExpr(this);
		}

		Expr left;
		String operator;
		Expr right;
		@Override
		public String toString() {
			return left + " " + operator + " " + right;
		}
	}

	static class Unary extends Expr {
		Unary(int line, String operator, Expr right) {
			this.operator = operator;
			this.right = right;
			type = right.type;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitUnaryExpr(this);
		}

		final String operator;
		final Expr right;
		@Override
		public String toString() {
			return operator + right;
		}
	}

	static class Postfix extends Expr {
		Postfix(int line, Expr left, String operator) {
			this.operator = operator;
			this.left = left;
			type = left.type;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitPostfixExpr(this);
		}

		final String operator;
		final Expr left;
		@Override
		public String toString() {
			return left + operator;
		}
	}

	static class Block extends Expr {
		
		List<Expr> expressions = new ArrayList<>();
		Expr last;
		
		Block(int line, List<Expr> expressions) {
			if(expressions.isEmpty()) {
				expressions.add(new Expr.Literal(line, null));
			}
			this.expressions = expressions;
			last = expressions.get(expressions.size() - 1);
			type = last.type;
			this.line = line;
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
		If(String name, int line, Expr condition, Expr thenBranch, Expr elseBranch, Set criteria) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
			this.criteria = criteria;
			type = Type.Void;
			this.line = line;
			if(name.equals(Keyword.UNLESS.name())) {
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
		Select(int line, Expr condition, List<Expr> whenExpressions, List<Expr> whenBranches, Expr elseBranch) {
			this.condition = condition;
			this.whenExpressions = whenExpressions;
			this.whenBranches = whenBranches;
			this.elseBranch = elseBranch;
			type = Type.Void;
			this.line = line;
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
		Literal(int line, Object value) {
			this.value = value;
			type = Interpreter.typeFromValue(value);
			this.line = line;
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
		Variable(String name, int line) {
			this.name = name;
			this.type = Type.Any;
			this.line = line;
		}

		public Variable(String name, int line, Type type) {
			this.name = name;
			this.type = type;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitVariableExpr(this);
		}

		final String name;
		
		@Override
		public String toString() {
			return name;
		}
	}

	static class TypeLiteral extends Expr {
		TypeLiteral(int line, Type type, Attributes attributes) {
			this.type = Type.Type;
			this.literal = type;
			this.attr = attributes;
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitTypeLiteralExpr(this);
		}
		
		final Type literal;
		Attributes attr = new Attributes();
		@Override
		public String toString() {
			return "type " + literal;
		}
	}

	static class TypeDef extends Expr {
		TypeDef(String name, int line, Type type, Attributes attributes) {
			this.name = name;
			this.type = Type.Type;
			this.literal = type;
			this.attributes = attributes;
			if(attributes.isLocal(false)) {
				attributes.put(PUBLIC, false);
			} else {
				attributes.put(PUBLIC, true);
			}
			this.line = line;
		}

		Result accept(Visitor visitor) {
			return visitor.visitTypeDefExpr(this);
		}
		
		final String name;
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
			this.line = current.line;
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
			this.line = seq.line;
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
			this.line = current.line;
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

		public Import(int line, Expr.Map imports, String repo, Attributes attributes) {
			this.repo = repo;
			this.imports = imports;
			this.attributes = attributes;
			this.line = line;
		}

		String repo;
		Expr.Map imports;
		Attributes attributes = new Attributes();
		
		Result accept(Visitor visitor) {
			return visitor.visitImportExpr(this);
		}

	}

	abstract Result accept(Visitor visitor);
}
