package com.thorn;

import java.util.List;

class ThornFunction implements ThornCallable {
    private final String name;
    private final List<Stmt.Parameter> params;
    private final List<Stmt> body;
    private final Environment closure;
    private final ThornType returnType;

    ThornFunction(String name, List<Stmt.Parameter> params, List<Stmt> body, Environment closure, ThornType returnType) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.closure = closure;
        this.returnType = returnType;
    }
    
    // Legacy constructor for backward compatibility
    ThornFunction(String name, List<Token> legacyParams, List<Stmt> body, Environment closure) {
        this.name = name;
        this.params = new java.util.ArrayList<>();
        for (Token param : legacyParams) {
            this.params.add(new Stmt.Parameter(param, null));
        }
        this.body = body;
        this.closure = closure;
        this.returnType = null;
    }

    @Override
    public int arity() {
        return params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        
        // Type check parameters and bind them
        for (int i = 0; i < params.size(); i++) {
            Stmt.Parameter param = params.get(i);
            Object argument = arguments.get(i);
            
            // Perform type checking if parameter has a type annotation
            if (param.type != null) {
                ThornType paramType = (ThornType) interpreter.evaluateType(param.type);
                if (!paramType.matches(argument)) {
                    throw new Thorn.RuntimeError(param.name, "Type error: expected " + paramType.getName() + 
                                         " but got " + getTypeName(argument) + 
                                         " for parameter '" + param.name.lexeme + "'");
                }
            }
            
            environment.define(param.name.lexeme, argument, false);
        }

        // Save current return state
        Object previousReturnValue = interpreter.returnValue;
        boolean previousHasReturned = interpreter.hasReturned;
        
        // Reset return state for this function call
        interpreter.returnValue = null;
        interpreter.hasReturned = false;
        
        interpreter.executeBlock(body, environment);
        
        // Get the return value
        Object result = interpreter.hasReturned ? interpreter.returnValue : null;
        
        // Type check return value if function has return type annotation
        if (returnType != null && result != null) {
            if (!returnType.matches(result)) {
                throw new Thorn.RuntimeError(new Token(TokenType.RETURN, "return", null, -1), 
                                     "Type error: expected return type " + returnType.getName() + 
                                     " but got " + getTypeName(result));
            }
        }
        
        // Restore previous return state
        interpreter.returnValue = previousReturnValue;
        interpreter.hasReturned = previousHasReturned;
        
        return result;
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

    ThornFunction bind(ThornInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance, false);
        return new ThornFunction(name, params, body, environment, returnType);
    }

    public boolean hasTypeAnnotations() {
        // Check if any parameters have type annotations
        for (Stmt.Parameter param : params) {
            if (param.type != null) {
                return true;
            }
        }
        // Also check if return type is specified
        return returnType != null;
    }
    
    public List<Stmt.Parameter> getParameters() {
        return params;
    }

    @Override
    public String toString() {
        if (name == null) return "<lambda>";
        return "<fn " + name + ">";
    }
}