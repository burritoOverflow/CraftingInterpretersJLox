package com.craftinginterpreters.lox;

public class Interpreter implements Expr.Visitor<Object> {
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
    public Object visitBinaryExpr(Expr.Binary expr) {
        final Object left = evaluate(expr.left);
        final Object right = evaluate(expr.right);

        switch (expr.operator.tokenType) {
            case GREATER:
                return (double) left > (double) right;

            case GREATER_EQUAL:
                return (double) left >= (double) right;

            case LESS:
                return (double) left < (double) right;

            case LESS_EQUAL:
                return (double) left <= (double) right;

            case MINUS:
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
                break;

            case SLASH:
                return (double) left / (double) right;

            case STAR:
                return (double) left * (double) right;

            case EQUAL_EQUAL:
                return isEqual(left, right);

            case BANG_EQUAL:
                return !isEqual(left, right);
        }

        return null;
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
    public Object visitUnaryExpr(Expr.Unary expr) {
        final Object right = evaluate(expr.right);

        switch (expr.operator.tokenType) {
            case MINUS:
                return -(double) right;

            case BANG:
                // negate the `truth` value of the right operand
                return !isTruthy(right);
        }

        return null;
    }
}
