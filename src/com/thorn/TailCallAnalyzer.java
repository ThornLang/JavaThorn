package com.thorn;

import java.util.*;

/**
 * Analyzes functions to detect tail calls for optimization.
 * A tail call is a function call that is the last operation before returning.
 */
public class TailCallAnalyzer {
    
    /**
     * Information about a potential tail call.
     */
    public static class TailCallInfo {
        public final boolean isTailCall;
        public final String targetFunction;
        public final boolean isSelfRecursive;
        public final List<Expr> arguments;
        
        public TailCallInfo(boolean isTailCall, String targetFunction, 
                          boolean isSelfRecursive, List<Expr> arguments) {
            this.isTailCall = isTailCall;
            this.targetFunction = targetFunction;
            this.isSelfRecursive = isSelfRecursive;
            this.arguments = arguments;
        }
        
        public static TailCallInfo notTailCall() {
            return new TailCallInfo(false, null, false, null);
        }
    }
    
    /**
     * Analyzes a function to determine if it contains tail calls.
     */
    public boolean hasTailRecursion(Stmt.Function function) {
        String functionName = function.name.lexeme;
        
        for (Stmt stmt : function.body) {
            if (containsTailCall(stmt, functionName)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a statement contains a tail call.
     */
    private boolean containsTailCall(Stmt stmt, String currentFunction) {
        return stmt.accept(new Stmt.Visitor<Boolean>() {
            @Override
            public Boolean visitReturnStmt(Stmt.Return stmt) {
                if (stmt.value == null) return false;
                
                TailCallInfo info = analyzeFunctionCall(stmt, currentFunction);
                return info.isTailCall && info.isSelfRecursive;
            }
            
            @Override
            public Boolean visitThrowStmt(Stmt.Throw stmt) {
                return false; // Throw statements don't contain tail calls
            }
            
            @Override
            public Boolean visitIfStmt(Stmt.If stmt) {
                boolean thenHasTail = containsTailCall(stmt.thenBranch, currentFunction);
                boolean elseHasTail = stmt.elseBranch != null && 
                                    containsTailCall(stmt.elseBranch, currentFunction);
                return thenHasTail || elseHasTail;
            }
            
            @Override
            public Boolean visitBlockStmt(Stmt.Block stmt) {
                for (Stmt s : stmt.statements) {
                    if (containsTailCall(s, currentFunction)) {
                        return true;
                    }
                }
                return false;
            }
            
            @Override
            public Boolean visitWhileStmt(Stmt.While stmt) {
                return containsTailCall(stmt.body, currentFunction);
            }
            
            // Other statement types cannot contain tail calls
            @Override
            public Boolean visitExpressionStmt(Stmt.Expression stmt) { return false; }
            @Override
            public Boolean visitVarStmt(Stmt.Var stmt) { return false; }
            @Override
            public Boolean visitForStmt(Stmt.For stmt) { return false; }
            @Override
            public Boolean visitFunctionStmt(Stmt.Function stmt) { return false; }
            @Override
            public Boolean visitClassStmt(Stmt.Class stmt) { return false; }
            @Override
            public Boolean visitImportStmt(Stmt.Import stmt) { return false; }
            @Override
            public Boolean visitExportStmt(Stmt.Export stmt) { return false; }
            @Override
            public Boolean visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) { return false; }
            @Override
            public Boolean visitTypeAliasStmt(Stmt.TypeAlias stmt) { return false; }
        });
    }
    
    /**
     * Analyzes a return statement to check if it contains a tail call.
     */
    public TailCallInfo analyzeFunctionCall(Stmt.Return stmt, String currentFunction) {
        if (stmt.value instanceof Expr.Call) {
            Expr.Call call = (Expr.Call) stmt.value;
            
            if (call.callee instanceof Expr.Variable) {
                String targetName = ((Expr.Variable) call.callee).name.lexeme;
                
                return new TailCallInfo(
                    true,
                    targetName,
                    targetName.equals(currentFunction),
                    call.arguments
                );
            }
        }
        return TailCallInfo.notTailCall();
    }
    
    /**
     * Gets all tail calls in a function.
     */
    public List<Stmt.Return> getTailCalls(Stmt.Function function) {
        List<Stmt.Return> tailCalls = new ArrayList<>();
        String functionName = function.name.lexeme;
        
        collectTailCalls(function.body, functionName, tailCalls);
        
        return tailCalls;
    }
    
    private void collectTailCalls(List<Stmt> statements, String functionName, 
                                 List<Stmt.Return> tailCalls) {
        for (Stmt stmt : statements) {
            collectTailCallsFromStatement(stmt, functionName, tailCalls);
        }
    }
    
    private void collectTailCallsFromStatement(Stmt stmt, String functionName, 
                                              List<Stmt.Return> tailCalls) {
        stmt.accept(new Stmt.Visitor<Void>() {
            @Override
            public Void visitReturnStmt(Stmt.Return stmt) {
                TailCallInfo info = analyzeFunctionCall(stmt, functionName);
                if (info.isTailCall && info.isSelfRecursive) {
                    tailCalls.add(stmt);
                }
                return null;
            }
            
            @Override
            public Void visitThrowStmt(Stmt.Throw stmt) {
                return null; // Throw statements don't contain tail calls
            }
            
            @Override
            public Void visitIfStmt(Stmt.If stmt) {
                collectTailCallsFromStatement(stmt.thenBranch, functionName, tailCalls);
                if (stmt.elseBranch != null) {
                    collectTailCallsFromStatement(stmt.elseBranch, functionName, tailCalls);
                }
                return null;
            }
            
            @Override
            public Void visitBlockStmt(Stmt.Block stmt) {
                collectTailCalls(stmt.statements, functionName, tailCalls);
                return null;
            }
            
            @Override
            public Void visitWhileStmt(Stmt.While stmt) {
                collectTailCallsFromStatement(stmt.body, functionName, tailCalls);
                return null;
            }
            
            // Other statement types are ignored
            @Override
            public Void visitExpressionStmt(Stmt.Expression stmt) { return null; }
            @Override
            public Void visitVarStmt(Stmt.Var stmt) { return null; }
            @Override
            public Void visitForStmt(Stmt.For stmt) { return null; }
            @Override
            public Void visitFunctionStmt(Stmt.Function stmt) { return null; }
            @Override
            public Void visitClassStmt(Stmt.Class stmt) { return null; }
            @Override
            public Void visitImportStmt(Stmt.Import stmt) { return null; }
            @Override
            public Void visitExportStmt(Stmt.Export stmt) { return null; }
            @Override
            public Void visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) { return null; }
            @Override
            public Void visitTypeAliasStmt(Stmt.TypeAlias stmt) { return null; }
        });
    }
}