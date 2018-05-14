package org.mcv.app;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Resolver extends Base {

	private static final String DOTJAVA = ".java";
	private static String[] magicClasses = new String[] {Application.class.getName()};
	private static List<String> magicMethods = new ArrayList<>(); 
	static Application staticApp = new Application("resolver");

	static Map<StackTraceElement, Resolver> cache = new HashMap<>();
	Set<String> packages = new HashSet<>();
	StackTraceElement ste;
	String declaringClass;
	String resolvedName;
	Class<?> resolvedClass;

	public Resolver(String name) {

		super(name);
		
		Optional<StackTraceElement> maybeSte = resolveStack(Thread.currentThread().getStackTrace());
		
		if(!maybeSte.isPresent()) {
			app.getLog().error("Cannot find calling method");
			return;
		}
		
		ste = maybeSte.get();
		declaringClass = ste.getClassName();

		try {
			if (cache.containsKey(ste)) {
				// DEBUG
				Application.print("Resolved from cache");
				Resolver solver = cache.get(ste);
				this.resolvedName = solver.resolvedName;
				this.resolvedClass = solver.resolvedClass;
			} else {				
				notInCache();
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	private void notInCache() throws ParseException {
		InputStream is = toStream(ste.getClassName());
		CompilationUnit cu = null;
		if(is != null) {
			Application.print("From classpath");
			 cu = JavaParser.parse(is);
		} else {
			Application.print("From file " + ste.getClassName());
			File src = toFile(ste.getClassName());
			try {
				cu = JavaParser.parse(src);
			} catch(Exception e) {
				Application.print("Error reading file " + ste.getClassName());
				throw new WrapperException(e);
			}
		}
		packages.add("");
		packages.add("java.lang");
		packages.add(cu.getPackage().getName().toStringWithoutComments());
		List<ImportDeclaration> imports = cu.getImports();
		for (ImportDeclaration imp : imports) {
			String pkgname = imp.getName().toString();
			int ix = pkgname.lastIndexOf('.');
			pkgname = pkgname.substring(0, ix);
			if (imp.isStatic()) {
				ix = pkgname.lastIndexOf('.');
				pkgname = pkgname.substring(0, ix);
			}
			packages.add(pkgname);
		}

		for(String pkg : packages) {
			debug(pkg);
		}
		
		a.visit(cu, null);

		if (resolvedName != null && resolvedClass != null) {
			cache.put(ste, this);
			/* do NOT persist resolver... */
		} else {
			throw new ApplicationException(String.format("Resolver could not find call at line %d in class %s", ste.getLineNumber(), ste.getClassName()));
		}
	}
	
	VoidVisitorAdapter<Object> a = new VoidVisitorAdapter<Object>() {

		private Optional<Class<?>> getClassFromType(String type, String base) {
			for (String pkg : packages) {
				try {
					Application.print("TRY " + pkg + "." + type);
					return Optional.of(Class.forName(pkg + "." + type));
				} catch (ClassNotFoundException e) {
					// continue
				}
			}
			try {
				Application.print("TRY " + base + "$" + type);
				return Optional.of(Class.forName(base + "$" + type));
			} catch(ClassNotFoundException e2) {
				Application.print("Class not found  " + base + "$" + type);
				return Optional.empty();
			}
		}

		@Override
		public void visit(VariableDeclarationExpr n, Object arg) {
			Type vartype = n.getType();
			List<VariableDeclarator> vars = n.getVars();
			String varname = vars.get(0).getId().getName();
			int varline = vars.get(0).getBeginLine();
			if (varline == ste.getLineNumber()) {
				Application.print("VARDECL: " + varname + " : " + vartype);
				resolvedName = varname;
				resolvedClass = getClassFromType(String.valueOf(vartype), declaringClass).orElse(Object.class);
				return;
			}
			super.visit(n, arg);
		}

		@Override
		public void visit(FieldDeclaration n, Object arg) {
			Type vartype = n.getType();
			List<VariableDeclarator> vars = n.getVariables();
			String varname = vars.get(0).getId().getName();
			int varline = vars.get(0).getBeginLine();
			if (varline == ste.getLineNumber()) {
				Application.print("FLDDECL: " + varname + " : " + vartype);
				resolvedName = varname;
				resolvedClass = getClassFromType(String.valueOf(vartype), declaringClass).orElse(Object.class);
				return;
			}
			super.visit(n, arg);
		}
	};

	
	private Optional<StackTraceElement> resolveStack(StackTraceElement[] stackTrace) {
		
		if(magicMethods.isEmpty()) {
			initMagicMethods();
		}

		boolean next = false;
		for(StackTraceElement stelt : stackTrace) {
			if(next) {
				return Optional.of(stelt);
			}
			if(magicMethods.contains(stelt.getMethodName()) && Arrays.asList(magicClasses).contains(stelt.getClassName())) {
				next = true;
			}
		}
		return Optional.empty();
	}

	private void initMagicMethods() {
		for(String magicClass : magicClasses) {
			try {
				Class<?> clazz = Class.forName(magicClass);
				for(Method method : clazz.getDeclaredMethods()) {
					if(method.getAnnotation(Magic.class) != null) {
						magicMethods.add(method.getName());
					}
				}
			} catch(Exception e) {/* what are we to do? */}
		}
	}

	private File toFile(String className) {
		File f = new File("src/" + className.replace(".", "/") + DOTJAVA);
		if (!f.exists()) {
			f = new File("test/" + className.replace(".", "/") + DOTJAVA);
		}
		return f;
	}

	private InputStream toStream(String className) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(className.replace(".", "/") + DOTJAVA);
	}

}
