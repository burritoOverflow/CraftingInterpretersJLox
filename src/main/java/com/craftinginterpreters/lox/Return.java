package com.craftinginterpreters.lox;

public class Return extends RuntimeException {
    final Object value;

    public Return(Object value) {
        // disables unneeded functionality for the RuntimeException base class.
        // (as we're not actually handling an exception)
        super(null, null, false, false);
        this.value = value;
    }

}
