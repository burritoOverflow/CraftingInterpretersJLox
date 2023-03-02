package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    // stores the bindings that associate variables to values
    private final Map<String, Object> values;

    public Environment() {
        values = new HashMap<>();
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // see chapter 8.3 for details and rationale
        throw new RuntimeError(name, String.format("Undefined variable %s.", name.lexeme));
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

}
