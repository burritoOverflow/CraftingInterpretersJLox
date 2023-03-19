package com.craftinginterpreters.lox;

/*
    Utility Class (debugging) for pretty-printing the generated AST
 */
public class AstPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        // TODO implement
        return null;
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return null;
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) {
            return "nil";
        }
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        // TODO implement
        return null;
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        // TODO implement
        return null;
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("(").append(name);

        for (Expr expr : exprs) {
            stringBuilder.append(" ");
            stringBuilder.append(expr.accept(this));
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
