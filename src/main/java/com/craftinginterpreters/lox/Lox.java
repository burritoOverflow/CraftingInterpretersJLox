package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public final class Lox {
    static boolean hadError = false;

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

        // display tokens for now
        System.out.println(new AstPrinter().print(expression));
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    /**
     * Display the error and context to the user via stderr
     *
     * @param line    the line where the error occurred
     * @param where
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

}