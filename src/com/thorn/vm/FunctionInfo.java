package com.thorn.vm;

import java.util.Arrays;

/**
 * Metadata about a function for the VM.
 * Stores information needed for function calls, register allocation, and optimization.
 */
public class FunctionInfo {
    private final String name;
    private final int arity;           // Number of parameters
    private final int localCount;      // Number of local variables (including parameters)
    private final int upvalueCount;    // Number of upvalues (for closures)
    private final int[] bytecode;      // Function bytecode
    private final int startPc;         // Starting program counter
    private final boolean isVariadic;  // Whether function accepts variable arguments
    private final String[] parameterNames;  // Parameter names for debugging
    private final String[] localNames;      // Local variable names for debugging
    private final UpvalueInfo[] upvalues;   // Upvalue information
    
    public FunctionInfo(String name, int arity, int localCount, int upvalueCount, 
                       int[] bytecode, int startPc) {
        this(name, arity, localCount, upvalueCount, bytecode, startPc, false, null, null, null);
    }
    
    public FunctionInfo(String name, int arity, int localCount, int upvalueCount,
                       int[] bytecode, int startPc, boolean isVariadic,
                       String[] parameterNames, String[] localNames, UpvalueInfo[] upvalues) {
        this.name = name;
        this.arity = arity;
        this.localCount = localCount;
        this.upvalueCount = upvalueCount;
        this.bytecode = bytecode != null ? bytecode.clone() : new int[0];
        this.startPc = startPc;
        this.isVariadic = isVariadic;
        this.parameterNames = parameterNames != null ? parameterNames.clone() : new String[0];
        this.localNames = localNames != null ? localNames.clone() : new String[0];
        this.upvalues = upvalues != null ? upvalues.clone() : new UpvalueInfo[0];
    }
    
    public String getName() {
        return name;
    }
    
    public int getArity() {
        return arity;
    }
    
    public int getLocalCount() {
        return localCount;
    }
    
    public int getUpvalueCount() {
        return upvalueCount;
    }
    
    public int[] getBytecode() {
        return bytecode.clone();
    }
    
    public int getStartPc() {
        return startPc;
    }
    
    public boolean isVariadic() {
        return isVariadic;
    }
    
    public String[] getParameterNames() {
        return parameterNames.clone();
    }
    
    public String[] getLocalNames() {
        return localNames.clone();
    }
    
    public UpvalueInfo[] getUpvalues() {
        return upvalues.clone();
    }
    
    /**
     * Get parameter name by index.
     */
    public String getParameterName(int index) {
        if (index >= 0 && index < parameterNames.length) {
            return parameterNames[index];
        }
        return "param" + index;
    }
    
    /**
     * Get local variable name by index.
     */
    public String getLocalName(int index) {
        if (index >= 0 && index < localNames.length) {
            return localNames[index];
        }
        return "local" + index;
    }
    
    /**
     * Get upvalue info by index.
     */
    public UpvalueInfo getUpvalue(int index) {
        if (index >= 0 && index < upvalues.length) {
            return upvalues[index];
        }
        throw new IndexOutOfBoundsException("Upvalue index out of bounds: " + index);
    }
    
    /**
     * Check if this function needs a closure (has upvalues).
     */
    public boolean needsClosure() {
        return upvalueCount > 0;
    }
    
    /**
     * Calculate the total frame size needed for this function.
     * Includes locals and temporary registers.
     */
    public int getFrameSize() {
        // For now, use localCount + many temporary registers
        // Reserve space for nested loop control registers (250+)
        // In a more sophisticated implementation, this would be calculated
        // based on register allocation analysis
        return Math.max(localCount + 200, 256);  // 256 registers total
    }
    
    @Override
    public String toString() {
        return String.format("FunctionInfo{name='%s', arity=%d, locals=%d, upvalues=%d, variadic=%s}",
                           name, arity, localCount, upvalueCount, isVariadic);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        FunctionInfo that = (FunctionInfo) obj;
        return arity == that.arity &&
               localCount == that.localCount &&
               upvalueCount == that.upvalueCount &&
               startPc == that.startPc &&
               isVariadic == that.isVariadic &&
               name.equals(that.name) &&
               Arrays.equals(bytecode, that.bytecode);
    }
    
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + arity;
        result = 31 * result + localCount;
        result = 31 * result + upvalueCount;
        result = 31 * result + startPc;
        result = 31 * result + (isVariadic ? 1 : 0);
        result = 31 * result + Arrays.hashCode(bytecode);
        return result;
    }
    
    /**
     * Create a builder for constructing FunctionInfo objects.
     */
    public static class Builder {
        private String name;
        private int arity = 0;
        private int localCount = 0;
        private int upvalueCount = 0;
        private int[] bytecode = new int[0];
        private int startPc = 0;
        private boolean isVariadic = false;
        private String[] parameterNames;
        private String[] localNames;
        private UpvalueInfo[] upvalues;
        
        public Builder(String name) {
            this.name = name;
        }
        
        public Builder arity(int arity) {
            this.arity = arity;
            return this;
        }
        
        public Builder localCount(int localCount) {
            this.localCount = localCount;
            return this;
        }
        
        public Builder upvalueCount(int upvalueCount) {
            this.upvalueCount = upvalueCount;
            return this;
        }
        
        public Builder bytecode(int[] bytecode) {
            this.bytecode = bytecode;
            return this;
        }
        
        public Builder startPc(int startPc) {
            this.startPc = startPc;
            return this;
        }
        
        public Builder variadic(boolean isVariadic) {
            this.isVariadic = isVariadic;
            return this;
        }
        
        public Builder parameterNames(String[] parameterNames) {
            this.parameterNames = parameterNames;
            return this;
        }
        
        public Builder localNames(String[] localNames) {
            this.localNames = localNames;
            return this;
        }
        
        public Builder upvalues(UpvalueInfo[] upvalues) {
            this.upvalues = upvalues;
            return this;
        }
        
        public FunctionInfo build() {
            return new FunctionInfo(name, arity, localCount, upvalueCount, bytecode, startPc,
                                  isVariadic, parameterNames, localNames, upvalues);
        }
    }
}