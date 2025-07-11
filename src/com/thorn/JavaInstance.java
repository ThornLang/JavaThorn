package com.thorn;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for Java objects returned from stdlib modules to make their methods accessible from Thorn.
 */
public class JavaInstance {
    private final Object instance;
    private final Class<?> clazz;
    private final Map<String, JavaFunction> methodCache = new HashMap<>();
    
    public JavaInstance(Object instance) {
        this.instance = instance;
        this.clazz = instance.getClass();
    }
    
    public Object get(Token name) {
        String methodName = name.lexeme;
        
        // Check cache first
        if (methodCache.containsKey(methodName)) {
            return methodCache.get(methodName);
        }
        
        // Look for a matching method (use original method names)
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                JavaFunction function = createBoundMethod(methodName, method);
                methodCache.put(methodName, function);
                return function;
            }
        }
        
        throw new Thorn.RuntimeError(name, 
            "Property '" + methodName + "' is not defined on type '" + clazz.getSimpleName() + "'.");
    }
    
    private JavaFunction createBoundMethod(String name, Method method) {
        return new JavaFunction(name, method.getParameterCount(), (interpreter, arguments) -> {
            try {
                Object[] args = new Object[arguments.size()];
                Class<?>[] paramTypes = method.getParameterTypes();
                for (int i = 0; i < arguments.size(); i++) {
                    args[i] = convertArgument(arguments.get(i), paramTypes[i]);
                }
                Object result = method.invoke(instance, args);
                
                // Wrap returned Java objects
                if (result != null && shouldWrap(result)) {
                    return new JavaInstance(result);
                }
                return result;
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (cause instanceof com.thorn.StdlibException) {
                    throw new Thorn.RuntimeError(null, cause.getMessage());
                }
                throw new Thorn.RuntimeError(null, "Error calling " + name + ": " + cause.getMessage());
            }
        });
    }
    
    private boolean shouldWrap(Object obj) {
        // Don't wrap primitives, strings, lists, maps, or already wrapped objects
        return !(obj instanceof String || 
                 obj instanceof Number || 
                 obj instanceof Boolean ||
                 obj instanceof java.util.List ||
                 obj instanceof java.util.Map ||
                 obj instanceof JavaInstance ||
                 obj instanceof ThornInstance);
    }
    
    
    private Object convertArgument(Object thornValue, Class<?> targetType) {
        if (thornValue == null) {
            return null;
        }
        
        // Handle primitive types and their wrappers
        if (targetType == String.class) {
            return thornValue.toString();
        } else if (targetType == double.class || targetType == Double.class) {
            if (thornValue instanceof Double) {
                return thornValue;
            } else if (thornValue instanceof Number) {
                return ((Number) thornValue).doubleValue();
            }
        } else if (targetType == int.class || targetType == Integer.class) {
            if (thornValue instanceof Double) {
                return ((Double) thornValue).intValue();
            } else if (thornValue instanceof Number) {
                return ((Number) thornValue).intValue();
            }
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            if (thornValue instanceof Boolean) {
                return thornValue;
            }
        } else if (targetType == java.util.List.class) {
            if (thornValue instanceof java.util.List) {
                return thornValue;
            }
        } else if (targetType == java.util.Map.class) {
            if (thornValue instanceof java.util.Map) {
                return thornValue;
            }
        } else if (targetType == Object.class) {
            return thornValue;
        }
        
        // If no conversion found, return as-is and let Java handle it
        return thornValue;
    }
    
    public Object getWrappedInstance() {
        return instance;
    }
    
    @Override
    public String toString() {
        return instance.toString();
    }
}