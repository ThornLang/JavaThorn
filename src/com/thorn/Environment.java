package com.thorn;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Boolean> immutables = new HashMap<>();
    
    // Cache for frequently accessed variables
    private String lastAccessedName;
    private Object lastAccessedValue;
    
    // Environment pooling for performance
    private static final List<Environment> pool = new ArrayList<>();
    private static final int MAX_POOL_SIZE = 100;

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }
    
    // Factory method for pooled environments
    static Environment create(Environment enclosing) {
        Environment env;
        synchronized (pool) {
            if (!pool.isEmpty()) {
                env = pool.remove(pool.size() - 1);
                env.reset(enclosing);
            } else {
                env = new Environment(enclosing);
            }
        }
        return env;
    }
    
    // Reset environment for reuse
    private void reset(Environment newEnclosing) {
        values.clear();
        immutables.clear();
        lastAccessedName = null;
        lastAccessedValue = null;
        // Note: enclosing is final, so we can't change it
    }
    
    // Return environment to pool
    void release() {
        synchronized (pool) {
            if (pool.size() < MAX_POOL_SIZE) {
                pool.add(this);
            }
        }
    }

    void define(String name, Object value, boolean isImmutable) {
        values.put(name, value);
        if (isImmutable) {
            immutables.put(name, true);
        }
    }

    Object get(Token name) {
        // Check cache first
        if (name.lexeme.equals(lastAccessedName)) {
            return lastAccessedValue;
        }
        
        Object value = values.get(name.lexeme);
        if (value != null) {
            // Cache the access
            lastAccessedName = name.lexeme;
            lastAccessedValue = value;
            return value;
        }
        
        // Check if key exists but value is null
        if (values.containsKey(name.lexeme)) {
            return null;
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
            
            // Update cache if this was the last accessed variable
            if (name.lexeme.equals(lastAccessedName)) {
                lastAccessedValue = value;
            }
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