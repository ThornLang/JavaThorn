#!/usr/bin/env python3

import re
import glob

# Files that need visitThrowStmt method added
files_to_fix = [
    "src/com/thorn/BranchOptimizationPass.java",
    "src/com/thorn/ConstantFoldingPass.java", 
    "src/com/thorn/DeadCodeEliminationPass.java",
    "src/com/thorn/DeadCodeEliminator.java",
    "src/com/thorn/FunctionInliningPass.java",
    "src/com/thorn/LoopOptimizationPass.java",
    "src/com/thorn/TailCallAnalyzer.java",
    "src/com/thorn/TailCallOptimizationPass.java",
    "src/com/thorn/UnreachableCodeEliminationPass.java"
]

def add_visitThrowStmt_to_anonymous_classes(file_path):
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Pattern to find anonymous Stmt.Visitor implementations that have visitReturnStmt
    pattern = r'(new Stmt\.Visitor<[^>]+>\(\)\s*\{[^}]*?@Override[^}]*?visitReturnStmt\([^}]*?\}[^}]*?)(\s*\}[^;]*?;)'
    
    def replacement(match):
        visitor_content = match.group(1)
        closing = match.group(2)
        
        # Check if visitThrowStmt is already there
        if 'visitThrowStmt' in visitor_content:
            return match.group(0)
        
        # Determine return type from visitReturnStmt
        if 'return stmt;' in visitor_content:
            throw_method = '''
                @Override public Stmt visitThrowStmt(Stmt.Throw stmt) { return stmt; }'''
        elif 'return null;' in visitor_content:
            throw_method = '''
                @Override public Void visitThrowStmt(Stmt.Throw stmt) { return null; }'''
        elif 'return false;' in visitor_content or 'return true;' in visitor_content:
            throw_method = '''
                @Override public Boolean visitThrowStmt(Stmt.Throw stmt) { return false; }'''
        else:
            throw_method = '''
                @Override public Object visitThrowStmt(Stmt.Throw stmt) { return null; }'''
        
        return visitor_content + throw_method + closing
    
    new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)
    
    if new_content != content:
        with open(file_path, 'w') as f:
            f.write(new_content)
        print(f"Updated {file_path}")
    else:
        print(f"No changes needed for {file_path}")

# Apply fixes to all files
for file_path in files_to_fix:
    try:
        add_visitThrowStmt_to_anonymous_classes(file_path)
    except Exception as e:
        print(f"Error processing {file_path}: {e}")