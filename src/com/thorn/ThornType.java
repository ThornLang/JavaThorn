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
                
            case "Dict":
                if (!(value instanceof java.util.Map)) return false;
                if (typeArgs.size() < 2) return true; // No type constraints
                
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
                ThornType keyType = (ThornType) typeArgs.get(0);
                ThornType valueType = (ThornType) typeArgs.get(1);
                
                for (java.util.Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!keyType.matches(entry.getKey())) return false;
                    if (!valueType.matches(entry.getValue())) return false;
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
 * Class type
 */
class ThornClassType extends ThornType {
    private final String className;
    
    public ThornClassType(String className) {
        this.className = className;
    }
    
    @Override
    public String getName() {
        return className;
    }
    
    @Override
    public boolean matches(Object value) {
        if (!(value instanceof ThornInstance)) return false;
        ThornInstance instance = (ThornInstance) value;
        return instance.getKlass().name.equals(className);
    }
    
    @Override
    public boolean isAssignableFrom(ThornType other) {
        if (other instanceof ThornClassType) {
            // For now, just check exact class match
            // Future: support inheritance
            return className.equals(((ThornClassType) other).className);
        }
        return false;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ThornClassType that = (ThornClassType) obj;
        return Objects.equals(className, that.className);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(className);
    }
}

/**
 * Result type for error handling
 */
class ThornResultType extends ThornType {
    private final Object valueType;
    private final Object errorType;
    
    public ThornResultType(Object valueType, Object errorType) {
        this.valueType = valueType;
        this.errorType = errorType;
    }
    
    @Override
    public String getName() {
        return "Result[" + valueType + ", " + errorType + "]";
    }
    
    @Override
    public boolean matches(Object value) {
        return value instanceof ThornResult;
    }
    
    @Override
    public boolean isAssignableFrom(ThornType other) {
        if (other instanceof ThornResultType) {
            ThornResultType otherResult = (ThornResultType) other;
            return valueType.equals(otherResult.valueType) && errorType.equals(otherResult.errorType);
        }
        return false;
    }
    
    public Object getValueType() {
        return valueType;
    }
    
    public Object getErrorType() {
        return errorType;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ThornResultType that = (ThornResultType) obj;
        return Objects.equals(valueType, that.valueType) && Objects.equals(errorType, that.errorType);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valueType, errorType);
    }
}

/**
 * Type parameter for generic types (T, K, V, etc.)
 */
class ThornTypeParameter extends ThornType {
    private final String name;
    private final ThornType constraint; // Optional constraint
    
    public ThornTypeParameter(String name) {
        this(name, null);
    }
    
    public ThornTypeParameter(String name, ThornType constraint) {
        this.name = name;
        this.constraint = constraint;
    }
    
    @Override
    public String getName() {
        if (constraint != null) {
            return name + ": " + constraint.getName();
        }
        return name;
    }
    
    @Override
    public boolean matches(Object value) {
        // Type parameters match any value during checking
        // Actual type checking happens after substitution
        if (constraint != null) {
            return constraint.matches(value);
        }
        return true;
    }
    
    @Override
    public boolean isAssignableFrom(ThornType other) {
        // Type parameters are assignable from themselves
        if (other instanceof ThornTypeParameter) {
            return name.equals(((ThornTypeParameter) other).name);
        }
        // If constrained, check constraint compatibility
        if (constraint != null) {
            return constraint.isAssignableFrom(other);
        }
        return true;
    }
    
    public String getParameterName() {
        return name;
    }
    
    public ThornType getConstraint() {
        return constraint;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ThornTypeParameter that = (ThornTypeParameter) obj;
        return Objects.equals(name, that.name) && Objects.equals(constraint, that.constraint);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, constraint);
    }
}

/**
 * Helper class for creating type instances
 */
class ThornTypeFactory {
    public static ThornType createType(String name) {
        // Check if this is a known primitive type
        switch (name) {
            case "string":
            case "number":
            case "boolean":
            case "null":
            case "Any":
            case "void":
                return new ThornPrimitiveType(name);
            default:
                // Check if it's a single uppercase letter (likely a type parameter)
                if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
                    return new ThornTypeParameter(name);
                }
                // Assume it's a class type
                return new ThornClassType(name);
        }
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
    
    public static ThornType createResultType(Object valueType, Object errorType) {
        return new ThornResultType(valueType, errorType);
    }
    
    public static ThornType createTypeParameter(String name) {
        return new ThornTypeParameter(name);
    }
    
    public static ThornType createTypeParameter(String name, ThornType constraint) {
        return new ThornTypeParameter(name, constraint);
    }
}