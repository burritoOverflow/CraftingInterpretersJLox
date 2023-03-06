package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    // for this environment's enclosing scope
    final Environment enclosing;

    // stores the bindings that associate variables to values
    private final Map<String, Object> values;

    public Environment() {
        values = new HashMap<>();
        enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.values = new HashMap<>();
        this.enclosing = enclosing;
    }

    Object get(Token name) throws RuntimeError {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // recursively attempt to locate the name in the enclosing environment
        if (enclosing != null) {
            return enclosing.get(name);
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

        // recursively attempt to assign the value to the name
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, String.format("Undefined variable %s.", name.lexeme));
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

}
