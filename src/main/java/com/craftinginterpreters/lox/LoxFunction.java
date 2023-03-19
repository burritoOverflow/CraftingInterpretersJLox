package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    // store the environment surrounding the function's declaration
    private final Environment closure;
    private final Stmt.Function declaration;

    public LoxFunction(Stmt.Function declaration, Environment closure) {
        this.closure = closure;
        this.declaration = declaration;
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
            return returnValue.value;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("<fn %s>", declaration.name.lexeme);
    }
}
