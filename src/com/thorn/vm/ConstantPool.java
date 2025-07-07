package com.thorn.vm;

import java.util.*;

/**
 * Constant pool for storing literals, strings, and function metadata.
 * Provides efficient storage and retrieval of compile-time constants.
 */
public class ConstantPool {
    private final List<Object> constants;
    private final Map<Object, Integer> constantIndex;
    private final List<String> strings;
    private final Map<String, Integer> stringIndex;
    private final List<FunctionInfo> functions;
    private final Map<String, Integer> functionIndex;
    
    public ConstantPool() {
        this.constants = new ArrayList<>();
        this.constantIndex = new HashMap<>();
        this.strings = new ArrayList<>();
        this.stringIndex = new HashMap<>();
        this.functions = new ArrayList<>();
        this.functionIndex = new HashMap<>();
    }
    
    /**
     * Add a constant to the pool, returning its index.
     * Deduplicates identical constants.
     */
    public int addConstant(Object value) {
        if (value == null) {
            return addConstantDirect(null);
        }
        
        // Handle special cases for efficient storage
        if (value instanceof String) {
            return addString((String) value);
        }
        
        if (value instanceof Double) {
            // Intern common numbers for efficiency
            Double d = (Double) value;
            if (d == 0.0) return addConstantDirect(0.0);
            if (d == 1.0) return addConstantDirect(1.0);
            if (d == -1.0) return addConstantDirect(-1.0);
        }
        
        return addConstantDirect(value);
    }
    
    private int addConstantDirect(Object value) {
        Integer existing = constantIndex.get(value);
        if (existing != null) {
            return existing;
        }
        
        int index = constants.size();
        constants.add(value);
        constantIndex.put(value, index);
        return index;
    }
    
    /**
     * Add a string to the string pool with interning.
     */
    public int addString(String str) {
        Integer existing = stringIndex.get(str);
        if (existing != null) {
            return existing;
        }
        
        int stringPoolIndex = strings.size();
        strings.add(str.intern());  // Intern for memory efficiency
        
        // Also add to main constant pool
        int constantPoolIndex = addConstantDirect(str.intern());
        stringIndex.put(str, constantPoolIndex);  // Store constant pool index, not string pool index
        
        return constantPoolIndex;
    }
    
    /**
     * Add function metadata to the pool.
     */
    public int addFunction(FunctionInfo function) {
        String name = function.getName();
        Integer existing = functionIndex.get(name);
        if (existing != null) {
            // Update existing function info
            functions.set(existing, function);
            return existing;
        }
        
        int index = functions.size();
        functions.add(function);
        functionIndex.put(name, index);
        return index;
    }
    
    /**
     * Get constant by index.
     */
    public Object getConstant(int index) {
        if (index < 0 || index >= constants.size()) {
            throw new IndexOutOfBoundsException("Constant index out of bounds: " + index);
        }
        return constants.get(index);
    }
    
    /**
     * Get string by index from string pool.
     */
    public String getString(int index) {
        if (index < 0 || index >= strings.size()) {
            throw new IndexOutOfBoundsException("String index out of bounds: " + index);
        }
        return strings.get(index);
    }
    
    /**
     * Get function info by index.
     */
    public FunctionInfo getFunction(int index) {
        if (index < 0 || index >= functions.size()) {
            throw new IndexOutOfBoundsException("Function index out of bounds: " + index);
        }
        return functions.get(index);
    }
    
    /**
     * Get the index of a constant, or -1 if not found.
     */
    public int getConstantIndex(Object value) {
        Integer index = constantIndex.get(value);
        return index != null ? index : -1;
    }
    
    /**
     * Get the index of a string, or -1 if not found.
     */
    public int getStringIndex(String str) {
        Integer index = stringIndex.get(str);
        return index != null ? index : -1;
    }
    
    /**
     * Get the index of a function, or -1 if not found.
     */
    public int getFunctionIndex(String name) {
        Integer index = functionIndex.get(name);
        return index != null ? index : -1;
    }
    
    /**
     * Get the number of constants in the pool.
     */
    public int getConstantCount() {
        return constants.size();
    }
    
    /**
     * Get the number of strings in the pool.
     */
    public int getStringCount() {
        return strings.size();
    }
    
    /**
     * Get the number of functions in the pool.
     */
    public int getFunctionCount() {
        return functions.size();
    }
    
    /**
     * Check if the pool contains a constant.
     */
    public boolean containsConstant(Object value) {
        return constantIndex.containsKey(value);
    }
    
    /**
     * Clear the constant pool.
     */
    public void clear() {
        constants.clear();
        constantIndex.clear();
        strings.clear();
        stringIndex.clear();
        functions.clear();
        functionIndex.clear();
    }
    
    /**
     * Get all constants as a read-only list.
     */
    public List<Object> getAllConstants() {
        return Collections.unmodifiableList(constants);
    }
    
    /**
     * Get all strings as a read-only list.
     */
    public List<String> getAllStrings() {
        return Collections.unmodifiableList(strings);
    }
    
    /**
     * Get all functions as a read-only list.
     */
    public List<FunctionInfo> getAllFunctions() {
        return Collections.unmodifiableList(functions);
    }
    
    /**
     * Print debug information about the constant pool.
     */
    public void printDebugInfo() {
        System.out.println("=== Constant Pool Debug Info ===");
        System.out.println("Constants (" + constants.size() + "):");
        for (int i = 0; i < constants.size(); i++) {
            Object value = constants.get(i);
            String type = value != null ? value.getClass().getSimpleName() : "null";
            System.out.printf("  [%d] %s: %s%n", i, type, value);
        }
        
        System.out.println("Strings (" + strings.size() + "):");
        for (int i = 0; i < strings.size(); i++) {
            System.out.printf("  [%d] \"%s\"%n", i, strings.get(i));
        }
        
        System.out.println("Functions (" + functions.size() + "):");
        for (int i = 0; i < functions.size(); i++) {
            FunctionInfo func = functions.get(i);
            System.out.printf("  [%d] %s (arity: %d, locals: %d, upvalues: %d)%n", 
                            i, func.getName(), func.getArity(), 
                            func.getLocalCount(), func.getUpvalueCount());
        }
        System.out.println("================================");
    }
    
    @Override
    public String toString() {
        return String.format("ConstantPool{constants=%d, strings=%d, functions=%d}", 
                           constants.size(), strings.size(), functions.size());
    }
}