package com.thorn;

import java.util.List;

public abstract class Stmt {
    interface Visitor<R> {
        R visitBlockStmt(Block stmt);
        R visitExpressionStmt(Expression stmt);
        R visitFunctionStmt(Function stmt);
        R visitIfStmt(If stmt);
        R visitReturnStmt(Return stmt);
        R visitVarStmt(Var stmt);
        R visitWhileStmt(While stmt);
        R visitForStmt(For stmt);
        R visitClassStmt(Class stmt);
        R visitImportStmt(Import stmt);
        R visitExportStmt(Export stmt);
    }

    abstract <R> R accept(Visitor<R> visitor);

    public static class Block extends Stmt {
        Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }

        public final List<Stmt> statements;
    }

    public static class Expression extends Stmt {
        Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }

        public final Expr expression;
    }

    public static class Function extends Stmt {
        Function(Token name, List<Token> params, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }

        public final Token name;
        public final List<Token> params;
        public final List<Stmt> body;
    }

    public static class If extends Stmt {
        If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }

        public final Expr condition;
        public final Stmt thenBranch;
        public final Stmt elseBranch;
    }

    public static class Return extends Stmt {
        Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitReturnStmt(this);
        }

        public final Token keyword;
        public final Expr value;
    }

    public static class Var extends Stmt {
        Var(Token name, Expr initializer, boolean isImmutable) {
            this.name = name;
            this.initializer = initializer;
            this.isImmutable = isImmutable;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }

        public final Token name;
        public final Expr initializer;
        public final boolean isImmutable;
    }

    public static class While extends Stmt {
        While(Expr condition, Stmt body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitWhileStmt(this);
        }

        public final Expr condition;
        public final Stmt body;
    }

    public static class For extends Stmt {
        For(Token variable, Expr iterable, Stmt body) {
            this.variable = variable;
            this.iterable = iterable;
            this.body = body;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitForStmt(this);
        }

        public final Token variable;
        public final Expr iterable;
        public final Stmt body;
    }

    public static class Class extends Stmt {
        Class(Token name, List<Stmt.Function> methods) {
            this.name = name;
            this.methods = methods;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitClassStmt(this);
        }

        public final Token name;
        public final List<Stmt.Function> methods;
    }

    public static class Import extends Stmt {
        Import(Token module, List<Token> names) {
            this.module = module;
            this.names = names;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitImportStmt(this);
        }

        public final Token module;
        public final List<Token> names;  // null for "import module", list for "import { a, b } from module"
    }

    public static class Export extends Stmt {
        Export(Stmt declaration) {
            this.declaration = declaration;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExportStmt(this);
        }

        public final Stmt declaration;
    }
}