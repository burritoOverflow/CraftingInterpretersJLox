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

    /**
     * Retrieve a method or field from this Instance for the `name` provided
     *
     * @param name the name to retrieve
     * @return the Object corresponding to the field or method, if found.
     */
    Object get(Token name) throws RuntimeError {
        if (this.fields.containsKey(name.lexeme)) {
            return this.fields.get(name.lexeme);
        }

        final LoxFunction method = this.klass.findMethod(name.lexeme);
        if (method != null) {
            // at runtime create the environment after finding the method on the instance
            return method.bind(this);
        }

        throw new RuntimeError(name, String.format("Undefined property %s.", name.lexeme));
    }

    @Override
    public String toString() {
        final String instanceStr = String.format("%s instance", this.klass.className);
        StringBuilder stringBuilder = new StringBuilder(instanceStr);

        if (!this.fields.isEmpty()) {
            for (Map.Entry<String, Object> field : this.fields.entrySet()) {
                final String fieldStr = String.format("\n%s.%s -> %s", this.klass.className,
                        field.getKey(), field.getValue());
                stringBuilder.append(fieldStr);
            }
        }

        return stringBuilder.toString();
    }

    public void set(Token name, Object value) {
        this.fields.put(name.lexeme, value);
    }
}
