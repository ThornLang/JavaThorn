# Immediate Performance Optimization Tasks

## Priority 1: VM Superinstructions (1 week)

### Task 1.1: Add Superinstruction Opcodes
- [ ] Add new opcodes to `OpCode.java`:
  - `ADD_LOCALS` - Add two local variables
  - `ADD_CONST_TO_LOCAL` - Add constant to local
  - `CMP_JUMP_IF_FALSE` - Compare and jump in one op
  - `INCREMENT_LOCAL` - In-place increment
  - `LOAD_LOCAL_LOAD_LOCAL` - Load two locals at once

### Task 1.2: Update Compiler Pattern Detection
- [ ] Modify `SimpleCompiler.java` to detect patterns:
  ```java
  // Detect: local1 + local2
  if (isLocalPlusLocal(expr)) {
      emit(OpCode.ADD_LOCALS, local1Index, local2Index);
  }
  ```

### Task 1.3: Implement Superinstruction Execution
- [ ] Update `ThornVM.execute()` with new cases
- [ ] Add fast paths that skip stack operations
- [ ] Benchmark each superinstruction

### Expected Impact
- 30-40% faster arithmetic operations
- 25% overall VM performance improvement

## Priority 2: Simple Tail Call Optimization (1 week)

### Task 2.1: Tail Call Detection
- [ ] Add `TailCallAnalyzer.java`:
  - Detect self-recursive tail calls
  - Mark functions as tail-recursive
  - Generate optimization hints

### Task 2.2: AST Transformation
- [ ] Create `TailCallOptimizationPass.java`:
  - Transform tail recursion to loops
  - Handle parameter updates correctly
  - Preserve semantics

### Task 2.3: Benchmark Tail-Recursive Functions
- [ ] Create tail-recursive fibonacci
- [ ] Compare performance (expect 100x improvement)
- [ ] Add to benchmark suite

### Expected Impact
- 100-500x faster for recursive algorithms
- Makes functional programming patterns viable

## Priority 3: Interpreter Fast Paths (3 days)

### Task 3.1: Numeric Operation Fast Paths
- [ ] In `Interpreter.visitBinaryExpr()`:
  ```java
  // Fast path for double arithmetic
  if (left instanceof Double && right instanceof Double) {
      double l = (Double) left;
      double r = (Double) right;
      switch (expr.operator.type) {
          case PLUS: return l + r;  // Skip type checking
          case MINUS: return l - r;
          case STAR: return l * r;
          case SLASH: return r != 0 ? l / r : handleDivByZero();
      }
  }
  ```

### Task 3.2: Variable Access Caching
- [ ] Cache frequently accessed variables
- [ ] Add inline caches for property access
- [ ] Reduce environment lookup overhead

### Expected Impact
- 20-30% faster arithmetic
- 15% overall interpreter improvement

## Testing Strategy

### Performance Test Suite
```thorn
// superinstruction_test.thorn
$ benchmark_arithmetic() {
    sum = 0;
    for (i = 0; i < 1000000; i = i + 1) {
        sum = sum + i;  // Should use INCREMENT_LOCAL
    }
    return sum;
}

// tail_recursion_test.thorn  
$ factorial_tail(n, acc) {
    if (n <= 1) return acc;
    return factorial_tail(n - 1, n * acc);  // Tail call
}

$ fib_tail(n, a, b) {
    if (n == 0) return a;
    return fib_tail(n - 1, b, a + b);  // Tail call
}
```

### Benchmark Harness
```java
public class PerformanceBenchmark {
    public static void main(String[] args) {
        // Compare implementations
        benchmarkWithOptimization("None", "-Doptimize.level=0");
        benchmarkWithOptimization("Current", "-Doptimize.level=3");
        benchmarkWithOptimization("VM+Super", "-Dvm.superinstructions=true");
        benchmarkWithOptimization("VM+TCO", "-Dvm.tco=true");
    }
}
```

## Success Criteria

1. **Fibonacci(30)**: 250ms → 25ms (10x faster)
2. **Arithmetic loops**: 2x faster minimum
3. **VM faster than interpreter**: For all benchmarks
4. **No regression**: Existing code runs correctly

## Implementation Order

1. **Day 1-2**: Superinstruction opcodes and compiler
2. **Day 3-4**: VM execution of superinstructions
3. **Day 5-6**: Tail call detection and transformation
4. **Day 7**: Benchmarking and tuning

## Code Locations

- VM: `/src/com/thorn/vm/ThornVM.java`
- Compiler: `/src/com/thorn/vm/SimpleCompiler.java`
- Opcodes: `/src/com/thorn/vm/OpCode.java`
- New pass: `/src/com/thorn/TailCallOptimizationPass.java`
- Benchmarks: `/benchmarks/performance/`