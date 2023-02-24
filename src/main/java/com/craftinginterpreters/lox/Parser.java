package com.craftinginterpreters.lox;

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

    Expr parse() {
        try {
            return expression();
        } catch (ParseError parseError) {
            return null;
        }
    }

    private Expr expression() {
        return equality();
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
            return new Expr.Literal(TRUE);
        }

        if (match(FALSE)) {
            return new Expr.Literal(FALSE);
        }

        if (match(NIL)) {
            return new Expr.Literal(NIL);
        }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
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
