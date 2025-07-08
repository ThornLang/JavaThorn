package com.thorn;

import java.util.List;
import java.util.Objects;

/**
 * Represents a type in the Thorn type system.
 */
public abstract class ThornType {
    public abstract String getName();
    public abstract boolean matches(Object value);
    public abstract boolean isAssignableFrom(ThornType other);
    
    @Override
    public String toString() {
        return getName();
    }
}

/**
 * Basic type (string, number, boolean, null, Any, void)
 */
class ThornPrimitiveType extends ThornType {
    private final String name;
    
    public ThornPrimitiveType(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean matches(Object value) {
        switch (name) {
            case "string":
                return value instanceof String;
            case "number":
                return value instanceof Double;
            case "boolean":
                return value instanceof Boolean;
            case "null":
                return value == null;
            case "Any":
                return true;
            case "void":
                return value == null;
            default:
                return false;
        }
    }
    
    @Override
    public boolean isAssignableFrom(ThornType other) {
        if (name.equals("Any")) return true;
        if (other instanceof ThornPrimitiveType) {
            return name.equals(((ThornPrimitiveType) other).name);
        }
        return false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ThornPrimitiveType that = (ThornPrimitiveType) obj;
        return Objects.equals(name, that.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

/**
 * Generic type (Array[T], Function[T, R], etc.)
 */
class ThornGenericType extends ThornType {
    private final String name;
    private final List<Object> typeArgs;
    
    public ThornGenericType(String name, List<Object> typeArgs) {
        this.name = name;
        this.typeArgs = typeArgs;
    }
    
    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder(name);
        sb.append("[");
        for (int i = 0; i < typeArgs.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(typeArgs.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public boolean matches(Object value) {
        switch (name) {
            case "Array":
                if (!(value instanceof List)) return false;
                if (typeArgs.isEmpty()) return true;
                
                List<?> list = (List<?>) value;
                ThornType elementType = (ThornType) typeArgs.get(0);
                
                for (Object item : list) {
                    if (!elementType.matches(item)) return false;
                }
                return true;
                
            case "Function":
                return value instanceof ThornCallable;
                
            default:
                return false;
        }
    }
    
    @Override
    public boolean isAssignableFrom(ThornType other) {
        if (other instanceof ThornGenericType) {
            ThornGenericType otherGeneric = (ThornGenericType) other;
            return name.equals(otherGeneric.name) && typeArgs.equals(otherGeneric.typeArgs);
        }
        return false;
    }
    
    public List<Object> getTypeArgs() {
        return typeArgs;
    }
}

/**
 * Function type (param1, param2) -> returnType
 */
class ThornFunctionType extends ThornType {
    private final List<Object> paramTypes;
    private final Object returnType;
    
    public ThornFunctionType(List<Object> paramTypes, Object returnType) {
        this.paramTypes = paramTypes;
        this.returnType = returnType;
    }
    
    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramTypes.get(i));
        }
        sb.append(") -> ");
        sb.append(returnType);
        return sb.toString();
    }
    
    @Override
    public boolean matches(Object value) {
        return value instanceof ThornCallable;
    }
    
    @Override
    public boolean isAssignableFrom(ThornType other) {
        if (other instanceof ThornFunctionType) {
            ThornFunctionType otherFunc = (ThornFunctionType) other;
            return paramTypes.equals(otherFunc.paramTypes) && returnType.equals(otherFunc.returnType);
        }
        return false;
    }
    
    public List<Object> getParamTypes() {
        return paramTypes;
    }
    
    public Object getReturnType() {
        return returnType;
    }
}

/**
 * Array type T[]
 */
class ThornArrayType extends ThornType {
    private final Object elementType;
    
    public ThornArrayType(Object elementType) {
        this.elementType = elementType;
    }
    
    @Override
    public String getName() {
        return elementType + "[]";
    }
    
    @Override
    public boolean matches(Object value) {
        if (!(value instanceof List)) return false;
        
        List<?> list = (List<?>) value;
        ThornType elemType = (ThornType) elementType;
        
        for (Object item : list) {
            if (!elemType.matches(item)) return false;
        }
        return true;
    }
    
    @Override
    public boolean isAssignableFrom(ThornType other) {
        if (other instanceof ThornArrayType) {
            ThornArrayType otherArray = (ThornArrayType) other;
            return elementType.equals(otherArray.elementType);
        }
        return false;
    }
    
    public Object getElementType() {
        return elementType;
    }
}

/**
 * Helper class for creating type instances
 */
class ThornTypeFactory {
    public static ThornType createType(String name) {
        return new ThornPrimitiveType(name);
    }
    
    public static ThornType createGenericType(String name, List<Object> typeArgs) {
        return new ThornGenericType(name, typeArgs);
    }
    
    public static ThornType createFunctionType(List<Object> paramTypes, Object returnType) {
        return new ThornFunctionType(paramTypes, returnType);
    }
    
    public static ThornType createArrayType(Object elementType) {
        return new ThornArrayType(elementType);
    }
}