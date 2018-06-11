package org.mcv.mu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.mcv.math.BigInteger;
import org.mcv.mu.Expr.TemplateDef;
import org.mcv.mu.Interpreter.InterpreterError;
import org.mcv.mu.Parser.ParserError;
import org.mcv.mu.Scanner.ScannerError;

import lombok.extern.slf4j.Slf4j;
import nl.novadoc.utils.FileUtils;
import nl.novadoc.utils.config.Config;
import nl.novadoc.utils.config.sources.PropertiesSource;

@Slf4j
public class Mu {

	boolean hadError = false;
	List<String> lines = new ArrayList<>();
	Environment main = new Environment("main");
	ListMap<Object> system = new ListMap<>();
	static Config cfg;
	
	public static void main(String... args) {
		cfg = new Config(new PropertiesSource("mu.props"));
		new Mu(args);
	}
	
	public Mu(String... args) {
		Pair<ListMap<Object>,ListMap<Object>> pair = parseArgs(args);
		ListMap<Object> flags = pair.left;
		ListMap<Object> arguments = pair.right;
		
		/* reset deps file: -reset-deps */
		if(flags.containsKey("reset-deps")) {
			resetDeps((String)arguments.get(0));
		}
		
		/* system properties: -key:value */
		for(Entry<String, Object> entry :  flags.entrySet()) {
			system.put(entry.getKey(), entry.getValue());
		}
		
		initializeStdLib();
		for(Entry<String, Object> entry : arguments.entrySet()) {
			main.define(entry.getKey(), new Result(entry.getValue(), Interpreter.typeFromClass(entry.getValue().getClass())), false, true);
		}
		main.define("$arguments", new Result(arguments, new Type.MapType(Type.Any)), false, true);

		if(flags.containsKey("eval")) {
			return;
		} else if(arguments.isEmpty()) {
			runPrompt();			
		} else {
			runFile((String)arguments.get(0));
		}
	}

	private void resetDeps(String file) {
		File depsfile = new File(file + ".deps");	
		depsfile.delete();
	}

	public Pair<ListMap<Object>,ListMap<Object>> parseArgs(String[] args) {
		ListMap<Object> flags = new ListMap<>();
		ListMap<Object> nonflags = new ListMap<>();
		
		for(String arg : args) {
			boolean isFlag = false;
			
			while(arg.startsWith("-") || arg.startsWith("/")) {
				isFlag = true;
				arg = arg.substring(1);
			}
			
			String[] kv;
			if(arg.indexOf('=') >= 0) {
				kv = arg.split("=");
			} else if(arg.indexOf(':') >= 0 && arg.indexOf(":\\") == -1) {
				kv = arg.split(":");
			} else {
				kv = new String[]{arg};
			}
			if(kv.length == 2) {
				(isFlag ? flags : nonflags).put(kv[0], convert(kv[1]));
			} else if(kv.length == 1) {
				if(isFlag) flags.put(arg, true);
				else nonflags.put(arg, arg);
			} else {
				// error
			}
		}
		return new Pair<>(flags, nonflags);
	}

	Object convert(String s) {
		if(s.equals("true")) return true;
		if(s.equals("false")) return false;
		try {
			return BigInteger.valueOf(Integer.parseInt(s));
		} catch(Exception e) {
			return s;
		}
	}
	
	void runFile(String file) {
		String toRun = "";
		try {
			Path path = Paths.get(file);
			system.put("currentDirectory", path.getParent().toString());
			system.put("currentFile", path.toString());

			lines = Files.readAllLines(path);
			List<String> normalized = new ArrayList<>();
			for(String line : lines) {
				/* trim all lines */
				line = trimTrailing(line);
				/* normalize to NFD */
				Form form = getNormalization();
				if(form == null) {
					normalized.add(line);
				} else {
					normalized.add(Normalizer.normalize(line, form));
				}
			}
			toRun = String.join("\n", normalized) + "\n";
		} catch (Exception e) {
			error(e);
			return;
		}
		run(toRun);
	}

	public Form getNormalization() {
		String norm = cfg.getString("unicodeNormalization", "NFD");
		switch(norm) {
		case "NFC": return Form.NFC;
		case "NFD": return Form.NFD;
		case "NFKC": return Form.NFKC;
		case "NFKD": return Form.NFKD;
		default: return null;
		}
	}

	public String trimTrailing(String str) {
		if(str == null)
	      return null;
	    int len = str.length();
	    for( ; len > 0; len--) {
	    	if(!Character.isWhitespace(str.charAt(len - 1)))
	    		break;
	    }
	    return str.substring(0, len);
	}
	
	void runPrompt() {
		try {
			system.put("currentDirectory", System.getProperty("user.dir"));

			InputStreamReader input = new InputStreamReader(System.in, "UTF-8");
			BufferedReader reader = new BufferedReader(input);

			StringBuilder buffer = new StringBuilder();

			for (;;) {
				if (buffer.length() == 0)
					System.out.print("> ");
				else
					System.out.print(">> ");

				String line = reader.readLine();
				lines.add(line);
				
				if (line.endsWith("!")) {
					line = line.substring(0, line.length() - 1);
					buffer.append(line).append('\n');
					run(buffer.toString());
					buffer = new StringBuilder();
					hadError = false;
					lines = new ArrayList<>();
				} else {
					buffer.append(line).append('\n');
				}
			}
		} catch (IOException ioe) {
			error(ioe);
		}
	}

	void initializeStdLib() {
		/* standard types */		
		main.define("None", new Result(Type.None, Type.Type), false, true);
		main.define("Void", new Result(Type.Void, Type.Type), false, true);
		main.define("Any", new Result(Type.Any, Type.Type), false, true);
		main.define("Bool", new Result(Type.Bool, Type.Type), false, true);
		main.define("Int", new Result(Type.Int, Type.Type), false, true);
		main.define("Real", new Result(Type.Real, Type.Type), false, true);
		main.define("Char", new Result(Type.Char, Type.Type), false, true);
		main.define("String", new Result(Type.String, Type.Type), false, true);
		main.define("Type", new Result(Type.Type, Type.Type), false, true);
		
		main.define("Inf", new Result(BigInteger.POSITIVE_INFINITY, Type.Int), false, true);
		main.define("NaN", new Result(BigInteger.NAN, Type.Int), false, true);
		
		system.put("user", System.getProperty("user.name"));
		system.put("arch", System.getProperty("os.arch"));
		system.put("os", System.getProperty("os.name"));
		system.put("osVersion", System.getProperty("os.version"));
		
		main.define("system", new Result(system, new Type.MapType(Type.Any)), true, true);
	}

	private void run(String source) {
		Scanner scanner = new Scanner(source, main, this);
		List<Token> tokens = scanner.scanTokens();
		System.err.flush();		
		Parser parser = new Parser(tokens, main, this);
		List<Expr> statements = parser.parse();

		// Stop if there was a syntax error.
		if (hadError) {
			System.err.println("Had errors: program will not run...");
			System.err.flush();
			hadError = false;
			return;
		}
		
		System.err.flush();		
		Interpreter intr = new Interpreter(main, this);
		// define $this...
		String path = (String)system.get("currentFile");
		if(path != null) {
			String funcname = FileUtils.getFileNameWithoutExtension(new File(path));
			Template mainFunc = new Template(funcname, main);
			main.define("$this", new Result(mainFunc, new Type.SignatureType((TemplateDef)mainFunc.def)), false, true);
		}
		intr.interpret(statements);		
	}

	public Expr parse(String source) {
		Scanner scanner = new Scanner(source, main, this);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens, main, this);
		List<Expr> statements = parser.parse();

		// Stop if there was a syntax error.
		if (hadError) {
			hadError = false;
			return null;
		}
		return statements.get(statements.size() - 1);
	}


	public Result eval(String source) {
		Scanner scanner = new Scanner(source, main, this);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens, main, this);
		List<Expr> statements = parser.parse();
		// Stop if there was a syntax error.
		if (hadError) {
			hadError = false;
			return null;
		}
		Interpreter interpreter = new Interpreter(main, this);
		Result result = null; 
		for(Expr expr : statements) {
			result = interpreter.evaluate(expr);
		}
		return result;
	}

	void error(Exception e) {
		hadError = true;
		if(e instanceof MuException) {
			MuException mue = (MuException)e;
			System.err.println("Uncaught exception at: " + mue.line + " (" + mue.expr + "): " + mue.value);
		} else if(e instanceof InterpreterError) {
			InterpreterError ie = (InterpreterError)e;
			System.err.println("Runtime error at: " + (ie.line+1) + " (" + ie.expr + "): " + ie.getMessage());
		} else if(e instanceof ParserError) {
			ParserError pe = (ParserError)e;
			System.err.println("Parse error at: " + pe.line + " (" + pe.tok + "): " + pe.getMessage());
		} else if(e instanceof ScannerError) {
			ScannerError se = (ScannerError)e;
			System.err.println("Scan error at: " + se.line + " (" + new String(Character.toChars(se.cp)) + "): " + se.getMessage());
		} else {
			System.err.println("General error " + e.toString());
		}
		if(cfg.getString("DEBUG", "false").equals("true")) {
			log.error(e.toString(), e);
		}
	}
	
	String getLine(int lineno) {
		return lines.get(lineno);
	}
}