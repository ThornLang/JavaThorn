package com.thorn.vm;

/**
 * Bytecode instruction opcodes for the Thorn VM.
 * 
 * 32-bit instruction format:
 * [31-26] [25-18] [17-9] [8-0]
 * Opcode    A       B      C
 * 
 * Where:
 * - Opcode (6 bits): 64 possible instructions
 * - A (8 bits): Destination register
 * - B (9 bits): Source register/constant index (bit 8 = isConstant flag)
 * - C (9 bits): Source register/constant index (bit 8 = isConstant flag)
 */
public enum OpCode {
    // Load/Store Operations (0-7)
    LOAD_CONSTANT(0),    // A = constants[B]
    LOAD_LOCAL(1),       // A = locals[B]
    STORE_LOCAL(2),      // locals[A] = B
    LOAD_GLOBAL(3),      // A = globals[constants[B]]
    STORE_GLOBAL(4),     // globals[constants[A]] = B
    LOAD_UPVALUE(5),     // A = upvalues[B]
    STORE_UPVALUE(6),    // upvalues[A] = B
    MOVE(7),             // A = B
    
    // Arithmetic Operations (8-15)
    ADD(8),              // A = B + C
    SUB(9),              // A = B - C
    MUL(10),             // A = B * C
    DIV(11),             // A = B / C
    MOD(12),             // A = B % C
    POW(13),             // A = B ** C
    NEG(14),             // A = -B
    
    // Fast arithmetic for numbers (16-19)
    ADD_FAST(16),        // A = B + C (numbers only)
    SUB_FAST(17),        // A = B - C (numbers only)
    MUL_FAST(18),        // A = B * C (numbers only)
    DIV_FAST(19),        // A = B / C (numbers only)
    
    // Comparison Operations (20-27)
    EQ(20),              // A = B == C
    NE(21),              // A = B != C
    LT(22),              // A = B < C
    LE(23),              // A = B <= C
    GT(24),              // A = B > C
    GE(25),              // A = B >= C
    EQ_FAST(26),         // A = B == C (numbers only)
    LT_FAST(27),         // A = B < C (numbers only)
    
    // Logical Operations (28-31)
    AND(28),             // A = B && C
    OR(29),              // A = B || C
    NOT(30),             // A = !B
    NULL_COALESCE(31),   // A = B ?? C (Thorn-specific)
    
    // Control Flow (32-39)
    JUMP(32),            // pc += A (signed offset)
    JUMP_IF_FALSE(33),   // if (!A) pc += B
    JUMP_IF_TRUE(34),    // if (A) pc += B
    CALL(35),            // Call function at A with B args
    RETURN(36),          // Return A
    TAIL_CALL(37),       // Tail call optimization
    
    // Object Operations (40-47)
    NEW_OBJECT(40),      // A = new Object()
    GET_PROPERTY(41),    // A = B.constants[C]
    SET_PROPERTY(42),    // B.constants[A] = C
    GET_INDEX(43),       // A = B[C]
    SET_INDEX(44),       // B[A] = C
    NEW_ARRAY(45),       // A = new Array(B elements from stack)
    NEW_DICT(46),        // A = new Dict(B pairs from stack)
    
    // Function Operations (48-51)
    MAKE_CLOSURE(48),    // A = closure(constants[B], C upvalues)
    CLOSE_UPVALUE(49),   // Close upvalue at stack position A
    
    // Array Operations (52-55)
    ARRAY_PUSH(52),      // A.push(B)
    ARRAY_POP(53),       // A = B.pop()
    ARRAY_LENGTH(54),    // A = B.length
    
    // Built-in Operations (56-63)
    PRINT(56),           // print(A)
    CLOCK(57),           // A = clock()
    TYPE_OF(58),         // A = typeof(B)
    LOAD_IMMUTABLE(59),  // A = immutable_globals[constants[B]]
    MATCH_PATTERN(60),   // Pattern matching optimization
    LAMBDA_CREATE(61),   // Create lambda function
    NOP(62),             // No operation
    HALT(63),            // Stop execution
    
    // Superinstructions for common patterns (64-71)
    ADD_LOCALS(64),              // A = locals[B] + locals[C] (direct register add)
    ADD_CONST_TO_LOCAL(65),      // A = constants[B] + locals[C]
    LOAD_CONST_ADD(66),          // A = B + constants[C] (load and add in one)
    CMP_JUMP_IF_FALSE(67),       // if (!A) pc += B (compare and jump combined)
    INCREMENT_LOCAL(68),         // locals[A] = locals[A] + 1 (in-place increment)
    LOAD_LOCAL_LOAD_LOCAL(69),   // Load two locals: A = locals[B], A+1 = locals[C]
    STORE_LOCAL_STORE_LOCAL(70), // Store two locals: locals[A] = B, locals[A+1] = C
    JUMP_BACK(71);               // pc = A (absolute jump for tail calls)
    
    private final int code;
    
    OpCode(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    public static OpCode fromCode(int code) {
        for (OpCode op : values()) {
            if (op.code == code) {
                return op;
            }
        }
        throw new IllegalArgumentException("Invalid opcode: " + code);
    }
    
    // Instruction format helpers
    public static final int OPCODE_MASK = 0xFC000000;  // 6 bits starting at bit 26
    public static final int A_MASK = 0x03FC0000;       // 8 bits starting at bit 18
    public static final int B_MASK = 0x0003FE00;       // 9 bits starting at bit 9
    public static final int C_MASK = 0x000001FF;       // 9 bits starting at bit 0
    
    public static final int OPCODE_SHIFT = 26;
    public static final int A_SHIFT = 18;
    public static final int B_SHIFT = 9;
    public static final int C_SHIFT = 0;
    
    // Constant flag in B and C operands (bit 8)
    public static final int CONSTANT_FLAG = 0x100;
    
    // Create instruction
    public static int makeInstruction(OpCode opcode, int a, int b, int c) {
        return (opcode.getCode() << OPCODE_SHIFT) |
               ((a & 0xFF) << A_SHIFT) |
               ((b & 0x1FF) << B_SHIFT) |
               (c & 0x1FF);
    }
    
    // Extract instruction components
    public static OpCode getOpcode(int instruction) {
        return fromCode((instruction & OPCODE_MASK) >>> OPCODE_SHIFT);
    }
    
    public static int getA(int instruction) {
        return (instruction & A_MASK) >>> A_SHIFT;
    }
    
    public static int getB(int instruction) {
        return (instruction & B_MASK) >>> B_SHIFT;
    }
    
    public static int getC(int instruction) {
        return instruction & C_MASK;
    }
    
    public static boolean isBConstant(int instruction) {
        return (getB(instruction) & CONSTANT_FLAG) != 0;
    }
    
    public static boolean isCConstant(int instruction) {
        return (getC(instruction) & CONSTANT_FLAG) != 0;
    }
    
    public static int getBValue(int instruction) {
        return getB(instruction) & ~CONSTANT_FLAG;
    }
    
    public static int getCValue(int instruction) {
        return getC(instruction) & ~CONSTANT_FLAG;
    }
}