#!/bin/bash

# Build script for ThornLang
# Compiles the project in the correct order to handle dependencies

set -e  # Exit on any error

echo "Building ThornLang..."

# Clean previous builds
echo "Cleaning previous builds..."
rm -rf com/

# Create output directory
mkdir -p com/thorn/vm

# Compile core language classes first (required by VM)
echo "Compiling core language classes..."
javac -d . src/com/thorn/TokenType.java src/com/thorn/Token.java src/com/thorn/Expr.java src/com/thorn/Stmt.java

# Compile ThornResult standalone (no dependencies)
echo "Compiling ThornResult..."
javac -d . src/com/thorn/ThornResult.java

# Compile VM package
echo "Compiling VM package..."
javac -d . src/com/thorn/vm/*.java

# Compile remaining main package classes
echo "Compiling main package..."
javac -d . src/com/thorn/*.java

echo "Build completed successfully!"
echo "Run with: java com.thorn.Thorn [--ast] [--vm] [script]"