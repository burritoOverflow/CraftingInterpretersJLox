package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private final Map<String, Object> fields;
    private final LoxClass klass;

    LoxInstance(LoxClass klass) {
        this.klass = klass;
        this.fields = new HashMap<>();
    }

    Object get(Token name) {
        if (this.fields.containsKey(name.lexeme)) return this.fields.get(name.lexeme);
        throw new RuntimeError(name, String.format("Undefined property %s.", name.lexeme));
    }

    @Override
    public String toString() {
        return this.klass.className + " instance";
    }

    public void set(Token name, Object value) {
        this.fields.put(name.lexeme, value);
    }
}
