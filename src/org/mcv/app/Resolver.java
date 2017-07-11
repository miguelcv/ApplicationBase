package org.mcv.app;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

@Data
@EqualsAndHashCode(callSuper = false)
public class Resolver extends Base {

	static Application staticApp = new Application("resolver");

	static Map<StackTraceElement, Resolver> cache = new HashMap<>();
	Set<String> packages = new HashSet<>();
	StackTraceElement ste;
	/* TODO Jackson bug deserializing STE !!! */
	String declaringClass;
	String resolvedName;
	Class<?> resolvedClass;

	static {
		List<Resolver> list = staticApp.getList(Resolver.class);
		for (Resolver solver : list) {
			StackTraceElement ste = new StackTraceElement(
					solver.getDeclaringClass(),
					solver.getSte().getMethodName(), solver.getSte()
							.getFileName(), solver.getSte().getLineNumber());
			solver.setSte(ste);
			cache.put(ste, solver);
		}
	}

	private Class<?> getClassFromType(String type) {
		for (String pkg : packages) {
			try {
				return Class.forName(pkg + "." + type);
			} catch (ClassNotFoundException e) {
			}
		}
		return null;
	}

	public Resolver(String name, Class<? extends Base> clazz) {

	}

	public Resolver() {

		ste = Thread.currentThread().getStackTrace()[3];
		declaringClass = ste.getClassName();

		VoidVisitorAdapter<Object> a = new VoidVisitorAdapter<Object>() {

			@SuppressWarnings("unchecked")
			@Override
			public void visit(VariableDeclarationExpr n, Object arg) {
				Type vartype = n.getType();
				List<VariableDeclarator> vars = n.getVars();
				String varname = vars.get(0).getId().getName();
				int varline = vars.get(0).getBeginLine();
				if (varline == ste.getLineNumber()) {
					resolvedName = varname;
					resolvedClass = (Class<? extends Base>) getClassFromType(String
							.valueOf(vartype));
					return;
				}
				super.visit(n, arg);
			}

			@SuppressWarnings("unchecked")
			@Override
			public void visit(FieldDeclaration n, Object arg) {
				Type vartype = n.getType();
				List<VariableDeclarator> vars = n.getVariables();
				String varname = vars.get(0).getId().getName();
				int varline = vars.get(0).getBeginLine();
				if (varline == ste.getLineNumber()) {
					resolvedName = varname;
					resolvedClass = (Class<? extends Base>) getClassFromType(String
							.valueOf(vartype));
					return;
				}
				super.visit(n, arg);
			}
		};

		try {
			if (cache.containsKey(ste)) {
				// DEBUG
				System.out.println("Resolved from cache");
				Resolver solver = cache.get(ste);
				this.resolvedName = solver.resolvedName;
				this.resolvedClass = solver.resolvedClass;
			} else {
				InputStream is = toStream(ste.getClassName());
				CompilationUnit cu = null;
				if(is != null) {
					System.out.println("From classpath");
					 cu = JavaParser.parse(is);
				} else {
					System.out.println("From file");
					File src = toFile(ste.getClassName());
					cu = JavaParser.parse(src);
				}
				packages.add("");
				packages.add("java.lang");
				packages.add(cu.getPackage().getName()
						.toStringWithoutComments());
				List<ImportDeclaration> imports = cu.getImports();
				for (ImportDeclaration imp : imports) {
					String name = imp.getName().toString();
					int ix = name.lastIndexOf('.');
					name = name.substring(0, ix);
					if (imp.isStatic()) {
						ix = name.lastIndexOf('.');
						name = name.substring(0, ix);
					}
					packages.add(name);
				}
				System.out.println(packages);

				a.visit(cu, null);

				if (resolvedName != null && resolvedClass != null) {
					cache.put(ste, this);
					app = staticApp;
					name = ste.getClassName() + "." + ste.getLineNumber();
					clazz = Resolver.class;
					children = new LinkedList<Long>();
					created = LocalDateTime.now();
					current = true;
					deleted = false;
					parent = 0;
					version = 1;
					json = Application.toJson(this);
					app.db.newRecord(this);
				} else {
					throw new Exception(String.format("Resolver could not find call at line %d in class %s", ste.getLineNumber(), ste.getClassName()));
				}
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	private File toFile(String className) {
		File f = new File("src/" + className.replace(".", "/") + ".java");
		if (!f.exists()) {
			f = new File("test/" + className.replace(".", "/") + ".java");
		}
		return f;
	}

	private InputStream toStream(String className) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(className.replace(".", "/") + ".java");
	}

}
