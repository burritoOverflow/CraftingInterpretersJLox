package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class Lox {
    private static final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage jlox [script]");
            // author adopted conventions from `sysexits.h`; see page 40
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(64);
        if (hadRuntimeError) System.exit(70);
    }

    /**
     * Run a REPL and (for the time) display the tokens
     *
     * @throws IOException
     */
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (; ; ) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            run(line);
            hadError = false;
        }
    }

    /**
     * Run the interpreter for the given source string
     *
     * @param source the string to run in the interpreter
     */
    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        final Parser parser = new Parser(tokens);
        final Expr expression = parser.parse();

        // stop parsing if there is a syntax error
        if (hadError) return;

        interpreter.interpret(expression);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    /**
     * Display the error and context to the user via stderr
     *
     * @param line    the line where the error occurred
     * @param where   context for the location of the error
     * @param message informative string to output
     */
    private static void report(int line, String where, String message) {
        System.err.printf("[line %s] Error:%s %s%n", line, where, message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.tokenType == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, String.format(" at '%s'", token.lexeme), message);
        }
    }

    /**
     * Display an error to the user.
     *
     * @param error the error with which to provide context.
     */
    public static void runtimeError(RuntimeError error) {
        final String errorMessage = error.getMessage();
        System.err.println(String.format("%s\n[line %d]", errorMessage, error.token.line));
        hadRuntimeError = true;
    }
}