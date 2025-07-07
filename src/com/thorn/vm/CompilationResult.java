package com.thorn.vm;

/**
 * Result of compiling Thorn source code to bytecode.
 * Contains the constant pool, bytecode, and main function information.
 */
public class CompilationResult {
    private final ConstantPool constantPool;
    private final int[] bytecode;
    private final FunctionInfo mainFunction;
    
    public CompilationResult(ConstantPool constantPool, int[] bytecode, FunctionInfo mainFunction) {
        this.constantPool = constantPool;
        this.bytecode = bytecode.clone();
        this.mainFunction = mainFunction;
    }
    
    public ConstantPool getConstantPool() {
        return constantPool;
    }
    
    public int[] getBytecode() {
        return bytecode.clone();
    }
    
    public FunctionInfo getMainFunction() {
        return mainFunction;
    }
    
    /**
     * Get the total number of instructions.
     */
    public int getInstructionCount() {
        return bytecode.length;
    }
    
    /**
     * Get instruction at specific index.
     */
    public int getInstruction(int index) {
        if (index < 0 || index >= bytecode.length) {
            throw new IndexOutOfBoundsException("Instruction index out of bounds: " + index);
        }
        return bytecode[index];
    }
    
    /**
     * Print disassembly of the bytecode for debugging.
     */
    public void disassemble() {
        System.out.println("=== Bytecode Disassembly ===");
        System.out.println("Main function: " + mainFunction.getName());
        System.out.println("Instructions: " + bytecode.length);
        System.out.println();
        
        for (int i = 0; i < bytecode.length; i++) {
            int instruction = bytecode[i];
            System.out.printf("%04d: %s%n", i, Instruction.format(instruction));
        }
        
        System.out.println();
        constantPool.printDebugInfo();
    }
    
    @Override
    public String toString() {
        return String.format("CompilationResult{instructions=%d, constants=%d, function=%s}",
                           bytecode.length, constantPool.getConstantCount(), mainFunction.getName());
    }
}