package com.thorn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;
    private final ModuleSystem moduleSystem;
    
    // Return value optimization - avoid exceptions
    Object returnValue = null;
    boolean hasReturned = false;

    Interpreter() {
        this.moduleSystem = new ModuleSystem(this);
        
        // Add built-in print function
        globals.define("print", new JavaFunction("print", 1, (interpreter, arguments) -> {
            System.out.println(stringify(arguments.get(0)));
            return null;
        }), false);

        // Add built-in clock function for benchmarking
        globals.define("clock", new JavaFunction("clock", 0, (interpreter, arguments) -> {
            return (double) System.currentTimeMillis();
        }), false);
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
    
    void executeModule(List<Stmt> statements, ModuleSystem.ModuleEnvironment moduleEnv) {
        Environment previous = this.environment;
        this.environment = moduleEnv;
        
        try {
            for (Stmt statement : statements) {
                execute(statement);
                if (hasReturned) {
                    hasReturned = false;
                    returnValue = null;
                }
            }
        } finally {
            this.environment = previous;
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
        if (function.arity() >= 0 && arguments.size() != function.arity()) {
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
    public Object visitIndexSetExpr(Expr.IndexSet expr) {
        Object object = evaluate(expr.object);
        Object index = evaluate(expr.index);
        Object value = evaluate(expr.value);

        if (object instanceof List) {
            if (!(index instanceof Double)) {
                throw new Thorn.RuntimeError(expr.bracket,
                        "List index must be a number.");
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>)object;
            int i = ((Double)index).intValue();
            if (i < 0 || i >= list.size()) {
                throw new Thorn.RuntimeError(expr.bracket,
                        "List index out of bounds.");
            }
            list.set(i, value);
            return value;
        } else if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>)object;
            map.put(index, value);
            return value;
        }

        throw new Thorn.RuntimeError(expr.bracket,
                "Only lists and dictionaries support index assignment.");
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
                    
                case "includes":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 1; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            Object searchValue = arguments.get(0);
                            for (Object element : list) {
                                if (isEqual(element, searchValue)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                        
                        @Override
                        public String toString() { return "<native array method>"; }
                    };
                    
                case "indexOf":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 1; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            Object searchValue = arguments.get(0);
                            for (int i = 0; i < list.size(); i++) {
                                if (isEqual(list.get(i), searchValue)) {
                                    return (double) i;
                                }
                            }
                            return -1.0;
                        }
                        
                        @Override
                        public String toString() { return "<native array method>"; }
                    };
                    
                case "slice":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return -1; } // Variable arguments
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            int start = 0;
                            int end = list.size();
                            
                            // Handle start parameter
                            if (arguments.size() >= 1 && arguments.get(0) != null) {
                                if (!(arguments.get(0) instanceof Double)) {
                                    throw new Thorn.RuntimeError(null, "Slice start index must be a number");
                                }
                                start = ((Double) arguments.get(0)).intValue();
                                // Handle negative indices
                                if (start < 0) {
                                    start = Math.max(0, list.size() + start);
                                }
                            }
                            
                            // Handle end parameter
                            if (arguments.size() >= 2 && arguments.get(1) != null) {
                                if (!(arguments.get(1) instanceof Double)) {
                                    throw new Thorn.RuntimeError(null, "Slice end index must be a number");
                                }
                                end = ((Double) arguments.get(1)).intValue();
                                // Handle negative indices
                                if (end < 0) {
                                    end = Math.max(0, list.size() + end);
                                }
                            }
                            
                            // Ensure valid range
                            start = Math.max(0, Math.min(start, list.size()));
                            end = Math.max(start, Math.min(end, list.size()));
                            
                            // Create new list with sliced elements
                            return new ArrayList<>(list.subList(start, end));
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
    
    // Type expression visitors - for now, just return type information
    @Override
    public Object visitTypeExpr(Expr.Type expr) {
        return ThornTypeFactory.createType(expr.name.lexeme);
    }
    
    @Override
    public Object visitGenericTypeExpr(Expr.GenericType expr) {
        List<Object> typeArgs = new ArrayList<>();
        for (Expr arg : expr.typeArgs) {
            typeArgs.add(evaluate(arg));
        }
        return ThornTypeFactory.createGenericType(expr.name.lexeme, typeArgs);
    }
    
    @Override
    public Object visitFunctionTypeExpr(Expr.FunctionType expr) {
        List<Object> paramTypes = new ArrayList<>();
        for (Expr paramType : expr.paramTypes) {
            paramTypes.add(evaluate(paramType));
        }
        Object returnType = evaluate(expr.returnType);
        return ThornTypeFactory.createFunctionType(paramTypes, returnType);
    }
    
    @Override
    public Object visitArrayTypeExpr(Expr.ArrayType expr) {
        Object elementType = evaluate(expr.elementType);
        return ThornTypeFactory.createArrayType(elementType);
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
        ThornType returnType = null;
        if (stmt.returnType != null) {
            returnType = (ThornType) evaluate(stmt.returnType);
        }
        
        ThornFunction function = new ThornFunction(stmt.name.lexeme, stmt.params, stmt.body, environment, returnType);
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
        
        // Type check if type annotation is present
        if (stmt.type != null) {
            ThornType variableType = (ThornType) evaluate(stmt.type);
            if (value != null && !variableType.matches(value)) {
                throw new Thorn.RuntimeError(stmt.name, "Type error: cannot assign " + getTypeName(value) + 
                                     " to variable '" + stmt.name.lexeme + "' of type " + variableType.getName());
            }
        }

        environment.define(stmt.name.lexeme, value, stmt.isImmutable);
        return null;
    }
    
    private String getTypeName(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "string";
        if (value instanceof Double) return "number";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof List) return "Array";
        if (value instanceof ThornCallable) return "Function";
        return value.getClass().getSimpleName();
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        // Try to optimize simple numeric while loops like: while (i < limit)
        if (tryOptimizedWhileLoop(stmt)) {
            return null;
        }
        
        // Fall back to general case
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
            if (hasReturned) break;
        }
        return null;
    }
    
    // Optimized while loop for simple numeric conditions
    private boolean tryOptimizedWhileLoop(Stmt.While stmt) {
        // Check if condition is a simple comparison: variable < number
        if (!(stmt.condition instanceof Expr.Binary)) {
            return false;
        }
        
        Expr.Binary condition = (Expr.Binary) stmt.condition;
        if (condition.operator.type != TokenType.LESS) {
            return false;
        }
        
        // Check if left side is a variable and right side is a literal
        if (!(condition.left instanceof Expr.Variable && condition.right instanceof Expr.Literal)) {
            return false;
        }
        
        Expr.Variable varExpr = (Expr.Variable) condition.left;
        Expr.Literal limitExpr = (Expr.Literal) condition.right;
        
        if (!(limitExpr.value instanceof Double)) {
            return false;
        }
        
        double limit = (double) limitExpr.value;
        
        // Execute optimized loop
        while (true) {
            try {
                Object varValue = environment.get(varExpr.name);
                if (!(varValue instanceof Double)) {
                    return false; // Fall back if variable becomes non-numeric
                }
                
                double current = (double) varValue;
                if (current >= limit) {
                    break; // Condition false, exit loop
                }
                
                execute(stmt.body);
                if (hasReturned) break;
                
            } catch (Thorn.RuntimeError e) {
                return false; // Fall back on any error
            }
        }
        
        return true;
    }

    @Override
    public Void visitForStmt(Stmt.For stmt) {
        Object iterable = evaluate(stmt.iterable);
        
        if (!(iterable instanceof List)) {
            throw new Thorn.RuntimeError(stmt.variable,
                    "Can only iterate over lists.");
        }

        List<?> list = (List<?>)iterable;
        
        // Optimized: reuse environment and avoid repeated defines
        String varName = stmt.variable.lexeme;
        Map<String, Object> envValues = environment.getValues();
        boolean varExisted = envValues.containsKey(varName);
        Object previousValue = varExisted ? envValues.get(varName) : null;
        
        try {
            for (Object element : list) {
                // Direct assignment instead of environment.define
                envValues.put(varName, element);
                execute(stmt.body);
                if (hasReturned) break;
            }
        } finally {
            // Restore previous state
            if (varExisted) {
                envValues.put(varName, previousValue);
            } else {
                envValues.remove(varName);
            }
        }
        
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null, false);

        Map<String, ThornFunction> methods = new HashMap<>();
        for (Stmt.Function method : stmt.methods) {
            ThornType returnType = null;
            if (method.returnType != null) {
                returnType = (ThornType) evaluate(method.returnType);
            }
            
            ThornFunction function = new ThornFunction(method.name.lexeme, 
                    method.params, method.body, environment, returnType);
            methods.put(method.name.lexeme, function);
        }

        ThornClass klass = new ThornClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitImportStmt(Stmt.Import stmt) {
        String modulePath = stmt.module.lexeme;
        ModuleSystem.Module module = moduleSystem.loadModule(modulePath);
        
        if (stmt.names == null || stmt.names.isEmpty()) {
            // Import all exports
            for (String name : module.getExportedNames()) {
                Object value = module.getExport(name);
                environment.define(name, value, false);
            }
        } else {
            // Import specific names
            for (Token name : stmt.names) {
                Object value = module.getExport(name.lexeme);
                environment.define(name.lexeme, value, false);
            }
        }
        
        return null;
    }

    @Override
    public Void visitExportStmt(Stmt.Export stmt) {
        // Check if we're in a module environment
        if (environment instanceof ModuleSystem.ModuleEnvironment) {
            ModuleSystem.ModuleEnvironment moduleEnv = (ModuleSystem.ModuleEnvironment) environment;
            
            // Execute the declaration
            stmt.declaration.accept(this);
            
            // Export based on declaration type
            if (stmt.declaration instanceof Stmt.Function) {
                Stmt.Function funcDecl = (Stmt.Function) stmt.declaration;
                Object value = environment.get(funcDecl.name);
                moduleEnv.export(funcDecl.name.lexeme, value);
            } else if (stmt.declaration instanceof Stmt.Var) {
                Stmt.Var varDecl = (Stmt.Var) stmt.declaration;
                Object value = environment.get(varDecl.name);
                moduleEnv.export(varDecl.name.lexeme, value);
            } else if (stmt.declaration instanceof Stmt.Class) {
                Stmt.Class classDecl = (Stmt.Class) stmt.declaration;
                Object value = environment.get(classDecl.name);
                moduleEnv.export(classDecl.name.lexeme, value);
            }
        } else {
            // Not in a module context, just execute the declaration
            stmt.declaration.accept(this);
        }
        
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
    
    // Public method for type evaluation from functions
    public Object evaluateType(Expr expr) {
        return evaluate(expr);
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