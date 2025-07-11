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
mkdir -p com/thorn/stdlib

# Compile all .java files in dependency order, letting javac resolve dependencies
echo "Compiling all source files..."
javac -d . -cp . $(find src -name "*.java" | sort)

echo "Build completed successfully!"
echo "Run with: java com.thorn.Thorn [--ast] [--vm] [script]"