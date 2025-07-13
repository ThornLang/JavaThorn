package com.thorn;

import java.util.List;

public abstract class Stmt {
    interface Visitor<R> {
        R visitBlockStmt(Block stmt);
        R visitExpressionStmt(Expression stmt);
        R visitFunctionStmt(Function stmt);
        R visitIfStmt(If stmt);
        R visitReturnStmt(Return stmt);
        R visitThrowStmt(Throw stmt);
        R visitVarStmt(Var stmt);
        R visitWhileStmt(While stmt);
        R visitForStmt(For stmt);
        R visitClassStmt(Class stmt);
        R visitImportStmt(Import stmt);
        R visitExportStmt(Export stmt);
        R visitExportIdentifierStmt(ExportIdentifier stmt);
        R visitTryCatchStmt(TryCatch stmt);
        R visitTypeAliasStmt(TypeAlias stmt);
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
        Function(Token name, List<Parameter> params, Expr returnType, List<Stmt> body) {
            this(name, null, params, returnType, body);
        }
        
        Function(Token name, List<TypeParameter> typeParams, List<Parameter> params, Expr returnType, List<Stmt> body) {
            this.name = name;
            this.typeParams = typeParams;
            this.params = params;
            this.returnType = returnType;
            this.body = body;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }

        public final Token name;
        public final List<TypeParameter> typeParams;  // null if no type parameters
        public final List<Parameter> params;
        public final Expr returnType;  // null if no return type annotation
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

    public static class Throw extends Stmt {
        Throw(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitThrowStmt(this);
        }

        public final Token keyword;
        public final Expr value;
    }

    public static class Var extends Stmt {
        Var(Token name, Expr type, Expr initializer, boolean isImmutable) {
            this.name = name;
            this.type = type;
            this.initializer = initializer;
            this.isImmutable = isImmutable;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVarStmt(this);
        }

        public final Token name;
        public final Expr type;  // null if no type annotation
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
            this(name, null, methods);
        }
        
        Class(Token name, List<TypeParameter> typeParams, List<Stmt.Function> methods) {
            this.name = name;
            this.typeParams = typeParams;
            this.methods = methods;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitClassStmt(this);
        }

        public final Token name;
        public final List<TypeParameter> typeParams;  // null if no type parameters
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
    
    public static class ExportIdentifier extends Stmt {
        ExportIdentifier(Token name) {
            this.name = name;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExportIdentifierStmt(this);
        }

        public final Token name;
    }

    public static class TryCatch extends Stmt {
        TryCatch(Stmt tryBlock, Token catchVariable, Stmt catchBlock) {
            this.tryBlock = tryBlock;
            this.catchVariable = catchVariable;
            this.catchBlock = catchBlock;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitTryCatchStmt(this);
        }

        public final Stmt tryBlock;
        public final Token catchVariable;  // can be null for catch without variable
        public final Stmt catchBlock;
    }

    public static class Throw extends Stmt {
        Throw(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitThrowStmt(this);
        }

        public final Token keyword;
        public final Expr value;
    }
    
    // Parameter class for typed function parameters
    public static class Parameter {
        Parameter(Token name, Expr type) {
            this.name = name;
            this.type = type;
        }
        
        public final Token name;
        public final Expr type;  // null if no type annotation
    }
    
    // Type alias statement: % TypeName = Type;
    public static class TypeAlias extends Stmt {
        TypeAlias(Token name, Expr type) {
            this.name = name;
            this.type = type;
        }

        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitTypeAliasStmt(this);
        }

        public final Token name;
        public final Expr type;
    }
    
    // Type parameter class for generic types
    public static class TypeParameter {
        TypeParameter(Token name, Expr constraint) {
            this.name = name;
            this.constraint = constraint;
        }
        
        public final Token name;
        public final Expr constraint;  // null if no constraint
    }
}