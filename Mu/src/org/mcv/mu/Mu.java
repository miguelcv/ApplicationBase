package org.mcv.mu;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Mu {

	static boolean hadError = false;
	private static boolean hadRuntimeError = false;
	private static final Interpreter interpreter = new Interpreter();

	public static void main(String[] args) {
		if (new File("Test.mu").exists()) {
			runFile("Test.mu");
		} else if (args.length > 1) {
			System.out.println("Usage: mu [script]");
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}

	private static void runFile(String path) {
		byte[] bytes;
		try {
			bytes = Files.readAllBytes(Paths.get(path));
		} catch (IOException e) {
			runtimeError(e, path);
			return;
		}
		run(new String(bytes, StandardCharsets.UTF_8));
		if (hadError)
			System.exit(65);
		if (hadRuntimeError)
			System.exit(70);
	}

	private static void runPrompt() {
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
				if (line.endsWith("!")) {
					line = line.substring(0, line.length() - 1);
					buffer.append(line).append('\n');
					run(buffer.toString());
					buffer = new StringBuilder();
					hadError = false;
					hadRuntimeError = false;
				} else {
					buffer.append(line).append('\n');
				}
			}
		} catch (IOException ioe) {
			runtimeError(ioe, "Could not read standard input");
		}
	}

	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		List<Expr> statements = parser.parse();

		// Stop if there was a syntax error.
		if (hadError)
			return;

		interpreter.interpret(statements);
	}

	public static Expr eval(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		List<Expr> statements = parser.parse();

		// Stop if there was a syntax error.
		if (hadError)
			return null;

		return statements.get(statements.size() - 1);
	}

	static void error(int line, String message) {
		report(line, "", message);
	}

	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}

	static void error(Token token, String message, Object... args) {
		if (token.type == Soperator.EOF) {
			report(token.line, " at end", String.format(message, args));
		} else {
			report(token.line, " at '" + token.lexeme + "'", String.format(message, args));
		}
	}

	static Object runtimeError(String msg, Object... args) {
		System.err.println(String.format(msg, args));
		hadRuntimeError = true;
		return null;
	}
	
	static Object runtimeError(Exception error, String msg, Object... args) {
		System.err.println(String.format(msg, args) + ": " + error.getMessage());
		hadRuntimeError = true;
		return null;
	}

}