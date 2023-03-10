package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Each method for parsing a grammar rule produces a syntax tree for that rule.
 * When the body contains a non-terminal, the corresponding method is called.
 */
public class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;

    // track the next token to be parsed
    private int current;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.current = 0;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    /**
     * expression -> assignment ;
     *
     * @return
     */
    private Expr expression() {
        return assignment();
    }

    /**
     * declaration  -> varDecl
     * | statement ;
     *
     * @return
     */
    private Stmt declaration() throws ParseError {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError parseError) {
            synchronize();
            return null;
        }
    }

    /**
     * statement -> exprStmt
     * | printStmt ;
     *
     * @return
     */
    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    /**
     * forStmt -> "for" "(" ( varDecl | exprStmt | ";" )
     * expression? ";"
     * expression? ")" statement ;
     * <p>
     * for (initializer ; condition ; increment)
     *
     * @return
     */
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            // initializer omitted
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        // second `point` in the loop
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        } // if false, condition is omitted
        consume(SEMICOLON, "Expect ';' after condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        } // if false increment is omitted

        consume(RIGHT_PAREN, "Expect ')' after clauses.");

        // body of the for loop is all that remains
        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        if (condition == null) {
            // infinite loop if no condition provided
            condition = new Expr.Literal(true);
        }

        // our for loop is just a while loop
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    /**
     * ifStmt -> "if" "(" expression ")" statement ;
     * ( "else" statement )? ;
     *
     * @return
     */
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        final Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'if'.");

        final Stmt thenBranch = statement();
        Stmt elseBranch = null;

        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        final Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after while.");
        final Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        final Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    /**
     * varDecl  -> "var" IDENTIFIER ( "=" expression )? ";" ;
     *
     * @return
     */
    private Stmt varDeclaration() {
        // Consume the identifier for the variable declared i.e `var myVar`
        final Token name = consume(IDENTIFIER, "Expect variable name");

        Expr initializer = null;
        if (match(EQUAL)) {
            //here `var myVar = expression`
            initializer = expression();
        }

        // Either way, we expect a declaration without initialization, `var myVar;`
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }


    /**
     * exprStmt -> expression ";" ;
     *
     * @return
     */
    private Stmt expressionStatement() {
        final Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Expression(expr);
    }

    /**
     * Consume declarations inside a block.
     *
     * @return the list of declarations contained in the block
     */
    private List<Stmt> block() {
        final List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * assignment -> IDENTIFIER "=" assignment | logic_or ;
     *
     * @return
     */
    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            final Token assignmentTarget = previous();
            final Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                final Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            error(assignmentTarget, "Invalid assignment target.");
        }
        // otherwise it's a `logical or`
        return expr;
    }

    /**
     * logic_or -> logic_and ( "or" logic_and )* ;
     */
    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * logic_and -> equality ( "and" equality )* ;
     */
    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    /**
     * equality -> comparison ( ( "!=" | "==" ) comparison )* ;
     *
     * @return
     */
    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            // match consumes the token when true, so we'll need the previous token
            final Token operator = previous();
            final Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        // no more equality operators
        return expr;
    }

    /**
     * comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
     *
     * @return
     */
    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            final Token operator = previous();
            final Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * term -> factor ( ( "-" | "+" ) factor )* ;
     *
     * @return
     */
    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            final Token operator = previous();
            final Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * factor -> unary ( ( "/" | "*" ) unary )* ;
     *
     * @return
     */
    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            final Token operator = previous();
            final Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * unary    -> ( "!" | "-" ) unary
     * | primary ;
     *
     * @return
     */
    private Expr unary() {
        if (match(BANG, MINUS)) {
            final Token operator = previous();
            final Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    /**
     * primary         -> NUMBER | STRING | "true" | "false" | "nil"
     * | "(" expression ")" ;
     *
     * @return
     */
    private Expr primary() {
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }

        if (match(FALSE)) {
            return new Expr.Literal(false);
        }

        if (match(NIL)) {
            return new Expr.Literal(null);
        }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            final Expr expr = expression();
            // if we have an opening paren, verify that this ends in a closing paren
            // after parsing the expression inside
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (final TokenType type : types) {
            // advance only if the token is of the expected type
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Consume the token type `type` if it's as expected, otherwise throws ParseError
     *
     * @param type    the expected token type
     * @param message the potential error message, if the type is not as expected
     * @return the current token
     * @throws ParseError thrown if the `type` is not as expected
     */
    private Token consume(TokenType type, String message) throws ParseError {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    /**
     * Determine if the current token is of the given type
     *
     * @param tokenType the type of the token
     * @return true if token is as expected
     */
    private boolean check(TokenType tokenType) {
        if (isAtEnd()) return false;
        return peek().tokenType == tokenType;
    }

    /**
     * Advance the index to the next token
     *
     * @return the previously consumed token
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().tokenType == EOF;
    }

    /**
     * Check the current token but without advancing
     *
     * @return the token at the current index
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Check the previously consumed token
     *
     * @return the token at one less than the current token index
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    /**
     * Clear out call frames; attempt to discard tokens until finding a statement boundary
     */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            // end of a statement boundary
            if (previous().tokenType == SEMICOLON) {
                return;
            }

            switch (peek().tokenType) {
                case CLASS:
                case FOR:
                case FUN:
                case IF:
                case PRINT:
                case RETURN:
                case VAR:
                case WHILE:
                    return;
            }

            advance();
        }
    }
}
