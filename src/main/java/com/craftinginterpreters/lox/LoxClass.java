package com.craftinginterpreters.lox;

import java.util.List;

public class LoxClass implements LoxCallable {
    final String className;

    public LoxClass(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return this.className;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        final LoxInstance instance = new LoxInstance(this);
        return instance;
    }
}
