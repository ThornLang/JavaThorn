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
    
    // Track if we're in a Result context for division by zero handling
    private boolean inResultContext = false;
    
    // Track if we're inside a try block for converting runtime errors to catchable exceptions
    private int tryDepth = 0;

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
        
        // Add built-in Ok constructor for Result type
        globals.define("Ok", new JavaFunction("Ok", 1, (interpreter, arguments) -> {
            interpreter.inResultContext = true;
            try {
                return ThornResult.ok(arguments.get(0));
            } finally {
                interpreter.inResultContext = false;
            }
        }), false);
        
        // Add built-in Error constructor for Result type
        globals.define("Error", new JavaFunction("Error", 1, (interpreter, arguments) -> {
            interpreter.inResultContext = true;
            try {
                return ThornResult.error(arguments.get(0));
            } finally {
                interpreter.inResultContext = false;
            }
        }), false);
    }
    
    private boolean isInResultContext() {
        return inResultContext;
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
                        // Check if we're in a Result context (being called from Ok/Error constructor)
                        if (isInResultContext()) {
                            return getNumber(left) / rightVal; // Returns Infinity
                        }
                        if (tryDepth > 0) {
                            throw new ThornThrowException("Division by zero.");
                        }
                        throw new Thorn.RuntimeError(expr.operator, "Division by zero.");
                    }
                    return getNumber(left) / rightVal;
                }
                checkNumberOperands(expr.operator, left, right);
                double rightNum = getNumber(right);
                if (rightNum == 0) {
                    // Check if we're in a Result context (being called from Ok/Error constructor)
                    if (isInResultContext()) {
                        return getNumber(left) / rightNum; // Returns Infinity
                    }
                    if (tryDepth > 0) {
                        throw new ThornThrowException("Division by zero.");
                    }
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

        // Check if this is a call to Ok or Error constructor
        boolean isResultConstructor = false;
        if (expr.callee instanceof Expr.Variable) {
            String name = ((Expr.Variable) expr.callee).name.lexeme;
            if ("Ok".equals(name) || "Error".equals(name)) {
                isResultConstructor = true;
            }
        }

        List<Object> arguments = new ArrayList<>();
        
        // Set Result context flag if calling Ok or Error
        if (isResultConstructor) {
            inResultContext = true;
        }
        
        try {
            for (Expr argument : expr.arguments) {
                arguments.add(evaluate(argument));
            }
        } finally {
            if (isResultConstructor) {
                inResultContext = false;
            }
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
                if (tryDepth > 0) {
                    throw new ThornThrowException("List index out of bounds.");
                }
                throw new Thorn.RuntimeError(expr.bracket,
                        "List index out of bounds.");
            }
            return list.get(i);
        } else if (object instanceof String) {
            if (!(index instanceof Double)) {
                throw new Thorn.RuntimeError(expr.bracket,
                        "String index must be a number.");
            }
            String str = (String) object;
            int i = ((Double)index).intValue();
            if (i < 0 || i >= str.length()) {
                throw new Thorn.RuntimeError(expr.bracket,
                        "String index out of bounds.");
            }
            return String.valueOf(str.charAt(i));
        } else if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>)object;
            return map.get(index);
        }

        throw new Thorn.RuntimeError(expr.bracket,
                "Only lists, strings, and dictionaries support indexing.");
    }

    @Override
    public Object visitSliceExpr(Expr.Slice expr) {
        Object object = evaluate(expr.object);
        
        if (!(object instanceof List)) {
            throw new Thorn.RuntimeError(expr.bracket,
                    "Only lists support slicing.");
        }
        
        List<?> list = (List<?>)object;
        int size = list.size();
        
        // Evaluate start index (default to 0)
        int start = 0;
        if (expr.start != null) {
            Object startObj = evaluate(expr.start);
            if (!(startObj instanceof Double)) {
                throw new Thorn.RuntimeError(expr.bracket,
                        "Slice start index must be a number.");
            }
            start = ((Double)startObj).intValue();
            if (start < 0) start = size + start;  // Handle negative indices
            start = Math.max(0, Math.min(start, size));
        }
        
        // Evaluate end index (default to size)
        int end = size;
        if (expr.end != null) {
            Object endObj = evaluate(expr.end);
            if (!(endObj instanceof Double)) {
                throw new Thorn.RuntimeError(expr.bracket,
                        "Slice end index must be a number.");
            }
            end = ((Double)endObj).intValue();
            if (end < 0) end = size + end;  // Handle negative indices
            end = Math.max(0, Math.min(end, size));
        }
        
        // Create the slice
        if (start > end) start = end;
        return new ArrayList<>(list.subList(start, end));
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
                if (tryDepth > 0) {
                    throw new ThornThrowException("List index out of bounds.");
                }
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
            } else if (matchCase.pattern instanceof Expr.Call) {
                // Constructor pattern like Ok(value) or Error(error)
                Expr.Call callPattern = (Expr.Call) matchCase.pattern;
                if (callPattern.callee instanceof Expr.Variable) {
                    String constructorName = ((Expr.Variable) callPattern.callee).name.lexeme;
                    
                    // Check if this is a Result constructor pattern
                    if (value instanceof ThornResult) {
                        ThornResult result = (ThornResult) value;
                        if (constructorName.equals("Ok") && result.isOk()) {
                            matches = true;
                            // Bind the inner value to the pattern variable if it's a variable
                            if (callPattern.arguments.size() == 1 && 
                                callPattern.arguments.get(0) instanceof Expr.Variable) {
                                Expr.Variable var = (Expr.Variable) callPattern.arguments.get(0);
                                environment.define(var.name.lexeme, result.getValue(), false);
                            }
                        } else if (constructorName.equals("Error") && result.isError()) {
                            matches = true;
                            // Bind the inner error to the pattern variable if it's a variable
                            if (callPattern.arguments.size() == 1 && 
                                callPattern.arguments.get(0) instanceof Expr.Variable) {
                                Expr.Variable var = (Expr.Variable) callPattern.arguments.get(0);
                                environment.define(var.name.lexeme, result.getError(), false);
                            }
                        }
                    }
                }
            } else {
                Object pattern = evaluate(matchCase.pattern);
                matches = isEqual(value, pattern);
            }

            if (matches) {
                if (matchCase.guard != null) {
                    Object guardResult = evaluate(matchCase.guard);
                    if (!isTruthy(guardResult)) continue;
                }
                
                if (matchCase.isBlock) {
                    // Execute block statements in a new environment
                    Environment blockEnv = new Environment(environment);
                    Object result = null;
                    
                    // Save current return state
                    Object previousReturnValue = returnValue;
                    boolean previousHasReturned = hasReturned;
                    returnValue = null;
                    hasReturned = false;
                    
                    try {
                        Environment previous = this.environment;
                        this.environment = blockEnv;
                        try {
                            for (int i = 0; i < matchCase.stmts.size(); i++) {
                                Stmt stmt = matchCase.stmts.get(i);
                                
                                // If this is the last statement and it's an expression statement,
                                // capture its value as the block result
                                if (i == matchCase.stmts.size() - 1 && stmt instanceof Stmt.Expression) {
                                    Stmt.Expression exprStmt = (Stmt.Expression) stmt;
                                    result = evaluate(exprStmt.expression);
                                } else {
                                    execute(stmt);
                                }
                                
                                if (hasReturned) {
                                    result = returnValue;
                                    break;
                                }
                            }
                        } finally {
                            this.environment = previous;
                        }
                    } finally {
                        // Restore return state
                        returnValue = previousReturnValue;
                        hasReturned = previousHasReturned;
                    }
                    
                    return result; // result of last expression or null for void blocks
                } else {
                    // Evaluate single expression
                    return evaluate(matchCase.value);
                }
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
        
        if (object instanceof JavaInstance) {
            return ((JavaInstance) object).get(expr.name);
        }

        // Add built-in properties and methods for native types
        if (object instanceof String) {
            String str = (String) object;
            String methodName = expr.name.lexeme;
            
            switch (methodName) {
                case "length":
                    return (double) str.length();
                    
                case "includes":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 1; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            Object arg = arguments.get(0);
                            if (!(arg instanceof String)) {
                                throw new Thorn.RuntimeError(expr.name, "includes() expects a string argument");
                            }
                            return str.contains((String) arg);
                        }
                        
                        @Override
                        public String toString() { return "<native string method>"; }
                    };
                    
                case "startsWith":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 1; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            Object arg = arguments.get(0);
                            if (!(arg instanceof String)) {
                                throw new Thorn.RuntimeError(expr.name, "startsWith() expects a string argument");
                            }
                            return str.startsWith((String) arg);
                        }
                        
                        @Override
                        public String toString() { return "<native string method>"; }
                    };
                    
                case "endsWith":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 1; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            Object arg = arguments.get(0);
                            if (!(arg instanceof String)) {
                                throw new Thorn.RuntimeError(expr.name, "endsWith() expects a string argument");
                            }
                            return str.endsWith((String) arg);
                        }
                        
                        @Override
                        public String toString() { return "<native string method>"; }
                    };
                    
                case "slice":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return -1; } // Variable arity
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            if (arguments.isEmpty() || arguments.size() > 2) {
                                throw new Thorn.RuntimeError(expr.name, "slice() expects 1 or 2 arguments");
                            }
                            
                            if (!(arguments.get(0) instanceof Double)) {
                                throw new Thorn.RuntimeError(expr.name, "slice() start index must be a number");
                            }
                            
                            int start = ((Double) arguments.get(0)).intValue();
                            int end = str.length();
                            
                            if (arguments.size() == 2) {
                                if (!(arguments.get(1) instanceof Double)) {
                                    throw new Thorn.RuntimeError(expr.name, "slice() end index must be a number");
                                }
                                end = ((Double) arguments.get(1)).intValue();
                            }
                            
                            // Handle negative indices
                            if (start < 0) start = Math.max(0, str.length() + start);
                            if (end < 0) end = Math.max(0, str.length() + end);
                            
                            // Clamp to valid range
                            start = Math.max(0, Math.min(start, str.length()));
                            end = Math.max(start, Math.min(end, str.length()));
                            
                            return str.substring(start, end);
                        }
                        
                        @Override
                        public String toString() { return "<native string method>"; }
                    };
            }
        }

        if (object instanceof List) {
            @SuppressWarnings("unchecked")
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
        
        // Handle dictionary/map methods
        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) object;
            
            switch (expr.name.lexeme) {
                case "keys":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 0; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            return new ArrayList<>(map.keySet());
                        }
                        
                        @Override
                        public String toString() { return "<native dictionary method>"; }
                    };
                    
                case "values":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 0; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            return new ArrayList<>(map.values());
                        }
                        
                        @Override
                        public String toString() { return "<native dictionary method>"; }
                    };
                    
                case "has":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 1; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            return map.containsKey(arguments.get(0));
                        }
                        
                        @Override
                        public String toString() { return "<native dictionary method>"; }
                    };
                    
                case "size":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 0; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            return (double) map.size();
                        }
                        
                        @Override
                        public String toString() { return "<native dictionary method>"; }
                    };
                    
                case "remove":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 1; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            return map.remove(arguments.get(0));
                        }
                        
                        @Override
                        public String toString() { return "<native dictionary method>"; }
                    };
                    
                case "get":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return -1; } // Variable arguments (1 or 2)
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            if (arguments.size() < 1 || arguments.size() > 2) {
                                throw new Thorn.RuntimeError(null, 
                                    "get() takes 1 or 2 arguments (key, optional default).");
                            }
                            Object key = arguments.get(0);
                            Object result = map.get(key);
                            if (result == null && arguments.size() == 2) {
                                return arguments.get(1); // Return default value
                            }
                            return result;
                        }
                        
                        @Override
                        public String toString() { return "<native dictionary method>"; }
                    };
                    
                case "set":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 2; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            Object key = arguments.get(0);
                            Object value = arguments.get(1);
                            map.put(key, value);
                            return map; // Return the map for method chaining
                        }
                        
                        @Override
                        public String toString() { return "<native dictionary method>"; }
                    };
            }
        }
        
        // Handle Result type properties
        if (object instanceof ThornResult) {
            ThornResult result = (ThornResult) object;
            
            switch (expr.name.lexeme) {
                case "is_ok":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 0; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            return result.isOk();
                        }
                        
                        @Override
                        public String toString() { return "<native result method>"; }
                    };
                    
                case "is_error":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 0; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            return result.isError();
                        }
                        
                        @Override
                        public String toString() { return "<native result method>"; }
                    };
                    
                case "unwrap":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 0; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            return result.unwrap();
                        }
                        
                        @Override
                        public String toString() { return "<native result method>"; }
                    };
                    
                case "unwrap_or":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 1; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            return result.unwrapOr(arguments.get(0));
                        }
                        
                        @Override
                        public String toString() { return "<native result method>"; }
                    };
                    
                case "unwrap_error":
                    return new ThornCallable() {
                        @Override
                        public int arity() { return 0; }
                        
                        @Override
                        public Object call(Interpreter interpreter, List<Object> arguments) {
                            return result.unwrapError();
                        }
                        
                        @Override
                        public String toString() { return "<native result method>"; }
                    };
            }
        }

        // Generate type-specific error messages
        String typeName = getTypeName(object);
        String errorMessage;
        
        if (object instanceof List) {
            errorMessage = "Array method '" + expr.name.lexeme + "' is not defined.\n" +
                          "Available array methods: length, push, pop, shift, unshift, includes, indexOf, slice";
        } else if (object instanceof Map) {
            errorMessage = "Dictionary method '" + expr.name.lexeme + "' is not defined.\n" +
                          "Available dictionary methods: keys, values, has, size, remove, get, set";
        } else if (object instanceof String) {
            errorMessage = "String method '" + expr.name.lexeme + "' is not defined.\n" +
                          "Available string methods: length, includes, startsWith, endsWith, slice";
        } else if (object instanceof ThornResult) {
            errorMessage = "Result method '" + expr.name.lexeme + "' is not defined.\n" +
                          "Available result methods: is_ok, is_error, unwrap, unwrap_or, unwrap_error";
        } else if (object instanceof Double || object instanceof Boolean) {
            errorMessage = "Cannot access property '" + expr.name.lexeme + "' on primitive type '" + typeName + "'.";
        } else if (object == null) {
            errorMessage = "Cannot access property '" + expr.name.lexeme + "' on null.";
        } else {
            errorMessage = "Property '" + expr.name.lexeme + "' is not defined on type '" + typeName + "'.";
        }
        
        if (tryDepth > 0) {
            throw new ThornThrowException(errorMessage);
        }
        throw new Thorn.RuntimeError(expr.name, errorMessage);
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
        // First check if this is a type alias
        try {
            Object aliasedType = lookUpVariable(expr.name, expr);
            if (aliasedType instanceof ThornType) {
                return aliasedType;
            }
        } catch (Thorn.RuntimeError e) {
            // Not a type alias, continue with built-in type
        }
        
        return ThornTypeFactory.createType(expr.name.lexeme);
    }
    
    @Override
    public Object visitGenericTypeExpr(Expr.GenericType expr) {
        // First check if this is a type alias for a generic type
        try {
            Object aliasedType = lookUpVariable(expr.name, expr);
            if (aliasedType instanceof ThornType) {
                // If it's already a complete type, return it
                // This handles cases where the type alias includes type parameters
                return aliasedType;
            }
        } catch (Thorn.RuntimeError e) {
            // Not a type alias, continue with built-in type
        }
        
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
        if (value instanceof List) return "array";
        if (value instanceof java.util.Map) return "dict";
        if (value instanceof ThornInstance) {
            return ((ThornInstance) value).getKlass().name;
        }
        if (value instanceof ThornCallable) return "function";
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
                Object value = module.getExport(name.lexeme, name);
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

    @Override
    public Void visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
        // Check if we're in a module environment
        if (environment instanceof ModuleSystem.ModuleEnvironment) {
            ModuleSystem.ModuleEnvironment moduleEnv = (ModuleSystem.ModuleEnvironment) environment;
            
            // Get the existing value from the environment
            Object value = environment.get(stmt.name);
            
            // Export it
            moduleEnv.export(stmt.name.lexeme, value);
        }
        
        return null;
    }
    
    @Override
    public Void visitTypeAliasStmt(Stmt.TypeAlias stmt) {
        // Evaluate the type expression to get the actual type
        Object type = evaluate(stmt.type);
        
        // Store the type alias in the environment as a special type value
        // We'll mark it as immutable since type aliases shouldn't be reassignable
        environment.define(stmt.name.lexeme, type, true);
        
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
        if (tryDepth > 0) {
            throw new ThornThrowException("Operand must be a number.");
        }
        throw new Thorn.RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        if (tryDepth > 0) {
            throw new ThornThrowException("Operands must be numbers.");
        }
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

    // Exception class for throw statements
    static class ThornThrowException extends RuntimeException {
        final Object value;

        ThornThrowException(Object value) {
            super(null, null, false, false);
            this.value = value;
        }
    }

    @Override
    public Void visitTryCatchStmt(Stmt.TryCatch stmt) {
        tryDepth++;
        try {
            execute(stmt.tryBlock);
        } catch (ThornThrowException throwEx) {
            // If there's a catch variable, bind the thrown value to it
            Environment previous = this.environment;
            this.environment = new Environment(environment);
            
            try {
                if (stmt.catchVariable != null) {
                    environment.define(stmt.catchVariable.lexeme, throwEx.value, false);
                }
                execute(stmt.catchBlock);
            } finally {
                this.environment = previous;
            }
        } catch (Thorn.RuntimeError error) {
            // Convert runtime errors to catchable exceptions when inside try block
            Environment previous = this.environment;
            this.environment = new Environment(environment);
            
            try {
                if (stmt.catchVariable != null) {
                    environment.define(stmt.catchVariable.lexeme, error.getMessage(), false);
                }
                execute(stmt.catchBlock);
            } finally {
                this.environment = previous;
            }
        } finally {
            tryDepth--;
        }
        return null;
    }

    @Override
    public Void visitThrowStmt(Stmt.Throw stmt) {
        Object value = evaluate(stmt.value);
        throw new ThornThrowException(value);
    }
}