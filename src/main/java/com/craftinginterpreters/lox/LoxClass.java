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

    /**
     * The number of arguments the function expects
     *
     * @return the initializer methods' arity; returns null if initializer not present
     */
    @Override
    public int arity() {
        final LoxFunction initializer = findMethod("init");
        return initializer == null ? 0 : initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        final LoxInstance instance = new LoxInstance(this);
        final LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            // bind and invoke the initializer if the method is present
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
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
