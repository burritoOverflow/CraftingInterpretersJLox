package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    final String className;

    final Map<String, LoxFunction> methods;

    public LoxClass(String className, Map<String, LoxFunction> methods) {
        this.className = className;
        this.methods = methods;
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
        return new LoxInstance(this);
    }

    /**
     * Return the LoxFunction associated with `methodName` if the method is present in the class,
     * otherwise, return null.
     *
     * @param methodName - the method name to retrieve
     * @return the corresponding LoxFunction | null.
     */
    LoxFunction findMethod(String methodName) {
        if (this.methods.containsKey(methodName)) {
            return this.methods.get(methodName);
        }

        return null;
    }
}
