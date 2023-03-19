package com.craftinginterpreters.lox;

import java.util.List;

public interface LoxCallable {
    // the number of arguments the function expects
    int arity();

    // evaluate the function and return the result
    Object call(Interpreter interpreter, List<Object> arguments);
}
