package com.thorn.vm;

/**
 * Bytecode disassembler for debugging VM issues.
 */
public class Disassembler {
    private final ConstantPool constantPool;
    
    public Disassembler(ConstantPool constantPool) {
        this.constantPool = constantPool;
    }
    
    public void disassemble(int[] bytecode, String name) {
        System.err.println("=== " + name + " ===");
        
        for (int i = 0; i < bytecode.length; i++) {
            int instruction = bytecode[i];
            OpCode opcode = OpCode.getOpcode(instruction);
            int a = OpCode.getA(instruction);
            int b = OpCode.getB(instruction);
            int c = OpCode.getC(instruction);
            
            System.err.printf("%04d: %-15s", i, opcode);
            
            switch (opcode) {
                case LOAD_CONSTANT:
                    Object constant = constantPool.getConstant(OpCode.getBValue(instruction));
                    System.err.printf(" R%d = %s", a, constant);
                    break;
                case ADD:
                case SUB:
                case MUL:
                case DIV:
                case LT:
                case GT:
                case EQ:
                case GET_INDEX:
                    System.err.printf(" R%d = R%d %s R%d", a, b, getOperatorSymbol(opcode), c);
                    break;
                case ARRAY_LENGTH:
                    System.err.printf(" R%d = R%d.length", a, b);
                    break;
                case JUMP:
                    System.err.printf(" pc += %d", (short) (instruction & 0xFFFF));
                    break;
                case JUMP_IF_FALSE:
                    System.err.printf(" if (!R%d) pc += %d", a, (short) (instruction & 0xFFFF));
                    break;
                case PRINT:
                    System.err.printf(" print(R%d)", a);
                    break;
                case HALT:
                    System.err.printf(" halt");
                    break;
                default:
                    System.err.printf(" R%d, R%d, R%d", a, b, c);
                    break;
            }
            System.err.println();
        }
        System.err.println();
    }
    
    private String getOperatorSymbol(OpCode opcode) {
        switch (opcode) {
            case ADD: return "+";
            case SUB: return "-";
            case MUL: return "*";
            case DIV: return "/";
            case LT: return "<";
            case GT: return ">";
            case EQ: return "==";
            case GET_INDEX: return "[]";
            default: return opcode.toString();
        }
    }
}