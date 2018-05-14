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

	public static void main(String[] args) throws IOException {
		if(new File("Test.mu").exists()) {
			runFile("Test.mu");
		} else if (args.length > 1) {
			System.out.println("Usage: mu [script]");
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}

	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, StandardCharsets.UTF_8));
		if (hadError)
			System.exit(65);
		if (hadRuntimeError)
			System.exit(70);
	}

	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in, "UTF-8");
		BufferedReader reader = new BufferedReader(input);

		StringBuilder buffer = new StringBuilder();

		for (;;) {
			if(buffer.length() == 0) System.out.print("> ");
			else System.out.print(">> ");
			
			String line = reader.readLine();
			if(line.endsWith("!")) {
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
	}

	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		List<Stmt> statements = parser.parse();

		// Stop if there was a syntax error.
		if (hadError)
			return;

		interpreter.interpret(statements);
	}

	public static Expr eval(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		List<Stmt> statements = parser.parse();

		// Stop if there was a syntax error.
		if (hadError)
			return null;

		Stmt last = statements.get(statements.size() - 1);
		if(last instanceof Stmt.Expression) {
			return ((Stmt.Expression) last).expr;
		}
		return null;
	}

	static void error(int line, String message) {
		report(line, "", message);
	}

	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}

	static void error(Token token, String message) {
		if (token.type == TokenType.EOF) {
			report(token.line, " at end", message);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message);
		}
	}

	static void runtimeError(Exception error) {
		System.err.println(error.getMessage());
		hadRuntimeError = true;
	}
}