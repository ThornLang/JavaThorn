package com.thorn;

import java.util.*;

/**
 * Removes unused variables, functions, and unreachable code.
 */
public class DeadCodeEliminationPass extends OptimizationPass {
    
    private int variablesRemoved = 0;
    private int functionsRemoved = 0;
    private int statementsRemoved = 0;
    
    @Override
    public String getName() {
        return "dead-code-elimination";
    }
    
    @Override
    public PassType getType() {
        return PassType.CLEANUP;
    }
    
    @Override
    public OptimizationLevel getMinimumLevel() {
        return OptimizationLevel.O1;
    }
    
    @Override
    public List<Stmt> optimize(List<Stmt> statements, OptimizationContext context) {
        if (context.isDebugMode()) {
            System.out.println("=== Dead Code Elimination Pass ===");
        }
        
        variablesRemoved = 0;
        functionsRemoved = 0;
        statementsRemoved = 0;
        
        // First pass: collect all used symbols
        UsageCollector usage = new UsageCollector();
        usage.collectUsage(statements);
        
        // Second pass: remove unused code
        List<Stmt> optimized = removeDeadCode(statements, usage, context);
        
        if (context.isDebugMode()) {
            System.out.println("  Variables removed: " + variablesRemoved);
            System.out.println("  Functions removed: " + functionsRemoved);
            System.out.println("  Statements removed: " + statementsRemoved);
        }
        
        return optimized;
    }
    
    private List<Stmt> removeDeadCode(List<Stmt> statements, UsageCollector usage, 
                                     OptimizationContext context) {
        List<Stmt> result = new ArrayList<>();
        
        for (Stmt stmt : statements) {
            if (stmt instanceof Stmt.Var) {
                Stmt.Var varStmt = (Stmt.Var) stmt;
                if (!usage.isUsed(varStmt.name.lexeme) && !hasSideEffects(varStmt.initializer)) {
                    variablesRemoved++;
                    continue; // Skip unused variable
                }
            } else if (stmt instanceof Stmt.Function) {
                Stmt.Function func = (Stmt.Function) stmt;
                if (!usage.isUsed(func.name.lexeme) && !isExported(func)) {
                    functionsRemoved++;
                    continue; // Skip unused function
                }
            } else if (stmt instanceof Stmt.Expression) {
                Stmt.Expression expr = (Stmt.Expression) stmt;
                if (!hasSideEffects(expr.expression)) {
                    statementsRemoved++;
                    continue; // Skip expression without side effects
                }
            } else if (stmt instanceof Stmt.Block) {
                Stmt.Block block = (Stmt.Block) stmt;
                List<Stmt> optimizedBlock = removeDeadCode(block.statements, usage, context);
                if (!optimizedBlock.isEmpty()) {
                    result.add(new Stmt.Block(optimizedBlock));
                }
                continue;
            }
            
            result.add(stmt);
        }
        
        return result;
    }
    
    private boolean hasSideEffects(Expr expr) {
        if (expr == null) return false;
        
        return expr.accept(new Expr.Visitor<Boolean>() {
            @Override
            public Boolean visitAssignExpr(Expr.Assign expr) {
                return true; // Assignments have side effects
            }
            
            @Override
            public Boolean visitCallExpr(Expr.Call expr) {
                return true; // Function calls may have side effects
            }
            
            @Override
            public Boolean visitBinaryExpr(Expr.Binary expr) {
                return hasSideEffects(expr.left) || hasSideEffects(expr.right);
            }
            
            @Override
            public Boolean visitUnaryExpr(Expr.Unary expr) {
                return hasSideEffects(expr.right);
            }
            
            @Override
            public Boolean visitGroupingExpr(Expr.Grouping expr) {
                return hasSideEffects(expr.expression);
            }
            
            @Override
            public Boolean visitLiteralExpr(Expr.Literal expr) {
                return false;
            }
            
            @Override
            public Boolean visitVariableExpr(Expr.Variable expr) {
                return false;
            }
            
            @Override
            public Boolean visitLogicalExpr(Expr.Logical expr) {
                return hasSideEffects(expr.left) || hasSideEffects(expr.right);
            }
            
            // Default for other expression types
            @Override
            public Boolean visitGetExpr(Expr.Get expr) { return hasSideEffects(expr.object); }
            @Override
            public Boolean visitSetExpr(Expr.Set expr) { return true; }
            @Override
            public Boolean visitThisExpr(Expr.This expr) { return false; }
            @Override
            public Boolean visitListExpr(Expr.ListExpr expr) { return false; }
            @Override
            public Boolean visitDictExpr(Expr.Dict expr) { return false; }
            @Override
            public Boolean visitIndexExpr(Expr.Index expr) { return false; }
            @Override
            public Boolean visitIndexSetExpr(Expr.IndexSet expr) { return true; }
            @Override
            public Boolean visitLambdaExpr(Expr.Lambda expr) { return false; }
            @Override
            public Boolean visitMatchExpr(Expr.Match expr) { return true; }
            @Override
            public Boolean visitTypeExpr(Expr.Type expr) { return false; }
            @Override
            public Boolean visitGenericTypeExpr(Expr.GenericType expr) { return false; }
            @Override
            public Boolean visitFunctionTypeExpr(Expr.FunctionType expr) { return false; }
            @Override
            public Boolean visitArrayTypeExpr(Expr.ArrayType expr) { return false; }
        });
    }
    
    private boolean isExported(Stmt.Function func) {
        // In a real implementation, check if function is exported
        // For now, assume main functions and public methods should be kept
        return func.name.lexeme.equals("main") || func.name.lexeme.startsWith("test");
    }
    
    /**
     * Collects usage information for all symbols.
     */
    private static class UsageCollector {
        private final Set<String> usedSymbols = new HashSet<>();
        
        public void collectUsage(List<Stmt> statements) {
            for (Stmt stmt : statements) {
                collectUsageFromStatement(stmt);
            }
        }
        
        public boolean isUsed(String symbol) {
            return usedSymbols.contains(symbol);
        }
        
        private void collectUsageFromStatement(Stmt stmt) {
            stmt.accept(new Stmt.Visitor<Void>() {
                @Override
                public Void visitExpressionStmt(Stmt.Expression stmt) {
                    collectUsageFromExpression(stmt.expression);
                    return null;
                }
                
                @Override
                public Void visitVarStmt(Stmt.Var stmt) {
                    if (stmt.initializer != null) {
                        collectUsageFromExpression(stmt.initializer);
                    }
                    return null;
                }
                
                @Override
                public Void visitBlockStmt(Stmt.Block stmt) {
                    collectUsage(stmt.statements);
                    return null;
                }
                
                @Override
                public Void visitIfStmt(Stmt.If stmt) {
                    collectUsageFromExpression(stmt.condition);
                    collectUsageFromStatement(stmt.thenBranch);
                    if (stmt.elseBranch != null) {
                        collectUsageFromStatement(stmt.elseBranch);
                    }
                    return null;
                }
                
                @Override
                public Void visitWhileStmt(Stmt.While stmt) {
                    collectUsageFromExpression(stmt.condition);
                    collectUsageFromStatement(stmt.body);
                    return null;
                }
                
                @Override
                public Void visitForStmt(Stmt.For stmt) {
                    collectUsageFromExpression(stmt.iterable);
                    collectUsageFromStatement(stmt.body);
                    return null;
                }
                
                @Override
                public Void visitReturnStmt(Stmt.Return stmt) {
                    if (stmt.value != null) {
                        collectUsageFromExpression(stmt.value);
                    }
                    return null;
                }
                
                @Override
                public Void visitFunctionStmt(Stmt.Function stmt) {
                    collectUsage(stmt.body);
                    return null;
                }
                
                @Override
                public Void visitClassStmt(Stmt.Class stmt) {
                    for (Stmt.Function method : stmt.methods) {
                        collectUsageFromStatement(method);
                    }
                    return null;
                }
                
                @Override
                public Void visitImportStmt(Stmt.Import stmt) {
                    return null;
                }
                
                @Override
                public Void visitExportStmt(Stmt.Export stmt) {
                    collectUsageFromStatement(stmt.declaration);
                    return null;
                }
                
                @Override
                public Void visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                    return null;
                }
            });
        }
        
        private void collectUsageFromExpression(Expr expr) {
            expr.accept(new Expr.Visitor<Void>() {
                @Override
                public Void visitVariableExpr(Expr.Variable expr) {
                    usedSymbols.add(expr.name.lexeme);
                    return null;
                }
                
                @Override
                public Void visitCallExpr(Expr.Call expr) {
                    collectUsageFromExpression(expr.callee);
                    for (Expr arg : expr.arguments) {
                        collectUsageFromExpression(arg);
                    }
                    return null;
                }
                
                @Override
                public Void visitBinaryExpr(Expr.Binary expr) {
                    collectUsageFromExpression(expr.left);
                    collectUsageFromExpression(expr.right);
                    return null;
                }
                
                @Override
                public Void visitUnaryExpr(Expr.Unary expr) {
                    collectUsageFromExpression(expr.right);
                    return null;
                }
                
                @Override
                public Void visitGroupingExpr(Expr.Grouping expr) {
                    collectUsageFromExpression(expr.expression);
                    return null;
                }
                
                @Override
                public Void visitLogicalExpr(Expr.Logical expr) {
                    collectUsageFromExpression(expr.left);
                    collectUsageFromExpression(expr.right);
                    return null;
                }
                
                @Override
                public Void visitAssignExpr(Expr.Assign expr) {
                    usedSymbols.add(expr.name.lexeme);
                    collectUsageFromExpression(expr.value);
                    return null;
                }
                
                // Default implementations for other expression types
                @Override
                public Void visitLiteralExpr(Expr.Literal expr) { return null; }
                @Override
                public Void visitGetExpr(Expr.Get expr) { 
                    collectUsageFromExpression(expr.object);
                    return null;
                }
                @Override
                public Void visitSetExpr(Expr.Set expr) { 
                    collectUsageFromExpression(expr.object);
                    collectUsageFromExpression(expr.value);
                    return null;
                }
                @Override
                public Void visitThisExpr(Expr.This expr) { return null; }
                @Override
                public Void visitListExpr(Expr.ListExpr expr) { 
                    for (Expr element : expr.elements) {
                        collectUsageFromExpression(element);
                    }
                    return null;
                }
                @Override
                public Void visitDictExpr(Expr.Dict expr) { 
                    for (int i = 0; i < expr.keys.size(); i++) {
                        collectUsageFromExpression(expr.keys.get(i));
                        collectUsageFromExpression(expr.values.get(i));
                    }
                    return null;
                }
                @Override
                public Void visitIndexExpr(Expr.Index expr) { 
                    collectUsageFromExpression(expr.object);
                    collectUsageFromExpression(expr.index);
                    return null;
                }
                @Override
                public Void visitIndexSetExpr(Expr.IndexSet expr) { 
                    collectUsageFromExpression(expr.object);
                    collectUsageFromExpression(expr.index);
                    collectUsageFromExpression(expr.value);
                    return null;
                }
                @Override
                public Void visitLambdaExpr(Expr.Lambda expr) { 
                    collectUsage(expr.body);
                    return null;
                }
                @Override
                public Void visitMatchExpr(Expr.Match expr) { 
                    collectUsageFromExpression(expr.expr);
                    for (Expr.Match.Case c : expr.cases) {
                        collectUsageFromExpression(c.pattern);
                        if (c.guard != null) {
                            collectUsageFromExpression(c.guard);
                        }
                        collectUsageFromExpression(c.value);
                    }
                    return null;
                }
                @Override
                public Void visitTypeExpr(Expr.Type expr) { return null; }
                @Override
                public Void visitGenericTypeExpr(Expr.GenericType expr) { return null; }
                @Override
                public Void visitFunctionTypeExpr(Expr.FunctionType expr) { return null; }
                @Override
                public Void visitArrayTypeExpr(Expr.ArrayType expr) { return null; }
            });
        }
    }
}