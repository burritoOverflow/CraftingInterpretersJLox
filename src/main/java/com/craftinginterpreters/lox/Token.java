package com.craftinginterpreters.lox;

public class Token {
    final TokenType tokenType;
    final String lexeme;
    final Object literal;
    final int line;


    public Token(TokenType tokenType, String lexeme, Object literal, int line) {
        this.tokenType = tokenType;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public String toString() {
        return String.format("%s %s %s", tokenType, lexeme, literal);
    }
}
