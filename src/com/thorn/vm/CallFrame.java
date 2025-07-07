package com.thorn.vm;

/**
 * Represents a function call frame in the VM.
 * Contains function information, return address, and register frame.
 */
public class CallFrame {
    private final FunctionInfo function;
    private final int returnAddress;        // Where to return to in caller
    private final int frameBase;           // Base register for this frame
    private final Object[] registers;      // Local registers for this frame
    private int pc;                        // Current program counter within function
    
    public CallFrame(FunctionInfo function, int returnAddress, int frameBase) {
        this.function = function;
        this.returnAddress = returnAddress;
        this.frameBase = frameBase;
        this.registers = new Object[function.getFrameSize()];
        this.pc = function.getStartPc();
    }
    
    public FunctionInfo getFunction() {
        return function;
    }
    
    public int getReturnAddress() {
        return returnAddress;
    }
    
    public int getFrameBase() {
        return frameBase;
    }
    
    public Object[] getRegisters() {
        return registers;
    }
    
    public int getPc() {
        return pc;
    }
    
    public void setPc(int pc) {
        this.pc = pc;
    }
    
    public void incrementPc() {
        this.pc++;
    }
    
    /**
     * Get a register value.
     */
    public Object getRegister(int index) {
        if (index < 0 || index >= registers.length) {
            throw new IndexOutOfBoundsException("Register index out of bounds: " + index);
        }
        return registers[index];
    }
    
    /**
     * Set a register value.
     */
    public void setRegister(int index, Object value) {
        if (index < 0 || index >= registers.length) {
            throw new IndexOutOfBoundsException("Register index out of bounds: " + index);
        }
        registers[index] = value;
    }
    
    /**
     * Get the number of registers in this frame.
     */
    public int getRegisterCount() {
        return registers.length;
    }
    
    /**
     * Clear all registers in this frame.
     */
    public void clearRegisters() {
        for (int i = 0; i < registers.length; i++) {
            registers[i] = null;
        }
    }
    
    /**
     * Copy argument values into parameter registers.
     */
    public void setArguments(Object[] args) {
        int paramCount = Math.min(args.length, function.getArity());
        for (int i = 0; i < paramCount; i++) {
            registers[i] = args[i];
        }
        
        // Fill remaining parameters with null if not enough arguments provided
        for (int i = paramCount; i < function.getArity(); i++) {
            registers[i] = null;
        }
    }
    
    /**
     * Get the current instruction from the function's bytecode.
     */
    public int getCurrentInstruction() {
        int[] bytecode = function.getBytecode();
        if (pc >= 0 && pc < bytecode.length) {
            return bytecode[pc];
        }
        throw new IndexOutOfBoundsException("Program counter out of bounds: " + pc);
    }
    
    /**
     * Check if there are more instructions to execute.
     */
    public boolean hasMoreInstructions() {
        return pc >= 0 && pc < function.getBytecode().length;
    }
    
    @Override
    public String toString() {
        return String.format("CallFrame{function=%s, pc=%d, returnAddr=%d, frameBase=%d, registers=%d}",
                           function.getName(), pc, returnAddress, frameBase, registers.length);
    }
}