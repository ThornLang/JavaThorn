package com.thorn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;
    
    // Return value optimization - avoid exceptions
    Object returnValue = null;
    boolean hasReturned = false;

    Interpreter() {
        // Add built-in print function
        globals.define("print", new ThornCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                System.out.println(stringify(arguments.get(0)));
                return null;
            }

            @Override
            public String toString() { return "<native fn>"; }
        }, false);

        // Add built-in clock function for benchmarking
        globals.define("clock", new ThornCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis();
            }

            @Override
            public String toString() { return "<native fn>"; }
        }, false);
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (Thorn.RuntimeError error) {
            Thorn.runtimeError(error);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
                if (left instanceof Double && right instanceof Double) {
                    return getNumber(left) - getNumber(right);
                }
                checkNumberOperands(expr.operator, left, right);
                return getNumber(left) - getNumber(right);
            case SLASH:
                if (left instanceof Double && right instanceof Double) {
                    double rightVal = getNumber(right);
                    if (rightVal == 0) {
                        throw new Thorn.RuntimeError(expr.operator, "Division by zero.");
                    }
                    return getNumber(left) / rightVal;
                }
                checkNumberOperands(expr.operator, left, right);
                double rightNum = getNumber(right);
                if (rightNum == 0) {
                    throw new Thorn.RuntimeError(expr.operator, "Division by zero.");
                }
                return getNumber(left) / rightNum;
            case STAR:
                if (left instanceof Double && right instanceof Double) {
                    return getNumber(left) * getNumber(right);
                }
                checkNumberOperands(expr.operator, left, right);
                return getNumber(left) * getNumber(right);
            case PERCENT:
                if (left instanceof Double && right instanceof Double) {
                    return getNumber(left) % getNumber(right);
                }
                checkNumberOperands(expr.operator, left, right);
                return getNumber(left) % getNumber(right);
            case STAR_STAR:
                if (left instanceof Double && right instanceof Double) {
                    return Math.pow(getNumber(left), getNumber(right));
                }
                checkNumberOperands(expr.operator, left, right);
                return Math.pow(getNumber(left), getNumber(right));
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return getNumber(left) + getNumber(right);
                }
                if (left instanceof String && right instanceof String) {
                    // Fast path for string concatenation
                    return (String)left + (String)right;
                }
                if (left instanceof String || right instanceof String) {
                    // Optimized string building
                    StringBuilder sb = new StringBuilder();
                    sb.append(stringify(left));
                    sb.append(stringify(right));
                    return sb.toString();
                }
                if (left instanceof List && right instanceof List) {
                    List<Object> result = new ArrayList<>((List<?>)left);
                    result.addAll((List<?>)right);
                    return result;
                }
                throw new Thorn.RuntimeError(expr.operator,
                        "Operands must be two numbers, two strings, or two lists.");
            case GREATER:
                if (left instanceof Double && right instanceof Double) {
                    return getNumber(left) > getNumber(right);
                }
                checkNumberOperands(expr.operator, left, right);
                return getNumber(left) > getNumber(right);
            case GREATER_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return getNumber(left) >= getNumber(right);
                }
                checkNumberOperands(expr.operator, left, right);
                return getNumber(left) >= getNumber(right);
            case LESS:
                if (left instanceof Double && right instanceof Double) {
                    return getNumber(left) < getNumber(right);
                }
                checkNumberOperands(expr.operator, left, right);
                return getNumber(left) < getNumber(right);
            case LESS_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return getNumber(left) <= getNumber(right);
                }
                checkNumberOperands(expr.operator, left, right);
                return getNumber(left) <= getNumber(right);
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case QUESTION_QUESTION:
                return left != null ? left : right;
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
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                if (right instanceof Double) {
                    return -getNumber(right);
                }
                checkNumberOperand(expr.operator, right);
                return -getNumber(right);
        }

        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        
        // In Thorn, assignment to undefined variable is declaration
        try {
            environment.assign(expr.name, value);
        } catch (Thorn.RuntimeError error) {
            if (error.getMessage().contains("Undefined variable")) {
                environment.define(expr.name.lexeme, value, false);
            } else {
                throw error;
            }
        }
        
        return value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR_OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof ThornCallable)) {
            throw new Thorn.RuntimeError(expr.paren,
                    "Can only call functions and classes.");
        }

        ThornCallable function = (ThornCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new Thorn.RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitLambdaExpr(Expr.Lambda expr) {
        return new ThornFunction(null, expr.params, expr.body, environment);
    }

    @Override
    public Object visitListExpr(Expr.ListExpr expr) {
        List<Object> elements = new ArrayList<>();
        for (Expr element : expr.elements) {
            elements.add(evaluate(element));
        }
        return elements;
    }

    @Override
    public Object visitDictExpr(Expr.Dict expr) {
        Map<Object, Object> dict = new HashMap<>();
        for (int i = 0; i < expr.keys.size(); i++) {
            Object key = evaluate(expr.keys.get(i));
            Object value = evaluate(expr.values.get(i));
            dict.put(key, value);
        }
        return dict;
    }

    @Override
    public Object visitIndexExpr(Expr.Index expr) {
        Object object = evaluate(expr.object);
        Object index = evaluate(expr.index);

        if (object instanceof List) {
            if (!(index instanceof Double)) {
                throw new Thorn.RuntimeError(expr.bracket,
                        "List index must be a number.");
            }
            List<?> list = (List<?>)object;
            int i = ((Double)index).intValue();
            if (i < 0 || i >= list.size()) {
                throw new Thorn.RuntimeError(expr.bracket,
                        "List index out of bounds.");
            }
            return list.get(i);
        } else if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>)object;
            return map.get(index);
        }

        throw new Thorn.RuntimeError(expr.bracket,
                "Only lists and dictionaries support indexing.");
    }

    @Override
    public Object visitMatchExpr(Expr.Match expr) {
        Object value = evaluate(expr.expr);

        for (Expr.Match.Case matchCase : expr.cases) {
            boolean matches = false;
            
            if (matchCase.pattern instanceof Expr.Literal && 
                ((Expr.Literal)matchCase.pattern).value == null) {
                // Default case (underscore)
                matches = true;
            } else {
                Object pattern = evaluate(matchCase.pattern);
                matches = isEqual(value, pattern);
            }

            if (matches) {
                if (matchCase.guard != null) {
                    Object guardResult = evaluate(matchCase.guard);
                    if (!isTruthy(guardResult)) continue;
                }
                return evaluate(matchCase.value);
            }
        }

        throw new Thorn.RuntimeError(null, "No matching case in match expression.");
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = evaluate(expr.object);
        if (object instanceof ThornInstance) {
            return ((ThornInstance) object).get(expr.name);
        }

        // Add built-in properties for native types
        if (object instanceof String && expr.name.lexeme.equals("length")) {
            return (double) ((String) object).length();
        }

        if (object instanceof List) {
            List<Object> list = (List<Object>) object;
            
            switch (expr.name.lexeme) {
                case "length":
                    return (double) list.size();
                    
                case "push":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 1; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            list.add(arguments.get(0));
                            return (double) list.size();
                        }
                        
                        @Override
                        public String toString() { return "<native array method>"; }
                    };
                    
                case "pop":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 0; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            if (list.isEmpty()) {
                                return null;
                            }
                            return list.remove(list.size() - 1);
                        }
                        
                        @Override
                        public String toString() { return "<native array method>"; }
                    };
                    
                case "shift":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 0; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            if (list.isEmpty()) {
                                return null;
                            }
                            return list.remove(0);
                        }
                        
                        @Override
                        public String toString() { return "<native array method>"; }
                    };
                    
                case "unshift":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 1; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            list.add(0, arguments.get(0));
                            return (double) list.size();
                        }
                        
                        @Override
                        public String toString() { return "<native array method>"; }
                    };
            }
        }

        throw new Thorn.RuntimeError(expr.name,
                "Only instances have properties.");
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.object);

        if (!(object instanceof ThornInstance)) {
            throw new Thorn.RuntimeError(expr.name,
                    "Only instances have fields.");
        }

        Object value = evaluate(expr.value);
        ((ThornInstance)object).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        ThornFunction function = new ThornFunction(stmt.name.lexeme, stmt.params, stmt.body, environment);
        environment.define(stmt.name.lexeme, function, false);
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
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        returnValue = value;
        hasReturned = true;
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value, stmt.isImmutable);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitForStmt(Stmt.For stmt) {
        Object iterable = evaluate(stmt.iterable);
        
        if (!(iterable instanceof List)) {
            throw new Thorn.RuntimeError(stmt.variable,
                    "Can only iterate over lists.");
        }

        List<?> list = (List<?>)iterable;
        Environment previous = environment;
        try {
            environment = new Environment(environment);
            
            for (Object element : list) {
                environment.define(stmt.variable.lexeme, element, false);
                execute(stmt.body);
            }
        } finally {
            environment = previous;
        }
        
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null, false);

        Map<String, ThornFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            ThornFunction function = new ThornFunction(method.name.lexeme, 
                    method.params, method.body, environment);
            methods.put(method.name.lexeme, function);
        }

        ThornClass klass = new ThornClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitImportStmt(Stmt.Import stmt) {
        // TODO: Implement module system
        return null;
    }

    @Override
    public Void visitExportStmt(Stmt.Export stmt) {
        // TODO: Implement module system
        return null;
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
                if (hasReturned) {
                    break;  // Early exit on return
                }
            }
        } finally {
            this.environment = previous;
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new Thorn.RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new Thorn.RuntimeError(operator, "Operands must be numbers.");
    }
    
    // Fast path for arithmetic operations
    private double getNumber(Object obj) {
        return (double) obj;
    }

    private String stringify(Object object) {
        if (object == null) return "null";

        if (object instanceof Double) {
            double value = (double) object;
            // Fast path for integers
            if (value == (long) value) {
                return Long.toString((long) value);
            }
            return Double.toString(value);
        }

        if (object instanceof String) {
            return (String) object;
        }

        return object.toString();
    }

    private Object lookUpVariable(Token name, Expr expr) {
        // For now, just look in the current environment
        return environment.get(name);
    }

    static class Return extends RuntimeException {
        final Object value;

        Return(Object value) {
            super(null, null, false, false);
            this.value = value;
        }
    }
}