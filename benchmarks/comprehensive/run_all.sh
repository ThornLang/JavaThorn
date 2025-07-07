#!/bin/bash

echo "Running comprehensive benchmarks..."
echo "=================================="

# Function to run a benchmark and format output
run_benchmark() {
    local name="$1"
    local lang="$2"
    local file="$3"
    local command="$4"
    
    echo
    echo "--- $name ($lang) ---"
    cd "$lang"
    eval "$command" 2>&1
    cd ..
}

# Recursive tree traversal benchmarks
echo
echo "RECURSIVE TREE TRAVERSAL:"
run_benchmark "Recursive Tree" "thorn" "recursive_tree.thorn" "java -cp ../../../out/production/Thorn com.thorn.Thorn recursive_tree.thorn"
run_benchmark "Recursive Tree" "python" "recursive_tree.py" "python3 recursive_tree.py"
run_benchmark "Recursive Tree" "javascript" "recursive_tree.js" "node recursive_tree.js"

# Hot variable access benchmarks
echo
echo "HOT VARIABLE ACCESS:"
run_benchmark "Hot Variables" "thorn" "hot_variables.thorn" "java -cp ../../../out/production/Thorn com.thorn.Thorn hot_variables.thorn"
run_benchmark "Hot Variables" "python" "hot_variables.py" "python3 hot_variables.py"
run_benchmark "Hot Variables" "javascript" "hot_variables.js" "node hot_variables.js"

# Arithmetic-heavy benchmarks
echo
echo "ARITHMETIC HEAVY (MANDELBROT):"
run_benchmark "Arithmetic Heavy" "thorn" "arithmetic_heavy.thorn" "java -cp ../../../out/production/Thorn com.thorn.Thorn arithmetic_heavy.thorn"
run_benchmark "Arithmetic Heavy" "python" "arithmetic_heavy.py" "python3 arithmetic_heavy.py"
run_benchmark "Arithmetic Heavy" "javascript" "arithmetic_heavy.js" "node arithmetic_heavy.js"

# Mixed workload benchmarks
echo
echo "MIXED WORKLOAD:"
run_benchmark "Mixed Workload" "thorn" "mixed_workload.thorn" "java -cp ../../../out/production/Thorn com.thorn.Thorn mixed_workload.thorn"
run_benchmark "Mixed Workload" "python" "mixed_workload.py" "python3 mixed_workload.py"
run_benchmark "Mixed Workload" "javascript" "mixed_workload.js" "node mixed_workload.js"

echo
echo "Comprehensive benchmarks complete!"