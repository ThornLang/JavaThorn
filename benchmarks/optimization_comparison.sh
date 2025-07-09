#!/bin/bash

echo "=========================================="
echo "  Thorn Optimization Level Comparison"
echo "=========================================="

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Change to project root
cd "$PROJECT_ROOT"

# Compile Thorn
javac -d . src/com/thorn/*.java src/com/thorn/vm/*.java 2>/dev/null

# Test programs
declare -a TESTS=("fibonacci" "string_ops" "array_ops")

# Run each test with different optimization levels
for test in "${TESTS[@]}"; do
    echo ""
    echo "🔢 Test: $test.thorn"
    echo "------------------------"
    
    # O0 - No optimization
    echo -n "O0 (no opt):    "
    java com.thorn.Thorn "benchmarks/thorn/$test.thorn" 2>&1 | grep Time || echo "Failed"
    
    # O1 - Basic optimization
    echo -n "O1 (basic):     "
    java -Doptimize.thorn.level=1 com.thorn.Thorn "benchmarks/thorn/$test.thorn" 2>&1 | grep Time || echo "Failed"
    
    # O2 - Standard optimization
    echo -n "O2 (standard):  "
    java -Doptimize.thorn.level=2 com.thorn.Thorn "benchmarks/thorn/$test.thorn" 2>&1 | grep Time || echo "Failed"
    
    # O3 - Aggressive optimization
    echo -n "O3 (aggressive):"
    java -Doptimize.thorn.level=3 com.thorn.Thorn "benchmarks/thorn/$test.thorn" 2>&1 | grep Time || echo "Failed"
done

echo ""
echo "=========================================="
echo "  Optimization Impact Summary"
echo "=========================================="
echo "O0: No optimization"
echo "O1: Constant folding, dead code, branch optimization"
echo "O2: + CSE, function inlining, loop optimization"
echo "O3: All optimizations enabled"