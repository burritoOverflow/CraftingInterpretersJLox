package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    // for this environment's enclosing scope
    final Environment enclosing;

    // stores the bindings that associate variables to values, i.e `var a = 12`
    // where `a` is the name and `12` is the value
    private final Map<String, Object> values;

    public Environment() {
        values = new HashMap<>();
        enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.values = new HashMap<>();
        this.enclosing = enclosing;
    }

    /**
     * Recursively attempt to retrieve a value from this environment (and the enclosing environments)
     *
     * @param name the name to locate
     * @return the value associated with the name
     * @throws RuntimeError when the variable is not found in this environment (undefined variable)
     */
    Object get(Token name) throws RuntimeError {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // recursively attempt to locate the name in the enclosing environment
        if (enclosing != null) {
            return enclosing.get(name);
        }

        // see chapter 8.3 for details and rationale
        throw new RuntimeError(name, String.format("Undefined variable %s.", name.lexeme));
    }

    /**
     * Recursively assign a value to a name in the environment iff it already exists
     *
     * @param name  the name to assign the value to
     * @param value the value to assign to the name
     */
    void assign(Token name, Object value) throws RuntimeError {
        // assignment differs from definition as assignment is not allowed to create a new variable
        if (values.containsKey(name.lexeme)) {
            // if the current environment contains the name, assign
            values.put(name.lexeme, value);
            return;
        }

        // recursively attempt to assign the value to the name
        // (traverse enclosing scopes until the name is found)
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        // attempt made to assign to a name that was not previously defined
        throw new RuntimeError(name, String.format("Undefined variable %s.", name.lexeme));
    }

    /**
     * Traverse a fixed number of environments and add the value with the name as key for that environment.
     *
     * @param distance the distance from the current env to traverse to the target env
     * @param name     the name associated with the value
     * @param value    the value associated with the name
     */
    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    /**
     * Get the value for `name` located `distance`environments from the current environment
     *
     * @param distance the `distance` where the value is stored
     * @param name     the name to get the value for
     * @return the value associated with name located environment `distance` from the current
     */
    Object getAt(Integer distance, String name) {
        return ancestor(distance).values.get(name);
    }

    /**
     * Get the environment `distance` environments from the current environment by
     * traversing the parent chain and returning that environment
     *
     * @param distance the distance to traverse up the environment chain
     * @return the environment distance from the current
     */
    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            // walk up enclosing environments
            environment = environment.enclosing;
        }
        return environment;
    }

}
