package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    // store the environment surrounding the function's declaration
    private final Environment closure;
    private final Stmt.Function declaration;
    private final boolean isInitializer;

    public LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.closure = closure;
        this.declaration = declaration;
        this.isInitializer = isInitializer;
    }

    @Override
    public int arity() {
        return this.declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // use the provided closure environment as the call's environment
        // so this new env's enclosing is the function's closure
        final Environment environment = new Environment(this.closure);

        // add to the environment the identifier for the parameter (key) and the value
        for (int i = 0; i < this.declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        // execute the body of this function in the function-local environment
        try {
            interpreter.executeBlock(this.declaration.body, environment);
        } catch (Return returnValue) {
            // when encountering a return statement, return the value
            // associated with that statement

            // we allow empty returns from initializer; it returns `this`
            if (isInitializer) return this.closure.getAt(0, "this");
            return returnValue.value;
        }

        // initializer returns `this`, even when directly called
        if (isInitializer) return closure.getAt(0, "this");
        return null;
    }

    @Override
    public String toString() {
        return String.format("<fn %s>", declaration.name.lexeme);
    }

    /**
     * Create a new environment nestled in the method's closure (pg. 211)
     * When this method is called, the that becomes the parent of the method's env.
     *
     * @param instance the instance to bind the method to
     * @return
     */
    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(this.closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer);
    }
}
