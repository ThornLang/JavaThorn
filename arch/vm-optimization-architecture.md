# VM Optimization Architecture

## Overview

This document details the architecture for optimizing ThornLang's virtual machine to achieve 2-3x performance improvement over the current implementation.

## Current VM Architecture Issues

1. **Inefficient Dispatch Loop**
   ```java
   // Current: Expensive switch for every instruction
   switch(instruction.opcode) {
       case ADD: // overhead
       case SUB: // overhead
   }
   ```

2. **Stack-Heavy Operations**
   - Every operation pushes/pops from stack
   - No register reuse
   - Excessive memory traffic

3. **Generic Instructions**
   - No fast paths for common patterns
   - Every ADD handles all types
   - No specialization

## Optimization Architecture

### 1. Superinstruction Design

**Concept**: Combine frequently occurring instruction sequences into single opcodes.

```java
// Before: 4 instructions, 4 dispatches
LOAD_CONST 0  // Push constant
LOAD_LOCAL 1  // Push local variable  
ADD           // Pop 2, add, push 1
STORE_LOCAL 2 // Pop and store

// After: 1 superinstruction, 1 dispatch
ADD_CONST_TO_LOCAL 0, 1, 2  // local[2] = const[0] + local[1]
```

**Implementation**:
```java
public enum OpCode {
    // Existing opcodes
    ADD, SUB, MUL, DIV,
    
    // Superinstructions
    ADD_CONST_TO_LOCAL,      // const + local → local
    ADD_LOCALS,              // local + local → local
    LOAD_CONST_ADD,          // load const, add to stack top
    CMP_JUMP_IF_FALSE,       // compare and conditional jump
    INCREMENT_LOCAL,         // local++ in place
    LOAD_LOCAL_CALL,         // load local and call it
    
    // Specialized numeric ops
    ADD_INT,                 // Fast integer addition
    ADD_DOUBLE,              // Fast double addition
    MUL_INT_CONST,          // Multiply by constant (strength reduction)
}
```

### 2. Enhanced Instruction Format

```java
public class Instruction {
    public final OpCode opcode;
    public final int arg1;       // First operand
    public final int arg2;       // Second operand  
    public final int arg3;       // Third operand (for superinstructions)
    public final Object data;    // Constant data
    
    // Computed goto support (future)
    public MethodHandle handler;
}
```

### 3. Optimized VM Execute Loop

```java
public class OptimizedVM extends ThornVM {
    // Register file for top stack values
    private double reg0, reg1, reg2, reg3;
    private Object obj0, obj1;
    private boolean regValid = false;
    
    @Override
    protected void executeOptimized(CompilationResult result) {
        Instruction[] code = result.getInstructions();
        int ip = 0;
        
        while (ip < code.length) {
            Instruction inst = code[ip++];
            
            // Fast dispatch for common cases
            switch (inst.opcode) {
                case ADD_INT:
                    // Use registers if valid
                    if (regValid && stackSize >= 2) {
                        reg0 = reg1 + reg2;
                        regValid = true;
                    } else {
                        // Fall back to stack
                        int b = (int) pop();
                        int a = (int) pop();
                        push(a + b);
                    }
                    break;
                    
                case ADD_CONST_TO_LOCAL:
                    double constant = constants.get(inst.arg1);
                    double local = (double) locals[inst.arg2];
                    locals[inst.arg3] = constant + local;
                    break;
                    
                case CMP_JUMP_IF_FALSE:
                    if (!isTruthy(pop())) {
                        ip = inst.arg1;
                    }
                    break;
                    
                case INCREMENT_LOCAL:
                    locals[inst.arg1] = ((double) locals[inst.arg1]) + 1.0;
                    break;
                    
                // ... more superinstructions
            }
        }
    }
}
```

### 4. Pattern-Based Optimization in Compiler

```java
public class OptimizingCompiler extends SimpleCompiler {
    
    @Override
    protected void compileExpression(Expr expr) {
        // Detect patterns and emit superinstructions
        if (expr instanceof Expr.Binary) {
            Expr.Binary binary = (Expr.Binary) expr;
            
            // Pattern: const + local
            if (binary.operator.type == TokenType.PLUS &&
                binary.left instanceof Expr.Literal &&
                binary.right instanceof Expr.Variable) {
                
                int constIndex = addConstant(((Expr.Literal) binary.left).value);
                int localIndex = getLocalIndex(((Expr.Variable) binary.right).name);
                
                // Emit superinstruction
                emit(OpCode.LOAD_CONST_ADD, constIndex, localIndex);
                return;
            }
            
            // Pattern: numeric operations
            if (isNumericOp(binary) && inferredType == Type.NUMBER) {
                compileNumericBinary(binary);
                return;
            }
        }
        
        // Fall back to standard compilation
        super.compileExpression(expr);
    }
    
    private void optimizeLoop(Stmt.While stmt) {
        // Detect common loop patterns
        if (isCountingLoop(stmt)) {
            emitOptimizedCountingLoop(stmt);
        } else if (isIteratorLoop(stmt)) {
            emitOptimizedIteratorLoop(stmt);
        } else {
            compileWhileStatement(stmt);
        }
    }
}
```

### 5. Profiling and Adaptive Optimization

```java
public class ProfilingVM extends OptimizedVM {
    private final Map<Integer, Integer> instructionCounts = new HashMap<>();
    private final Map<Integer, TypeProfile> typeProfiles = new HashMap<>();
    
    @Override
    protected void profiledExecute(Instruction inst, int ip) {
        // Count instruction frequency
        instructionCounts.merge(ip, 1, Integer::sum);
        
        // Profile types for polymorphic operations
        if (inst.opcode == OpCode.ADD) {
            TypeProfile profile = typeProfiles.computeIfAbsent(ip, k -> new TypeProfile());
            profile.recordTypes(peek(1), peek(0));
            
            // Specialize if monomorphic
            if (profile.isMonomorphic() && profile.getType() == Type.NUMBER) {
                // Rewrite to ADD_DOUBLE
                code[ip] = new Instruction(OpCode.ADD_DOUBLE, inst.arg1, inst.arg2);
            }
        }
        
        super.execute(inst);
    }
}
```

### 6. Memory and Cache Optimization

```java
public class CacheOptimizedVM extends OptimizedVM {
    // Instruction cache line alignment
    private static final int CACHE_LINE_SIZE = 64;
    
    // Object pools to reduce allocation
    private final Stack<CallFrame> framePool = new Stack<>();
    private final Stack<double[]> localsPool = new Stack<>();
    
    @Override
    protected CallFrame allocateFrame(int localCount) {
        CallFrame frame = framePool.isEmpty() ? new CallFrame() : framePool.pop();
        frame.locals = localsPool.isEmpty() ? new double[localCount] : localsPool.pop();
        return frame;
    }
    
    @Override
    protected void releaseFrame(CallFrame frame) {
        if (frame.locals.length <= 16) {  // Pool small arrays
            Arrays.fill(frame.locals, 0);
            localsPool.push(frame.locals);
        }
        framePool.push(frame);
    }
}
```

## Benchmarking Strategy

```java
public class VMBenchmark {
    public static void main(String[] args) {
        // Benchmark categories
        benchmarkArithmetic();      // Tests numeric superinstructions
        benchmarkFunctionCalls();   // Tests call optimization
        benchmarkLoops();          // Tests loop optimization
        benchmarkObjectAccess();   // Tests property access
        
        // Compare implementations
        compareVMs(new ThornVM(), new OptimizedVM());
    }
}
```

## Expected Performance Gains

| Operation | Current | Optimized | Improvement |
|-----------|---------|-----------|-------------|
| Arithmetic loops | 100ms | 40ms | 2.5x |
| Function calls | 50ms | 30ms | 1.7x |
| Property access | 80ms | 35ms | 2.3x |
| Overall | 250ms | 125ms | 2.0x |

## Implementation Phases

1. **Phase 1**: Superinstructions for arithmetic (1 week)
2. **Phase 2**: Optimized dispatch loop (3 days)
3. **Phase 3**: Pattern detection in compiler (1 week)
4. **Phase 4**: Profiling and specialization (1 week)

## Migration Strategy

1. Add `-Dvm.optimized=true` flag
2. Run side-by-side testing
3. Make optimized VM default once stable
4. Remove old VM code