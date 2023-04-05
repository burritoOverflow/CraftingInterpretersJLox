package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Resolve all the variables contained in the program. Executed after the syntax tree is produced by the parser,
 * and before the interpreter starts executing statements.
 */
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    // store the name of the variable and if it's been resolved
    private final Stack<Map<String, Boolean>> scopes;
    private FunctionType currentFunctionType = FunctionType.NONE;
    private ClassType currentClass = ClassType.NONE;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
        this.scopes = new Stack<>();
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt statement) {
        statement.accept(this);
    }

    private void resolve(Expr expression) {
        expression.accept(this);
    }

    private void beginScope() {
        this.scopes.push(new HashMap<>());
    }

    private void endScope() {
        this.scopes.pop();
    }

    /**
     * Set the token `name` as defined
     * Should be called once the initializer expression in the same scope has been resolved
     * post-condition - variable is fully-initialized and ready to be used.
     *
     * @param name the Token to define
     */
    private void define(Token name) {
        if (this.scopes.isEmpty()) {
            return;
        }
        this.scopes.peek().put(name.lexeme, true);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = this.scopes.size() - 1; i >= 0; i--) {
            if (this.scopes.get(i).containsKey(name.lexeme)) {
                // number of scopes between the current innermost scope and the scope where variable was found
                final int scopeDistance = this.scopes.size() - 1 - i;
                this.interpreter.resolve(expr, scopeDistance);
                return;
            }
        }
        // otherwise assume it's in global scope
    }

    private void resolveFunction(Stmt.Function function, FunctionType functionType) {
        FunctionType enclosingFunctionType = this.currentFunctionType;
        this.currentFunctionType = functionType;

        this.beginScope();
        for (final Token parameter : function.params) {
            this.declare(parameter);
            this.define(parameter);
        }
        this.resolve(function.body);
        this.endScope();
        // restore when done resolving the function
        this.currentFunctionType = enclosingFunctionType;
    }

    /**
     * Declaring a variable adds the variable to the innermost scope
     *
     * @param name the Token to declare
     */
    private void declare(Token name) {
        if (this.scopes.isEmpty()) {
            return;
        }

        final Map<String, Boolean> scope = this.scopes.peek();

        // check for variable collisions
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already a variable wth this name in the current scope.");
        }

        // "not yet ready"--no value assigned currently, but we're aware of its existence
        scope.put(name.lexeme, false);
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        // resolve the expression for the assigned value
        this.resolve(expr.value);
        // resolve variable that's being assigned to
        this.resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        this.resolve(expr.left);
        this.resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        // resolve the "thing being called" (most often a variable)
        resolve(expr.callee);
        for (final Expr argument : expr.arguments) {
            this.resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        this.resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        this.resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        // no expressions, so nothing to resolve
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        this.resolve(expr.left);
        this.resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
            return null;
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        // if the variable exists in the current scope but is not yet defined.
        // i.e variable being accessed in its own initializer.
        if (!this.scopes.isEmpty() && this.scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name, "Cannot read variable in its own initializer.");
        }

        this.resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        this.resolve(expr.right);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        this.beginScope();
        this.resolve(stmt.statements);
        this.endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        final ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        // define `this` and the class's methods in a new scope
        beginScope();

        // see pg. 210
        // whenever `this` expression is encountered it will resolve to a
        // local variable in an implicit scope outside of the block for the method body
        scopes.peek().put("this", true);

        for (Stmt.Function method : stmt.methods) {
            final FunctionType declaration = method.name.lexeme.equals("init") ?
                    FunctionType.INITIALIZER : FunctionType.METHOD;
            resolveFunction(method, declaration);
        }

        // restore the original once the methods have been resolved
        currentClass = enclosingClass;
        endScope();
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        this.resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        this.declare(stmt.name);
        this.define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        this.resolve(stmt.condition);
        this.resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) {
            this.resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        this.resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        // prevent "floating returns" outside of function scope
        if (currentFunctionType == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Cannot return from a top-level.");
        }

        if (stmt.value != null) {
            // initializers implicitly return `this`, any explicit return with a _value_ is an error
            if (currentFunctionType == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Cannot return from an initializer.");
            }

            this.resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        this.resolve(stmt.condition);
        this.resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    private enum FunctionType {
        NONE,
        METHOD,
        INITIALIZER,
        FUNCTION
    }

    private enum ClassType {
        NONE,
        CLASS
    }

}
