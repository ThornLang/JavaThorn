# ThornLang Performance Optimization Plan

## Analysis of Current Performance Issues

### 1. Why Current Optimizations Have Limited Impact

The fibonacci benchmark shows minimal improvement because:
- **Recursive overhead dominates**: Function call overhead is the primary cost
- **No tail call optimization**: Each recursive call creates a new stack frame
- **No memoization**: Recalculates same values repeatedly
- **Tree-walking interpreter overhead**: Each AST node visit has overhead

Current optimizations (CSE, constant folding, etc.) operate at the AST level but don't address fundamental execution model inefficiencies.

### 2. Major Performance Bottlenecks

1. **Interpreter Overhead** (40-60% of execution time)
   - Tree-walking requires visitor pattern dispatch for every node
   - No instruction caching or hot path optimization
   - Environment lookups for every variable access

2. **Function Call Overhead** (20-30% for recursive code)
   - Stack frame allocation for each call
   - Parameter copying
   - No inlining of recursive functions

3. **Object Allocation** (15-25% for string/array operations)
   - Creates new objects for every operation
   - No object pooling or reuse
   - Garbage collection pressure

## High-Impact Optimization Strategies

### Phase 1: Bytecode VM Optimization (2-3x speedup potential)

**Goal**: Make VM the default execution mode with competitive performance

1. **Register-based VM improvements**
   - Implement proper register allocation
   - Add specialized opcodes for common patterns
   - Inline caching for method dispatch

2. **Superinstructions**
   - Combine common instruction sequences
   - Examples: LOAD_CONST + ADD, LOAD_LOCAL + CALL
   - Reduces dispatch overhead by 30-40%

3. **Threaded Code Dispatch**
   - Replace switch-based dispatch with computed gotos (via method handles)
   - Improves branch prediction
   - 15-20% speedup for interpreter loop

### Phase 2: Tail Call Optimization (10x for recursive code)

**Goal**: Transform recursive functions into loops

1. **Direct tail recursion**
   ```thorn
   $ fib_tail(n, a, b) {
       if (n == 0) return a;
       return fib_tail(n - 1, b, a + b);  // Optimize to loop
   }
   ```

2. **Mutual tail recursion**
   - Handle mutually recursive functions
   - Transform to state machine

3. **Accumulator introduction**
   - Automatically transform recursive functions to tail-recursive form
   - Critical for functional programming patterns

### Phase 3: JIT Compilation Foundation (5-10x potential)

**Goal**: Compile hot code paths to JVM bytecode

1. **Profiling Infrastructure**
   - Track hot functions and loops
   - Identify monomorphic call sites
   - Build type profiles

2. **AST to JVM Bytecode Compiler**
   - Start with simple functions
   - Use ASM library for bytecode generation
   - Gradually expand coverage

3. **Deoptimization Support**
   - Handle dynamic language features
   - Fall back to interpreter when needed

## Implementation Priority

### Immediate (1-2 weeks): VM Performance
1. **Superinstructions** - Biggest bang for buck
2. **Better register allocation** - Reduce stack operations
3. **Specialized numeric opcodes** - Fast path for arithmetic

### Short-term (3-4 weeks): Tail Call Optimization
1. **Simple tail recursion** - Transform to loops
2. **Benchmarks** - Show 10x improvement on recursive code
3. **Documentation** - Explain optimization conditions

### Medium-term (2-3 months): JIT Foundation
1. **Profiling** - Identify hot code
2. **Simple JIT** - Compile arithmetic-heavy functions
3. **Inline caching** - Speed up method dispatch

## Expected Performance Gains

| Optimization | Fibonacci | String Ops | Array Ops | Overall |
|-------------|-----------|------------|-----------|---------|
| Current     | 10%       | 50%        | 50%       | 25%     |
| + VM Opts   | 40%       | 60%        | 70%       | 50%     |
| + Tail Call | 90%       | 60%        | 70%       | 70%     |
| + JIT       | 95%       | 80%        | 85%       | 85%     |

## Success Metrics

1. **Fibonacci(30)**: From 250ms → 25ms (10x improvement)
2. **VM as default**: VM faster than tree-walker for all benchmarks
3. **Competitive with Python**: Within 2x of CPython performance

## Next Steps

1. Create `VMOptimizations.java` with superinstruction support
2. Implement tail call detection in `Parser.java`
3. Add benchmark suite to track improvements
4. Document optimization flags and tuning