package com.thorn;

import java.util.List;

/**
 * Represents a Java class that can be instantiated from Thorn code.
 * Used for stdlib modules that expose classes like RandomGenerator, Hash, Process, etc.
 */
public class JavaClass implements ThornCallable {
    private final String name;
    private final JavaClassConstructor constructor;
    
    @FunctionalInterface
    public interface JavaClassConstructor {
        Object construct(Interpreter interpreter, List<Object> arguments);
    }
    
    public JavaClass(String name, JavaClassConstructor constructor) {
        this.name = name;
        this.constructor = constructor;
    }
    
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return constructor.construct(interpreter, arguments);
    }
    
    @Override
    public int arity() {
        // Variable arity - let the constructor handle validation
        return -1;
    }
    
    @Override
    public String toString() {
        return "<java class " + name + ">";
    }
}