package com.thorn;

import java.util.HashMap;
import java.util.Map;

class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Boolean> immutables = new HashMap<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value, boolean isImmutable) {
        values.put(name, value);
        if (isImmutable) {
            immutables.put(name, true);
        }
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if (enclosing != null) return enclosing.get(name);

        throw new Thorn.RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            if (immutables.containsKey(name.lexeme)) {
                throw new Thorn.RuntimeError(name,
                        "Cannot assign to immutable variable '" + name.lexeme + "'.\n" +
                        "Variable was declared as immutable with @immut.");
            }
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new Thorn.RuntimeError(name,
                "Undefined variable '" + name.lexeme + "'.");
    }
}