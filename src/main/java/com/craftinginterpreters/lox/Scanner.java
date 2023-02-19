package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

public class Scanner {
    /*
        start and current are offsets that index into the string
        start - points to the 1st char in the lexeme
        current - points to the character currently being considered
        line - tracks what source line current is on
    */

    // the source string provided to the interpreter
    private final String source;

    // the list of scanned tokens
    private final List<Token> tokens;

    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    public Scanner(String source) {
        this.source = source;
        this.tokens = new ArrayList<>();
    }

    /**
     * Add tokens until all characters consumed
     *
     * @return a List containing the Tokens
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // at the beginning of the next lexeme
            start = current;
            scanToken();
        }
        // once all tokens are consumed, add the final EOF
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    /**
     * Advance through the source string consuming lexemes and storing the corresponding tokens
     */
    private void scanToken() {
        final char c = advance();
        switch (c) {
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(MINUS);
                break;
            case '+':
                addToken(PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case '*':
                addToken(STAR);
                break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')) {
                    // comment is all contents until the end of the line; consume the entire line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                }
            case ' ':
            case '\r':
            case '\t':
                // ignore whitespace
                break;
            case '\n':
                line++;
                break;
            case '"':
                string();
                break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Lox.error(line, String.format("Unexpected Character: %c", c));
                }
        }
    }

    private void number() {
        // consume all characters while the next character is a digit
        while (isDigit(peek())) {
            advance();
        }

        // determine if floating point literal
        if (peek() == '.' && isDigit(peekNext())) {
            // we'll consume the `.`
            advance();
            // consume characters after the `.` for the remaining digits in the float literal
            while (isDigit(peek())) {
                advance();
            }
        }

        // add the number now that all digits have been collected
        final Double tokenValue = Double.parseDouble(source.substring(start, current));
        addToken(NUMBER, tokenValue);
    }

    private void string() {
        // consume characters until the end of the string
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated String");
            return;
        }

        // consume the closing "
        advance();

        // remove the quotes from the string
        // as `start` point to the 1st character in the lexeme, and we've consumed the last character
        final String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private void identifier() {
        // consume all valid characters in an identifier
        while (isAlphanumeric(peek())) {
            advance();
        }
        final String text = source.substring(start, current);
        final boolean isReservedToken = keywords.containsKey(text);
        if (isReservedToken) {
            final TokenType tokenType = keywords.get(text);
            addToken(tokenType);
        } else {
            // not a reserved word, identifier
            addToken(IDENTIFIER);
        }
    }

    /**
     * Only consume the character if it's what is expected
     * i.e determine if a single character lexeme/operator or a more complex operator
     * Make the determination between '!' or '!=', for example
     *
     * @param expected the anticipated character
     * @return true of the current character is as expected
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    /**
     * Perform lookahead for a single, current character (do not consume the character)
     *
     * @return the next character
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * Perform lookahead (do not consume) for an additional character (one greater than current)
     *
     * @return the character one index greater than the current index in the source string
     */
    private char peekNext() {
        if (current + 1 > source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private boolean isDigit(char c) {
        return '0' <= c && '9' >= c;
    }

    private boolean isAlpha(char c) {
        // allow a leading underscore
        return c == '_' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isAlphanumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    /**
     * Determine if all characters have been consumed
     *
     * @return true if all characters consumed
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Consume the next character and return it.
     *
     * @return - the current character
     */
    private char advance() {
        return source.charAt(current++);
    }

    /**
     * Create a new token for the text of the current lexeme
     *
     * @param tokenType the type of token to add to the Scanner's token collection
     */
    private void addToken(TokenType tokenType) {
        addToken(tokenType, null);
    }

    /**
     * Create a new token for the text of the current lexeme given the type and literal, store in the Scanner's collection
     *
     * @param tokenType the type of token to add to the Scanner's token collection
     * @param literal   the literal value of the token
     */
    private void addToken(TokenType tokenType, Object literal) {
        final String text = source.substring(start, current);
        tokens.add(new Token(tokenType, text, literal, line));
    }
}
