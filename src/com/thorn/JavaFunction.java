package com.thorn;

import java.util.List;

/**
 * Represents a function implemented in Java (builtin/native function).
 * This is distinct from ThornFunction which represents user-defined functions.
 */
class JavaFunction implements ThornCallable {
    private final String name;
    private final int arity;
    private final JavaFunctionCall implementation;
    
    @FunctionalInterface
    interface JavaFunctionCall {
        Object call(Interpreter interpreter, List<Object> arguments);
    }
    
    JavaFunction(String name, int arity, JavaFunctionCall implementation) {
        this.name = name;
        this.arity = arity;
        this.implementation = implementation;
    }
    
    @Override
    public int arity() {
        return arity;
    }
    
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return implementation.call(interpreter, arguments);
    }
    
    @Override
    public String toString() {
        return "<native fn " + name + ">";
    }
    
    public String getName() {
        return name;
    }
}