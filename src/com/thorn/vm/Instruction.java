package com.thorn.vm;

/**
 * Utility class for working with 32-bit bytecode instructions.
 * Provides convenient methods for creating and manipulating instructions.
 */
public class Instruction {
    
    /**
     * Create an instruction with no operands (like HALT, NOP)
     */
    public static int create(OpCode opcode) {
        return OpCode.makeInstruction(opcode, 0, 0, 0);
    }
    
    /**
     * Create an instruction with one operand in A field (like RETURN, PRINT)
     */
    public static int create(OpCode opcode, int a) {
        return OpCode.makeInstruction(opcode, a, 0, 0);
    }
    
    /**
     * Create an instruction with two operands (like MOVE, NEG, NOT)
     */
    public static int create(OpCode opcode, int a, int b) {
        return OpCode.makeInstruction(opcode, a, b, 0);
    }
    
    /**
     * Create an instruction with three operands (like ADD, SUB, etc.)
     */
    public static int create(OpCode opcode, int a, int b, int c) {
        return OpCode.makeInstruction(opcode, a, b, c);
    }
    
    /**
     * Create an instruction with constant flag for B operand
     */
    public static int createWithConstantB(OpCode opcode, int a, int constantIndex, int c) {
        return OpCode.makeInstruction(opcode, a, constantIndex | OpCode.CONSTANT_FLAG, c);
    }
    
    /**
     * Create an instruction with constant flag for C operand
     */
    public static int createWithConstantC(OpCode opcode, int a, int b, int constantIndex) {
        return OpCode.makeInstruction(opcode, a, b, constantIndex | OpCode.CONSTANT_FLAG);
    }
    
    /**
     * Create an instruction with constant flags for both B and C operands
     */
    public static int createWithConstants(OpCode opcode, int a, int bConstantIndex, int cConstantIndex) {
        return OpCode.makeInstruction(opcode, a, 
                                    bConstantIndex | OpCode.CONSTANT_FLAG, 
                                    cConstantIndex | OpCode.CONSTANT_FLAG);
    }
    
    /**
     * Create a jump instruction with signed offset
     */
    public static int createJump(OpCode opcode, int offset) {
        // For jumps, we use the A field for the offset
        // Sign-extend if necessary (8-bit signed range: -128 to +127)
        return OpCode.makeInstruction(opcode, offset & 0xFF, 0, 0);
    }
    
    /**
     * Create a conditional jump instruction
     */
    public static int createConditionalJump(OpCode opcode, int condition, int offset) {
        return OpCode.makeInstruction(opcode, condition, offset & 0xFF, 0);
    }
    
    /**
     * Get the signed jump offset from a jump instruction
     */
    public static int getJumpOffset(int instruction) {
        int offset = OpCode.getA(instruction);
        // Sign extend from 8 bits to 32 bits
        if ((offset & 0x80) != 0) {
            return offset | 0xFFFFFF00;  // Negative offset
        }
        return offset;  // Positive offset
    }
    
    /**
     * Format instruction for debugging/disassembly
     */
    public static String format(int instruction) {
        OpCode opcode = OpCode.getOpcode(instruction);
        int a = OpCode.getA(instruction);
        int b = OpCode.getB(instruction);
        int c = OpCode.getC(instruction);
        boolean bConst = OpCode.isBConstant(instruction);
        boolean cConst = OpCode.isCConstant(instruction);
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-12s", opcode.name()));
        
        switch (opcode) {
            // No operands
            case HALT:
            case NOP:
                break;
                
            // One operand (A)
            case RETURN:
            case PRINT:
            case CLOCK:
            case JUMP:
                sb.append(String.format(" %d", a));
                break;
                
            // Two operands (A, B)
            case MOVE:
            case NEG:
            case NOT:
            case LOAD_LOCAL:
            case STORE_LOCAL:
            case LOAD_UPVALUE:
            case STORE_UPVALUE:
            case JUMP_IF_FALSE:
            case JUMP_IF_TRUE:
                sb.append(String.format(" %d, %s%d", a, 
                    bConst ? "K" : "R", OpCode.getBValue(instruction)));
                break;
                
            // Three operands (A, B, C)
            default:
                sb.append(String.format(" %d, %s%d, %s%d", a,
                    bConst ? "K" : "R", OpCode.getBValue(instruction),
                    cConst ? "K" : "R", OpCode.getCValue(instruction)));
                break;
        }
        
        return sb.toString();
    }
    
    /**
     * Check if instruction is a jump instruction
     */
    public static boolean isJump(int instruction) {
        OpCode opcode = OpCode.getOpcode(instruction);
        return opcode == OpCode.JUMP || 
               opcode == OpCode.JUMP_IF_FALSE || 
               opcode == OpCode.JUMP_IF_TRUE;
    }
    
    /**
     * Check if instruction is a call instruction
     */
    public static boolean isCall(int instruction) {
        OpCode opcode = OpCode.getOpcode(instruction);
        return opcode == OpCode.CALL || opcode == OpCode.TAIL_CALL;
    }
    
    /**
     * Check if instruction modifies control flow
     */
    public static boolean isControlFlow(int instruction) {
        return isJump(instruction) || isCall(instruction) || 
               OpCode.getOpcode(instruction) == OpCode.RETURN;
    }
}