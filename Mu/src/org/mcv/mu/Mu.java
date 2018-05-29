package org.mcv.mu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;

import org.mcv.mu.Interpreter.InterpreterError;
import org.mcv.mu.Parser.ParserError;
import org.mcv.mu.Scanner.ScannerError;

import lombok.extern.slf4j.Slf4j;
import nl.novadoc.utils.config.Config;
import nl.novadoc.utils.config.sources.PropertiesSource;

@Slf4j
public class Mu {

	static boolean hadError = false;
	static List<String> lines = new ArrayList<>();
	static Environment main = new Environment("main");
	static ListMap<Object> system;

	public static void main(String[] args) {
		initializeStdLib();
		if (args.length > 1) {
			System.out.println("Usage: mu [script]");
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}

	static void runFile(String path) {
		runFileNoExit(path);
		if (hadError)
			System.exit(65);
	}

	static void runFileNoExit(String path) {
		String toRun = "";
		try {
			lines = Files.readAllLines(Paths.get(path));
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

	private static Form getNormalization() {
		String norm = (String) system.getOrDefault("unicodeNormalization", "NFD");
		switch(norm) {
		case "NFC": return Form.NFC;
		case "NFD": return Form.NFD;
		case "NFKC": return Form.NFKC;
		case "NFKD": return Form.NFKD;
		default: return null;
		}
	}

	public static String trimTrailing(String str) {
		if(str == null)
	      return null;
	    int len = str.length();
	    for( ; len > 0; len--) {
	    	if(!Character.isWhitespace(str.charAt(len - 1)))
	    		break;
	    }
	    return str.substring(0, len);
	}
	
	static void runPrompt() {
		try {
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

	static void initializeStdLib() {
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
		
		system = new ListMap<>();
		Config cfg = new Config(new PropertiesSource("mu.props"));
		system.put("spacesToATab", Integer.parseInt(cfg.getString("spacesToATab", "4")));
		system.put("maxRawStringTerminator", Integer.parseInt(cfg.getString("maxRawStringTerminator", "8")));
		system.put("unicodeNormalization", cfg.getString("unicodeNormalization", "NFD"));
		system.put("DEBUG", Boolean.valueOf(cfg.getString("DEBUG", "false")));
		main.define("System", new Result(system, new Type.MapType(Type.String, Type.Any)), true, true);
	}

	private static void run(String source) {
		System.err.println("Scanning...");
		Scanner scanner = new Scanner(source, main);
		List<Token> tokens = scanner.scanTokens();
		System.err.flush();		
		System.err.println("Parsing...");
		Parser parser = new Parser(tokens, main);
		List<Expr> statements = parser.parse();

		// Stop if there was a syntax error.
		if (hadError) {
			System.err.println("Had errors: program will not run...");
			System.err.flush();		
			return;
		}
		
		System.err.flush();		
		System.err.println("Running...");
		new Interpreter(main).interpret(statements);
	}

	public static Expr parse(String source) {
		Scanner scanner = new Scanner(source, main);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens, main);
		List<Expr> statements = parser.parse();

		// Stop if there was a syntax error.
		if (hadError)
			return null;

		return statements.get(statements.size() - 1);
	}


	public static Result eval(String source) {
		Scanner scanner = new Scanner(source, main);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens, main);
		List<Expr> statements = parser.parse();
		// Stop if there was a syntax error.
		if (hadError)
			return null;
		Interpreter interpreter = new Interpreter(main);
		Result result = null; 
		for(Expr expr : statements) {
			result = interpreter.evaluate(expr);
		}
		return result;
	}

	static void error(Exception e) {
		hadError = true;
		if(e instanceof MuException) {
			MuException mue = (MuException)e;
			System.err.println("Uncaught exception at: " + mue.line + " (" + mue.expr + "): " + mue.getMessage());
		} else if(e instanceof InterpreterError) {
			InterpreterError ie = (InterpreterError)e;
			System.err.println("Runtime error at: " + ie.line + " (" + ie.expr + "): " + ie.getMessage());
		} else if(e instanceof ParserError) {
			ParserError pe = (ParserError)e;
			System.err.println("Parse error at: " + pe.line + " (" + pe.tok + "): " + pe.getMessage());
		} else if(e instanceof ScannerError) {
			ScannerError se = (ScannerError)e;
			System.err.println("Scan error at: " + se.line + " (" + new String(Character.toChars(se.cp)) + "): " + se.getMessage());
		} else {
			System.err.println("General error " + e.toString());
		}
		if((Boolean)system.getOrDefault("DEBUG", "false")) {
			log.error(e.toString(), e);
		}
	}
	
	static String getLine(int lineno) {
		return lines.get(lineno);
	}
}