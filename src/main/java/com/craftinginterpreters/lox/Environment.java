package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    // stores the bindings that associate variables to values
    private final Map<String, Object> values;

    public Environment() {
        values = new HashMap<>();
    }

    Object get(Token name) throws RuntimeError {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // see chapter 8.3 for details and rationale
        throw new RuntimeError(name, String.format("Undefined variable %s.", name.lexeme));
    }

    /**
     * Assign a value to a name in the environment iff it already exists
     *
     * @param name  the name to assign the value to
     * @param value the value
     */
    void assign(Token name, Object value) throws RuntimeError {
        // assignment differs from definition as assignment is not allowed to create a new variable
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        throw new RuntimeError(name, String.format("Undefined variable %s.", name.lexeme));
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

}
