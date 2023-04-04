package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    // a fixed reference to the outermost global environment
    final Environment globals = new Environment();
    // access to each variable's resolved location
    private final Map<Expr, Integer> locals = new HashMap<>();
    private Environment environment = globals;


    public Interpreter() {
        this.globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    /**
     * Public interface for the Interpreter
     *
     * @param statements the list of statements to interpret
     */
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError runtimeError) {
            Lox.runtimeError(runtimeError);
        }
    }

    /**
     * Execute a single statement.
     *
     * @param statement the statement to execute
     */
    private void execute(Stmt statement) {
        statement.accept(this);
    }

    /**
     * Add this expression to the local variables where K = expression
     * and V = depth.
     *
     * @param expr  the expression to resolve
     * @param depth the number of environments between the current env
     *              and the enclosing one where the
     *              variable's value resides
     */
    public void resolve(Expr expr, int depth) {
        this.locals.put(expr, depth);
    }

    /**
     * Get the variable's value
     *
     * @param name the name to retrieve the value for
     * @param expr expression to find the distance for
     * @return
     */
    private Object lookupVariable(Token name, Expr expr) {
        final Integer distance = locals.get(expr);
        if (distance != null) {
            // get the variable's value from the environment at `distance`
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    /**
     * Execute the block of statements in the provided environment.
     *
     * @param statements  the statements to execute
     * @param environment the block's environment
     */
    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            // restore the environment at the site of the invocation
            this.environment = previous;
        }
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        // Display integer values without trailing decimal (Lox only supports double type)
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    /**
     * Determine the `truth` value of an object
     *
     * @param object - the object to make the determination for
     * @return true if the object is present; evaluate if Boolean
     */
    private boolean isTruthy(Object object) {
        // only `true` where the object is not explicitly `nil` or false.
        if (object == null) {
            return false;
        }

        if (object instanceof Boolean) {
            return (boolean) object;
        }

        return true;
    }

    /**
     * Equality check for two Objects.
     *
     * @param a first object
     * @param b second object
     * @return true if both equal or both null; false otherwise.
     */
    private boolean isEqual(Object a, Object b) {
        // defensively to avoid NullPointers
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        final Object value = evaluate(expr.value);
        Integer distance = locals.get(expr);

        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            // assume it's global
            globals.assign(expr.name, value);
        }

        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) throws RuntimeError {
        // get the value of both literals
        final Object left = evaluate(expr.left);
        final Object right = evaluate(expr.right);

        switch (expr.operator.tokenType) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;

            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;

            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;

            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;

            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;

            case PLUS:
                // handle `Number` addition
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                // handle String concatenation
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                // no cases met; error.
                throw new RuntimeError(expr.operator,
                        "Operands must be either two numbers or two Strings.");

            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                // we've already verified both are of type Double, so just check if right == 0
                checkValidDivisor(expr.operator, right);
                return (double) left / (double) right;

            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;

            case EQUAL_EQUAL:
                return isEqual(left, right);

            case BANG_EQUAL:
                return !isEqual(left, right);
        }

        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        final Object callee = evaluate(expr.callee);

        // bail early if this is not a valid callee
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        final List<Object> arguments = new ArrayList<>();

        // evaluate and collect arguments
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        // evaluate the function and return the result.
        final LoxCallable function = (LoxCallable) callee;

        // argument mismatch is an error
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, String.format("Expected %s arguments, but got %s arguments.",
                    function.arity(), arguments.size()));
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        final Object object = evaluate(expr.object);
        if (object instanceof LoxInstance) {
            return ((LoxInstance) object).get(expr.name);
        }

        throw new RuntimeError(expr.name, "Only instances have properties");
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        final Object left = evaluate(expr.left);

        if (expr.operator.tokenType.equals(TokenType.OR)) {
            // short_circuit left iff left evaluates to Truthy
            if (isTruthy(left)) return left;
        } else {
            // AND
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        final Object object = evaluate(expr.object);
        if (!(object instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        }

        final Object value = evaluate(expr.value);
        ((LoxInstance) object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookupVariable(expr.name, expr);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        final Object right = evaluate(expr.right);

        switch (expr.operator.tokenType) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;

            case BANG:
                // negate the `truth` value of the right operand
                return !isTruthy(right);
        }

        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);

        final Map<String, LoxFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            final LoxFunction function = new LoxFunction(method, environment);
            methods.put(method.name.lexeme, function);
        }

        final LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    /**
     * Convert the function's syntax node (compile-time representation) to runtime representation
     *
     * @param stmt the statement to convert to the runtime representation of a function
     * @return null
     */
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // provide the environment to the newly declared function that exists when the func is declared.
        final LoxFunction function = new LoxFunction(stmt, this.environment);
        // bind the function to the function's identifier
        // and store a reference to it in the current environment
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        final Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }

        // unwind and return from the top of the stack back to `call`
        // (code that began executing the body)
        throw new Return(value);
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        // where `initializer` is the value to assign to the var stmt's identifier
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        // w/o initializer, variable's value is set to nil
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    /**
     * Check that the operand is a valid type; used for runtime error checking.
     * Used for Unary runtime error checking.
     * i.e operator operand -> -12 or !34
     * Throws iff operand is not instanceof Double.
     *
     * @param operator operator applied to the operand
     * @param operand  the operand
     */
    private void checkNumberOperand(Token operator, Object operand) throws RuntimeError {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }


    /**
     * Check that the operands are valid types; used for runtime error checking.
     * i.e. left operator right -> 12 + 13
     *
     * @param operator the arithmetic operator
     * @param left     left operator
     * @param right    right operator
     */
    private void checkNumberOperands(Token operator, Object left, Object right) throws RuntimeError {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    /**
     * Check for attempts to divide by zero; throw if value is "near" zero.
     * Throws iff `divisor` ~= 0.0
     *
     * @param operator the operator for RuntimeError reporting
     * @param divisor  the attempted divisor
     */
    private void checkValidDivisor(Token operator, Object divisor) throws RuntimeError {
        final double epsilon = 0.00001;
        final double d = (double) divisor;

        if (d < epsilon && d > -epsilon) {
            // we'll assume this is close enough to 0
            throw new RuntimeError(operator, "Cannot divide by 0.");
        }
    }

}
