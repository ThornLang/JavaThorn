#!/bin/bash

echo "==============================="
echo "  Thorn Language Benchmarks"
echo "==============================="

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Compile Thorn
cd "$PROJECT_ROOT"
javac -d . src/com/thorn/*.java 2>/dev/null

# Set paths
BENCHMARKS_DIR="$PROJECT_ROOT/benchmarks"

echo ""
echo "🔢 Fibonacci(30):"
echo -n "Thorn:      "
timeout 10s java com.thorn.Thorn "$BENCHMARKS_DIR/thorn/fibonacci.thorn" 2>/dev/null | grep Time || echo "Timeout"
echo -n "Python:     "
timeout 10s python3 "$BENCHMARKS_DIR/python/fibonacci.py" 2>/dev/null | grep Time || echo "Timeout" 
echo -n "JavaScript: "
timeout 10s node "$BENCHMARKS_DIR/javascript/fibonacci.js" 2>/dev/null | grep Time || echo "Timeout"
echo -n "Ruby:       "
timeout 10s ruby "$BENCHMARKS_DIR/ruby/fibonacci.rb" 2>/dev/null | grep Time || echo "Timeout"

echo ""
echo "📝 String Ops(1000):"
echo -n "Thorn:      "
timeout 10s java com.thorn.Thorn "$BENCHMARKS_DIR/thorn/string_ops.thorn" 2>/dev/null | grep Time || echo "Timeout"
echo -n "Python:     "
timeout 10s python3 "$BENCHMARKS_DIR/python/string_ops.py" 2>/dev/null | grep Time || echo "Timeout"
echo -n "JavaScript: "
timeout 10s node "$BENCHMARKS_DIR/javascript/string_ops.js" 2>/dev/null | grep Time || echo "Timeout"
echo -n "Ruby:       "
timeout 10s ruby "$BENCHMARKS_DIR/ruby/string_ops.rb" 2>/dev/null | grep Time || echo "Timeout"

echo ""
echo "📊 Array Ops(1000):"
echo -n "Thorn:      "
timeout 10s java com.thorn.Thorn "$BENCHMARKS_DIR/thorn/array_ops.thorn" 2>/dev/null | grep Time || echo "Timeout"
echo -n "Python:     "
timeout 10s python3 "$BENCHMARKS_DIR/python/array_ops.py" 2>/dev/null | grep Time || echo "Timeout"
echo -n "JavaScript: "
timeout 10s node "$BENCHMARKS_DIR/javascript/array_ops.js" 2>/dev/null | grep Time || echo "Timeout"
echo -n "Ruby:       "
timeout 10s ruby "$BENCHMARKS_DIR/ruby/array_ops.rb" 2>/dev/null | grep Time || echo "Timeout"