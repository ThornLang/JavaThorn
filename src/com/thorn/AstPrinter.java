package com.thorn;

import java.util.List;

class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
    String print(List<Stmt> statements) {
        StringBuilder builder = new StringBuilder();
        for (Stmt stmt : statements) {
            builder.append(print(stmt)).append("\n");
        }
        return builder.toString();
    }

    String print(Stmt stmt) {
        return stmt.accept(this);
    }

    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBlockStmt(Stmt.Block stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(block");
        for (Stmt statement : stmt.statements) {
            builder.append(" ").append(print(statement));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitExpressionStmt(Stmt.Expression stmt) {
        return "(expr-stmt " + print(stmt.expression) + ")";
    }

    @Override
    public String visitFunctionStmt(Stmt.Function stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(function ").append(stmt.name.lexeme);
        builder.append(" (params");
        for (Stmt.Parameter param : stmt.params) {
            builder.append(" ").append(param.name.lexeme);
            if (param.type != null) {
                builder.append(": ").append(print(param.type));
            }
        }
        builder.append(") (body");
        for (Stmt bodyStmt : stmt.body) {
            builder.append(" ").append(print(bodyStmt));
        }
        builder.append("))");
        return builder.toString();
    }

    @Override
    public String visitIfStmt(Stmt.If stmt) {
        String result = "(if " + print(stmt.condition) + " " + print(stmt.thenBranch);
        if (stmt.elseBranch != null) {
            result += " " + print(stmt.elseBranch);
        }
        result += ")";
        return result;
    }

    @Override
    public String visitReturnStmt(Stmt.Return stmt) {
        if (stmt.value == null) return "(return)";
        return "(return " + print(stmt.value) + ")";
    }

    @Override
    public String visitThrowStmt(Stmt.Throw stmt) {
        if (stmt.value == null) return "(throw)";
        return "(throw " + print(stmt.value) + ")";
    }

    @Override
    public String visitVarStmt(Stmt.Var stmt) {
        String result = "(var " + stmt.name.lexeme;
        if (stmt.isImmutable) result = "(immut-var " + stmt.name.lexeme;
        if (stmt.initializer != null) {
            result += " = " + print(stmt.initializer);
        }
        result += ")";
        return result;
    }

    @Override
    public String visitWhileStmt(Stmt.While stmt) {
        return "(while " + print(stmt.condition) + " " + print(stmt.body) + ")";
    }

    @Override
    public String visitForStmt(Stmt.For stmt) {
        return "(for " + stmt.variable.lexeme + " in " + print(stmt.iterable) + " " + print(stmt.body) + ")";
    }

    @Override
    public String visitClassStmt(Stmt.Class stmt) {
        StringBuilder builder = new StringBuilder();
        builder.append("(class ").append(stmt.name.lexeme);
        for (Stmt.Function method : stmt.methods) {
            builder.append(" ").append(print(method));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitImportStmt(Stmt.Import stmt) {
        return "(import " + stmt.module.lexeme + ")";
    }

    @Override
    public String visitExportStmt(Stmt.Export stmt) {
        return "(export " + print(stmt.declaration) + ")";
    }

    @Override
    public String visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
        return "(export " + stmt.name.lexeme + ")";
    }

    @Override
    public String visitTryCatchStmt(Stmt.TryCatch stmt) {
        String result = "(try " + print(stmt.tryBlock) + " catch";
        if (stmt.catchVariable != null) {
            result += " (" + stmt.catchVariable.lexeme + ")";
        }
        result += " " + print(stmt.catchBlock) + ")";
        return result;
    }

    @Override
    public String visitThrowStmt(Stmt.Throw stmt) {
        return "(throw " + print(stmt.value) + ")";
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "null";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return "(assign " + expr.name.lexeme + " " + print(expr.value) + ")";
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        StringBuilder builder = new StringBuilder();
        builder.append("(call ").append(print(expr.callee));
        for (Expr arg : expr.arguments) {
            builder.append(" ").append(print(arg));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitLambdaExpr(Expr.Lambda expr) {
        StringBuilder builder = new StringBuilder();
        builder.append("(lambda (params");
        for (Token param : expr.params) {
            builder.append(" ").append(param.lexeme);
        }
        builder.append(") (body");
        for (Stmt stmt : expr.body) {
            builder.append(" ").append(print(stmt));
        }
        builder.append("))");
        return builder.toString();
    }

    @Override
    public String visitListExpr(Expr.ListExpr expr) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < expr.elements.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(print(expr.elements.get(i)));
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public String visitDictExpr(Expr.Dict expr) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (int i = 0; i < expr.keys.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(print(expr.keys.get(i)));
            builder.append(": ");
            builder.append(print(expr.values.get(i)));
        }
        builder.append("}");
        return builder.toString();
    }

    @Override
    public String visitIndexExpr(Expr.Index expr) {
        return "(index " + print(expr.object) + " " + print(expr.index) + ")";
    }

    @Override
    public String visitIndexSetExpr(Expr.IndexSet expr) {
        return "(index-set " + print(expr.object) + " " + print(expr.index) + " " + print(expr.value) + ")";
    }

    @Override
    public String visitSliceExpr(Expr.Slice expr) {
        String start = expr.start != null ? print(expr.start) : "";
        String end = expr.end != null ? print(expr.end) : "";
        return "(slice " + print(expr.object) + " " + start + ":" + end + ")";
    }

    @Override
    public String visitMatchExpr(Expr.Match expr) {
        StringBuilder builder = new StringBuilder();
        builder.append("(match ").append(print(expr.expr));
        for (Expr.Match.Case c : expr.cases) {
            builder.append(" (case ").append(print(c.pattern));
            if (c.guard != null) {
                builder.append(" if ").append(print(c.guard));
            }
            builder.append(" => ").append(print(c.value)).append(")");
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return "(get " + print(expr.object) + " " + expr.name.lexeme + ")";
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return "(set " + print(expr.object) + " " + expr.name.lexeme + " " + print(expr.value) + ")";
    }

    @Override
    public String visitThisExpr(Expr.This expr) {
        return "this";
    }
    
    @Override
    public String visitTypeExpr(Expr.Type expr) {
        return expr.name.lexeme;
    }
    
    @Override
    public String visitGenericTypeExpr(Expr.GenericType expr) {
        StringBuilder builder = new StringBuilder();
        builder.append(expr.name.lexeme).append("[");
        for (int i = 0; i < expr.typeArgs.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(print(expr.typeArgs.get(i)));
        }
        builder.append("]");
        return builder.toString();
    }
    
    @Override
    public String visitFunctionTypeExpr(Expr.FunctionType expr) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i = 0; i < expr.paramTypes.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(print(expr.paramTypes.get(i)));
        }
        builder.append(") -> ");
        builder.append(print(expr.returnType));
        return builder.toString();
    }
    
    @Override
    public String visitArrayTypeExpr(Expr.ArrayType expr) {
        return print(expr.elementType) + "[]";
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(print(expr));
        }
        builder.append(")");
        return builder.toString();
    }
    
    @Override
    public String visitTypeAliasStmt(Stmt.TypeAlias stmt) {
        return "(type-alias " + stmt.name.lexeme + " = " + print(stmt.type) + ")";
    }
}