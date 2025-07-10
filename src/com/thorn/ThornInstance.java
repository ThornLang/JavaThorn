package com.thorn;

import java.util.HashMap;
import java.util.Map;

class ThornInstance {
    private ThornClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    ThornInstance(ThornClass klass) {
        this.klass = klass;
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        ThornFunction method = klass.findMethod(name.lexeme);
        if (method != null) return method.bind(this);

        throw new Thorn.RuntimeError(name,
                "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }
    
    ThornClass getKlass() {
        return klass;
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}