package com.thorn;

import java.util.List;

public abstract class Expr {
    interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal expr);
        R visitUnaryExpr(Unary expr);
        R visitVariableExpr(Variable expr);
        R visitAssignExpr(Assign expr);
        R visitLogicalExpr(Logical expr);
        R visitCallExpr(Call expr);
        R visitLambdaExpr(Lambda expr);
        R visitListExpr(ListExpr expr);
        R visitDictExpr(Dict expr);
        R visitIndexExpr(Index expr);
        R visitIndexSetExpr(IndexSet expr);
        R visitMatchExpr(Match expr);
        R visitGetExpr(Get expr);
        R visitSetExpr(Set expr);
        R visitThisExpr(This expr);
    }

    abstract <R> R accept(Visitor<R> visitor);

    public static class Binary extends Expr {
        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }

        public final Expr left;
        public final Token operator;
        public final Expr right;
    }

    public static class Grouping extends Expr {
        Grouping(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }

        public final Expr expression;
    }

    public static class Literal extends Expr {
        Literal(Object value) {
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }

        public final Object value;
    }

    public static class Unary extends Expr {
        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }

        public final Token operator;
        public final Expr right;
    }

    public static class Variable extends Expr {
        Variable(Token name) {
            this.name = name;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }

        public final Token name;
    }

    public static class Assign extends Expr {
        Assign(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitAssignExpr(this);
        }

        public final Token name;
        public final Expr value;
    }

    public static class Logical extends Expr {
        Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }

        public final Expr left;
        public final Token operator;
        public final Expr right;
    }

    public static class Call extends Expr {
        Call(Expr callee, Token paren, List<Expr> arguments) {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }

        public final Expr callee;
        public final Token paren;
        public final List<Expr> arguments;
    }

    public static class Lambda extends Expr {
        Lambda(List<Token> params, List<Stmt> body) {
            this.params = params;
            this.body = body;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLambdaExpr(this);
        }

        public final List<Token> params;
        public final List<Stmt> body;
    }

    public static class ListExpr extends Expr {
        ListExpr(List<Expr> elements) {
            this.elements = elements;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitListExpr(this);
        }

        public final List<Expr> elements;
    }

    public static class Dict extends Expr {
        Dict(List<Expr> keys, List<Expr> values) {
            this.keys = keys;
            this.values = values;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitDictExpr(this);
        }

        public final List<Expr> keys;
        public final List<Expr> values;
    }

    public static class Index extends Expr {
        Index(Expr object, Token bracket, Expr index) {
            this.object = object;
            this.bracket = bracket;
            this.index = index;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitIndexExpr(this);
        }

        public final Expr object;
        public final Token bracket;
        public final Expr index;
    }

    public static class IndexSet extends Expr {
        IndexSet(Expr object, Token bracket, Expr index, Expr value) {
            this.object = object;
            this.bracket = bracket;
            this.index = index;
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitIndexSetExpr(this);
        }

        public final Expr object;
        public final Token bracket;
        public final Expr index;
        public final Expr value;
    }

    public static class Match extends Expr {
        public static class Case {
            public final Expr pattern;
            public final Expr guard;  // optional: if condition
            public final Expr value;

            Case(Expr pattern, Expr guard, Expr value) {
                this.pattern = pattern;
                this.guard = guard;
                this.value = value;
            }
        }

        Match(Expr expr, List<Case> cases) {
            this.expr = expr;
            this.cases = cases;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitMatchExpr(this);
        }

        public final Expr expr;
        public final List<Case> cases;
    }

    public static class Get extends Expr {
        Get(Expr object, Token name) {
            this.object = object;
            this.name = name;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGetExpr(this);
        }

        public final Expr object;
        public final Token name;
    }

    public static class Set extends Expr {
        Set(Expr object, Token name, Expr value) {
            this.object = object;
            this.name = name;
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitSetExpr(this);
        }

        public final Expr object;
        public final Token name;
        public final Expr value;
    }

    public static class This extends Expr {
        This(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitThisExpr(this);
        }

        public final Token keyword;
    }
}