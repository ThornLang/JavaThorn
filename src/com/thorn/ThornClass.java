package com.thorn;

import java.util.List;
import java.util.Map;

class ThornClass implements ThornCallable {
    final String name;
    private final Map<String, ThornFunction> methods;

    ThornClass(String name, Map<String, ThornFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    ThornFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        ThornInstance instance = new ThornInstance(this);
        
        // Look for init method
        ThornFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        
        return instance;
    }

    @Override
    public int arity() {
        ThornFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }
}