package com.thorn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a group of overloaded functions with the same name.
 * Handles overload resolution based on argument count and types.
 */
public class FunctionGroup implements ThornCallable {
    private final String name;
    private final List<ThornCallable> overloads;
    
    public FunctionGroup(String name) {
        this.name = name;
        this.overloads = new ArrayList<>();
    }
    
    public void addOverload(ThornCallable function) {
        // If the function being added is itself a FunctionGroup, 
        // add its individual overloads to prevent nesting
        if (function instanceof FunctionGroup) {
            FunctionGroup group = (FunctionGroup) function;
            // Create a copy of the list to avoid ConcurrentModificationException
            List<ThornCallable> groupOverloads = new ArrayList<>(group.getOverloads());
            for (ThornCallable overload : groupOverloads) {
                addOverload(overload); // Recursive call to handle nested groups
            }
        } else {
            overloads.add(function);
        }
    }
    
    public List<ThornCallable> getOverloads() {
        return overloads;
    }
    
    @Override
    public int arity() {
        // Return -1 to indicate variable arity due to overloading
        return -1;
    }
    
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // Find the best matching overload
        ThornCallable bestMatch = findBestMatch(arguments);
        if (bestMatch == null) {
            throw new RuntimeException("No matching overload found for " + name + 
                                      " with " + arguments.size() + " arguments");
        }
        return bestMatch.call(interpreter, arguments);
    }
    
    private ThornCallable findBestMatch(List<Object> arguments) {
        // First pass: exact arity match
        List<ThornCallable> candidates = new ArrayList<>();
        
        for (ThornCallable overload : overloads) {
            // Skip any FunctionGroup objects that might have somehow gotten into the list
            if (overload instanceof FunctionGroup) {
                continue;
            }
            
            if (overload.arity() < 0) {
                // Variable arity function, consider it
                candidates.add(overload);
            } else if (overload.arity() == arguments.size()) {
                candidates.add(overload);
            }
        }
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        
        // Second pass: type-based selection
        // For now, we'll use parameter type hints if available
        ThornCallable bestMatch = null;
        int bestScore = Integer.MIN_VALUE;  // Start with lowest possible score
        
        for (ThornCallable candidate : candidates) {
            int score = scoreMatch(candidate, arguments);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }
        
        return bestMatch;
    }
    
    private int scoreMatch(ThornCallable function, List<Object> arguments) {
        if (!(function instanceof ThornFunction)) {
            // JavaFunctions (built-ins) get a base score of 0
            return 0;
        }
        
        ThornFunction thornFunc = (ThornFunction) function;
        List<Stmt.Parameter> params = thornFunc.getParameters();
        
        // If no parameters, perfect match for no arguments
        if (params.isEmpty() && arguments.isEmpty()) {
            return 1000;
        }
        
        int score = 0;
        
        // Check each parameter against the corresponding argument
        for (int i = 0; i < params.size() && i < arguments.size(); i++) {
            Stmt.Parameter param = params.get(i);
            Object arg = arguments.get(i);
            
            if (param.type != null) {
                // Parameter has a type annotation
                String expectedType = getTypeNameFromExpr(param.type);
                String actualType = getTypeName(arg);
                
                // Special handling for Any type - it should match everything
                if (expectedType.equals("Any")) {
                    score += 90; // High score for Any type - almost as good as exact match
                }
                // Special handling for null values
                else if (arg == null) {
                    // Penalize typed parameters when argument is null
                    // This encourages selecting untyped overloads for null
                    score -= 50;
                } else if (expectedType.equals(actualType)) {
                    // Exact type match
                    score += 100;
                } else if (isCompatibleType(expectedType, actualType, arg)) {
                    // Compatible but not exact match
                    score += 50;
                } else {
                    // Type mismatch - heavily penalize
                    score -= 1000;
                }
            } else {
                // No type annotation - neutral score
                score += 10;
                // Bonus for untyped parameters when argument is null
                if (arg == null) {
                    score += 30;
                }
            }
        }
        
        // Bonus for having type annotations
        if (thornFunc.hasTypeAnnotations()) {
            score += 20;
        }
        
        return score;
    }
    
    private String getTypeNameFromExpr(Expr type) {
        if (type instanceof Expr.Type) {
            return ((Expr.Type) type).name.lexeme;
        } else if (type instanceof Expr.GenericType) {
            Expr.GenericType generic = (Expr.GenericType) type;
            // For now, just return the base type name
            // Full generic type matching would require more complex logic
            return generic.name.lexeme;
        } else if (type instanceof Expr.ArrayType) {
            return "Array";
        }
        return "Unknown";
    }
    
    private String getTypeName(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "string";
        if (value instanceof Double) return "number";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof List) return "Array";
        if (value instanceof Map) return "Dict";
        if (value instanceof ThornCallable) return "Function";
        if (value instanceof ThornInstance) {
            return ((ThornInstance) value).getKlass().name;
        }
        return value.getClass().getSimpleName();
    }
    
    private boolean isCompatibleType(String expected, String actual, Object value) {
        // Handle null compatibility
        if (value == null || actual.equals("null")) {
            // null is compatible with string, Array, Dict, Function, and class types
            // but not with number or boolean
            return !expected.equals("number") && !expected.equals("boolean");
        }
        
        // Handle numeric compatibility (if we want to allow int/float compatibility in future)
        if (expected.equals("number") && actual.equals("number")) {
            return true;
        }
        
        // For now, no other compatibility rules
        return false;
    }
    
    @Override
    public String toString() {
        return "<function group: " + name + " with " + overloads.size() + " overloads>";
    }
}