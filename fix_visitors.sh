#!/bin/bash

# Files that need the visitor methods added
files=(
    "src/com/thorn/BranchOptimizationPass.java"
    "src/com/thorn/DeadCodeEliminator.java" 
    "src/com/thorn/FunctionInliningPass.java"
    "src/com/thorn/LoopOptimizationPass.java"
    "src/com/thorn/TailCallAnalyzer.java"
    "src/com/thorn/TailCallOptimizationPass.java"
    "src/com/thorn/UnreachableCodeEliminationPass.java"
)

# Add missing visitor methods to each file
for file in "${files[@]}"; do
    echo "Processing $file..."
    
    # Add visitTryCatchStmt and visitThrowStmt methods before the closing brace of anonymous visitors
    # This is a simple pattern - look for }) and add the methods before it
    
    # For each new Stmt.Visitor<...>() { ... } pattern, add the methods
    sed -i.bak 's/visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {[^}]*}/&\n                @Override\n                public Object visitTryCatchStmt(Stmt.TryCatch stmt) {\n                    return stmt;\n                }\n                @Override\n                public Object visitThrowStmt(Stmt.Throw stmt) {\n                    return stmt;\n                }/g' "$file"
done

echo "Fixed visitor methods in optimization passes"