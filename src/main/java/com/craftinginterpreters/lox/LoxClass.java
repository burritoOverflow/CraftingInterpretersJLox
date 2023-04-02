package com.craftinginterpreters.lox;

public class LoxClass {
    final String className;

    public LoxClass(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return this.className;
    }
}
