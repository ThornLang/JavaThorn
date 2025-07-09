#!/bin/bash

echo "=========================================="
echo "  ThornLang Performance Comparison"
echo "=========================================="

# Change to project root
cd "$(dirname "$0")/.."

# Compile if needed
javac -d . src/com/thorn/*.java src/com/thorn/vm/*.java 2>/dev/null

echo ""
echo "🔢 Regular Fibonacci(30) vs Tail-Recursive:"
echo "------------------------------------------------"

# Regular fibonacci with tree-walking interpreter
echo -n "Tree-walk (no opt):     "
java com.thorn.Thorn benchmarks/thorn/fibonacci.thorn 2>&1 | grep Time

echo -n "Tree-walk (O2):         "
java -Doptimize.thorn.level=2 com.thorn.Thorn benchmarks/thorn/fibonacci.thorn 2>&1 | grep Time

# Regular fibonacci with VM
echo -n "VM (no opt):            "
java com.thorn.Thorn --vm benchmarks/thorn/fibonacci.thorn 2>&1 | grep Time

echo -n "VM (superinstructions): "
java -Doptimize.thorn.level=2 com.thorn.Thorn --vm benchmarks/thorn/fibonacci.thorn 2>&1 | grep Time

# Tail-recursive fibonacci
echo -n "Tail-recursive (O2):    "
java -Doptimize.thorn.level=2 com.thorn.Thorn benchmarks/thorn/fibonacci_tail.thorn 2>&1 | grep Time

echo ""
echo "📊 Arithmetic Operations (VM mode):"
echo "------------------------------------------------"

# Without superinstructions
echo "Without superinstructions:"
java com.thorn.Thorn --vm benchmarks/thorn/arithmetic_intensive.thorn 2>&1 | grep -E "Loop increment|Local additions|Constant additions"

echo ""
echo "With superinstructions (O2):"
java -Doptimize.thorn.level=2 com.thorn.Thorn --vm benchmarks/thorn/arithmetic_intensive.thorn 2>&1 | grep -E "Loop increment|Local additions|Constant additions"

echo ""
echo "=========================================="
echo "Expected improvements:"
echo "- Fibonacci: 10x with tail call optimization"
echo "- Arithmetic: 2-3x with superinstructions"
echo "- VM should be faster than tree-walk"
echo "=========================================="