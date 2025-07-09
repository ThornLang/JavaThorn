package com.thorn;

import java.util.*;

/**
 * Optimization pass that eliminates dead stores (assignments that are never used).
 * This pass removes assignments to variables that are overwritten before being read.
 * Example: x = 1; x = 2; return x; becomes x = 2; return x;
 */
public class DeadStoreEliminationPass extends OptimizationPass {
    
    @Override
    public String getName() {
        return "dead-store-elimination";
    }
    
    @Override
    public PassType getType() {
        return PassType.CLEANUP;
    }
    
    @Override
    public List<String> getDependencies() {
        return Arrays.asList("copy-propagation");
    }
    
    @Override
    public boolean shouldRun(OptimizationContext context) {
        // Run at O1 and above
        return context.getLevel().includes(OptimizationLevel.O1);
    }
    
    @Override
    public List<Stmt> transform(List<Stmt> statements, OptimizationContext context) {
        DeadStoreEliminator eliminator = new DeadStoreEliminator(context);
        return eliminator.eliminate(statements);
    }
    
    @Override
    public int getEstimatedCost() {
        return 5; // Moderate-high cost due to liveness analysis
    }
    
    @Override
    public String getDescription() {
        return "Eliminates assignments to variables that are never read";
    }
    
    /**
     * Eliminator that performs dead store elimination.
     */
    private static class DeadStoreEliminator {
        private final OptimizationContext context;
        private int deadStoresRemoved = 0;
        
        public DeadStoreEliminator(OptimizationContext context) {
            this.context = context;
        }
        
        public List<Stmt> eliminate(List<Stmt> statements) {
            // First pass: analyze variable usage
            UsageAnalyzer analyzer = new UsageAnalyzer();
            analyzer.analyzeStatements(statements);
            
            // Second pass: eliminate dead stores
            List<Stmt> result = eliminateDeadStores(statements, analyzer);
            
            if (context.isDebugMode()) {
                System.out.println("=== Dead Store Elimination ===");
                System.out.println("Dead stores removed: " + deadStoresRemoved);
                System.out.println("Variables analyzed: " + analyzer.variableStores.size());
            }
            
            return result;
        }
        
        private List<Stmt> eliminateDeadStores(List<Stmt> statements, UsageAnalyzer analyzer) {
            List<Stmt> result = new ArrayList<>();
            
            for (int i = 0; i < statements.size(); i++) {
                Stmt stmt = statements.get(i);
                Stmt optimized = eliminateInStatement(stmt, analyzer, statements, i);
                if (optimized != null) {
                    result.add(optimized);
                }
            }
            
            return result;
        }
        
        private Stmt eliminateInStatement(Stmt stmt, UsageAnalyzer analyzer, 
                                         List<Stmt> allStatements, int currentIndex) {
            return stmt.accept(new Stmt.Visitor<Stmt>() {
                @Override
                public Stmt visitVarStmt(Stmt.Var stmt) {
                    String varName = stmt.name.lexeme;
                    
                    // Check if this variable is immediately reassigned without being used
                    if (stmt.initializer != null && !stmt.isImmutable) {
                        if (isDeadStore(varName, currentIndex + 1, allStatements, analyzer)) {
                            deadStoresRemoved++;
                            // Keep the declaration but remove the initialization
                            return new Stmt.Var(stmt.name, stmt.type, null, stmt.isImmutable);
                        }
                    }
                    
                    return stmt;
                }
                
                @Override
                public Stmt visitExpressionStmt(Stmt.Expression stmt) {
                    if (stmt.expression instanceof Expr.Assign) {
                        Expr.Assign assign = (Expr.Assign) stmt.expression;
                        String varName = assign.name.lexeme;
                        
                        // Check if this assignment is dead
                        if (isDeadStore(varName, currentIndex + 1, allStatements, analyzer)) {
                            // Check if the assignment has side effects
                            if (!hasSideEffects(assign.value)) {
                                deadStoresRemoved++;
                                return null; // Remove the entire statement
                            }
                        }
                    }
                    
                    return stmt;
                }
                
                @Override
                public Stmt visitBlockStmt(Stmt.Block stmt) {
                    UsageAnalyzer blockAnalyzer = new UsageAnalyzer();
                    blockAnalyzer.analyzeStatements(stmt.statements);
                    
                    List<Stmt> optimizedStatements = new ArrayList<>();
                    for (int i = 0; i < stmt.statements.size(); i++) {
                        Stmt s = stmt.statements.get(i);
                        Stmt optimized = eliminateInStatement(s, blockAnalyzer, stmt.statements, i);
                        if (optimized != null) {
                            optimizedStatements.add(optimized);
                        }
                    }
                    
                    return new Stmt.Block(optimizedStatements);
                }
                
                @Override
                public Stmt visitIfStmt(Stmt.If stmt) {
                    Stmt optimizedThen = eliminateInStatement(stmt.thenBranch, analyzer, 
                        allStatements, currentIndex);
                    Stmt optimizedElse = stmt.elseBranch != null ? 
                        eliminateInStatement(stmt.elseBranch, analyzer, allStatements, currentIndex) : null;
                    
                    return new Stmt.If(stmt.condition, optimizedThen, optimizedElse);
                }
                
                @Override
                public Stmt visitWhileStmt(Stmt.While stmt) {
                    Stmt optimizedBody = eliminateInStatement(stmt.body, analyzer, 
                        allStatements, currentIndex);
                    return new Stmt.While(stmt.condition, optimizedBody);
                }
                
                @Override
                public Stmt visitForStmt(Stmt.For stmt) {
                    Stmt optimizedBody = eliminateInStatement(stmt.body, analyzer, 
                        allStatements, currentIndex);
                    return new Stmt.For(stmt.variable, stmt.iterable, optimizedBody);
                }
                
                @Override
                public Stmt visitFunctionStmt(Stmt.Function stmt) {
                    // Analyze function body separately
                    UsageAnalyzer funcAnalyzer = new UsageAnalyzer();
                    funcAnalyzer.analyzeStatements(stmt.body);
                    
                    List<Stmt> optimizedBody = new ArrayList<>();
                    for (int i = 0; i < stmt.body.size(); i++) {
                        Stmt s = stmt.body.get(i);
                        Stmt optimized = eliminateInStatement(s, funcAnalyzer, stmt.body, i);
                        if (optimized != null) {
                            optimizedBody.add(optimized);
                        }
                    }
                    
                    return new Stmt.Function(stmt.name, stmt.params, stmt.returnType, optimizedBody);
                }
                
                @Override
                public Stmt visitClassStmt(Stmt.Class stmt) {
                    List<Stmt.Function> optimizedMethods = new ArrayList<>();
                    for (Stmt.Function method : stmt.methods) {
                        optimizedMethods.add((Stmt.Function) eliminateInStatement(method, analyzer, 
                            allStatements, currentIndex));
                    }
                    return new Stmt.Class(stmt.name, optimizedMethods);
                }
                
                @Override
                public Stmt visitReturnStmt(Stmt.Return stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitImportStmt(Stmt.Import stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitExportStmt(Stmt.Export stmt) {
                    Stmt optimizedDeclaration = eliminateInStatement(stmt.declaration, analyzer, 
                        allStatements, currentIndex);
                    return new Stmt.Export(optimizedDeclaration);
                }
            });
        }
        
        private boolean isDeadStore(String varName, int afterIndex, 
                                   List<Stmt> statements, UsageAnalyzer analyzer) {
            // Check if variable is used after this point before being reassigned
            for (int i = afterIndex; i < statements.size(); i++) {
                Stmt stmt = statements.get(i);
                
                // Check if variable is read
                if (analyzer.isVariableUsed(varName, stmt)) {
                    return false; // Variable is used, not a dead store
                }
                
                // Check if variable is reassigned
                if (isVariableAssigned(varName, stmt)) {
                    return true; // Variable is reassigned without being used
                }
            }
            
            // Variable is not used or reassigned in remaining statements
            // It's dead if we're not at global scope (could be exported or used elsewhere)
            return afterIndex < statements.size();
        }
        
        private boolean isVariableAssigned(String varName, Stmt stmt) {
            return stmt.accept(new Stmt.Visitor<Boolean>() {
                @Override
                public Boolean visitVarStmt(Stmt.Var stmt) {
                    return stmt.name.lexeme.equals(varName) && stmt.initializer != null;
                }
                
                @Override
                public Boolean visitExpressionStmt(Stmt.Expression stmt) {
                    if (stmt.expression instanceof Expr.Assign) {
                        Expr.Assign assign = (Expr.Assign) stmt.expression;
                        return assign.name.lexeme.equals(varName);
                    }
                    return false;
                }
                
                @Override
                public Boolean visitBlockStmt(Stmt.Block stmt) {
                    for (Stmt s : stmt.statements) {
                        if (isVariableAssigned(varName, s)) return true;
                    }
                    return false;
                }
                
                @Override
                public Boolean visitIfStmt(Stmt.If stmt) {
                    // Conservative: assume both branches might execute
                    return false;
                }
                
                @Override
                public Boolean visitWhileStmt(Stmt.While stmt) {
                    return false;
                }
                
                @Override
                public Boolean visitForStmt(Stmt.For stmt) {
                    return false;
                }
                
                @Override
                public Boolean visitFunctionStmt(Stmt.Function stmt) {
                    return false;
                }
                
                @Override
                public Boolean visitClassStmt(Stmt.Class stmt) {
                    return false;
                }
                
                @Override
                public Boolean visitReturnStmt(Stmt.Return stmt) {
                    return false;
                }
                
                @Override
                public Boolean visitImportStmt(Stmt.Import stmt) {
                    return false;
                }
                
                @Override
                public Boolean visitExportStmt(Stmt.Export stmt) {
                    return false;
                }
            });
        }
        
        private boolean hasSideEffects(Expr expr) {
            return expr.accept(new Expr.Visitor<Boolean>() {
                @Override
                public Boolean visitCallExpr(Expr.Call expr) {
                    // Function calls might have side effects
                    return true;
                }
                
                @Override
                public Boolean visitAssignExpr(Expr.Assign expr) {
                    // Assignments have side effects
                    return true;
                }
                
                @Override
                public Boolean visitSetExpr(Expr.Set expr) {
                    // Property sets have side effects
                    return true;
                }
                
                @Override
                public Boolean visitIndexSetExpr(Expr.IndexSet expr) {
                    // Index sets have side effects
                    return true;
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
                public Boolean visitLogicalExpr(Expr.Logical expr) {
                    return hasSideEffects(expr.left) || hasSideEffects(expr.right);
                }
                
                @Override
                public Boolean visitListExpr(Expr.ListExpr expr) {
                    for (Expr element : expr.elements) {
                        if (hasSideEffects(element)) return true;
                    }
                    return false;
                }
                
                @Override
                public Boolean visitDictExpr(Expr.Dict expr) {
                    for (Expr key : expr.keys) {
                        if (hasSideEffects(key)) return true;
                    }
                    for (Expr value : expr.values) {
                        if (hasSideEffects(value)) return true;
                    }
                    return false;
                }
                
                @Override
                public Boolean visitIndexExpr(Expr.Index expr) {
                    return hasSideEffects(expr.object) || hasSideEffects(expr.index);
                }
                
                @Override
                public Boolean visitGetExpr(Expr.Get expr) {
                    return hasSideEffects(expr.object);
                }
                
                // Pure expressions - no side effects
                @Override public Boolean visitLiteralExpr(Expr.Literal expr) { return false; }
                @Override public Boolean visitVariableExpr(Expr.Variable expr) { return false; }
                @Override public Boolean visitThisExpr(Expr.This expr) { return false; }
                @Override public Boolean visitLambdaExpr(Expr.Lambda expr) { return false; }
                @Override public Boolean visitMatchExpr(Expr.Match expr) { return false; }
                @Override public Boolean visitTypeExpr(Expr.Type expr) { return false; }
                @Override public Boolean visitGenericTypeExpr(Expr.GenericType expr) { return false; }
                @Override public Boolean visitFunctionTypeExpr(Expr.FunctionType expr) { return false; }
                @Override public Boolean visitArrayTypeExpr(Expr.ArrayType expr) { return false; }
            });
        }
        
        /**
         * Analyzer that tracks variable usage patterns.
         */
        private static class UsageAnalyzer {
            private final Map<String, List<Integer>> variableStores = new HashMap<>();
            private final Map<String, List<Integer>> variableUses = new HashMap<>();
            private int statementIndex = 0;
            
            public void analyzeStatements(List<Stmt> statements) {
                statementIndex = 0;
                for (Stmt stmt : statements) {
                    analyzeStatement(stmt);
                    statementIndex++;
                }
            }
            
            public boolean isVariableUsed(String varName, Stmt stmt) {
                UsageChecker checker = new UsageChecker(varName);
                return checker.checkStatement(stmt);
            }
            
            private void analyzeStatement(Stmt stmt) {
                stmt.accept(new Stmt.Visitor<Void>() {
                    @Override
                    public Void visitVarStmt(Stmt.Var stmt) {
                        String varName = stmt.name.lexeme;
                        recordStore(varName);
                        
                        if (stmt.initializer != null) {
                            analyzeExpression(stmt.initializer);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitExpressionStmt(Stmt.Expression stmt) {
                        analyzeExpression(stmt.expression);
                        return null;
                    }
                    
                    @Override
                    public Void visitReturnStmt(Stmt.Return stmt) {
                        if (stmt.value != null) {
                            analyzeExpression(stmt.value);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitIfStmt(Stmt.If stmt) {
                        analyzeExpression(stmt.condition);
                        analyzeStatement(stmt.thenBranch);
                        if (stmt.elseBranch != null) {
                            analyzeStatement(stmt.elseBranch);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitWhileStmt(Stmt.While stmt) {
                        analyzeExpression(stmt.condition);
                        analyzeStatement(stmt.body);
                        return null;
                    }
                    
                    @Override
                    public Void visitForStmt(Stmt.For stmt) {
                        analyzeExpression(stmt.iterable);
                        analyzeStatement(stmt.body);
                        return null;
                    }
                    
                    @Override
                    public Void visitBlockStmt(Stmt.Block stmt) {
                        for (Stmt s : stmt.statements) {
                            analyzeStatement(s);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitFunctionStmt(Stmt.Function stmt) {
                        // Don't analyze function bodies in outer scope
                        return null;
                    }
                    
                    @Override
                    public Void visitClassStmt(Stmt.Class stmt) {
                        return null;
                    }
                    
                    @Override
                    public Void visitImportStmt(Stmt.Import stmt) {
                        return null;
                    }
                    
                    @Override
                    public Void visitExportStmt(Stmt.Export stmt) {
                        analyzeStatement(stmt.declaration);
                        return null;
                    }
                });
            }
            
            private void analyzeExpression(Expr expr) {
                expr.accept(new Expr.Visitor<Void>() {
                    @Override
                    public Void visitVariableExpr(Expr.Variable expr) {
                        recordUse(expr.name.lexeme);
                        return null;
                    }
                    
                    @Override
                    public Void visitAssignExpr(Expr.Assign expr) {
                        recordStore(expr.name.lexeme);
                        analyzeExpression(expr.value);
                        return null;
                    }
                    
                    @Override
                    public Void visitBinaryExpr(Expr.Binary expr) {
                        analyzeExpression(expr.left);
                        analyzeExpression(expr.right);
                        return null;
                    }
                    
                    @Override
                    public Void visitUnaryExpr(Expr.Unary expr) {
                        analyzeExpression(expr.right);
                        return null;
                    }
                    
                    @Override
                    public Void visitCallExpr(Expr.Call expr) {
                        analyzeExpression(expr.callee);
                        for (Expr arg : expr.arguments) {
                            analyzeExpression(arg);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitGroupingExpr(Expr.Grouping expr) {
                        analyzeExpression(expr.expression);
                        return null;
                    }
                    
                    @Override
                    public Void visitLogicalExpr(Expr.Logical expr) {
                        analyzeExpression(expr.left);
                        analyzeExpression(expr.right);
                        return null;
                    }
                    
                    @Override
                    public Void visitListExpr(Expr.ListExpr expr) {
                        for (Expr element : expr.elements) {
                            analyzeExpression(element);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitDictExpr(Expr.Dict expr) {
                        for (Expr key : expr.keys) {
                            analyzeExpression(key);
                        }
                        for (Expr value : expr.values) {
                            analyzeExpression(value);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitIndexExpr(Expr.Index expr) {
                        analyzeExpression(expr.object);
                        analyzeExpression(expr.index);
                        return null;
                    }
                    
                    @Override
                    public Void visitGetExpr(Expr.Get expr) {
                        analyzeExpression(expr.object);
                        return null;
                    }
                    
                    @Override
                    public Void visitSetExpr(Expr.Set expr) {
                        analyzeExpression(expr.object);
                        analyzeExpression(expr.value);
                        return null;
                    }
                    
                    @Override
                    public Void visitIndexSetExpr(Expr.IndexSet expr) {
                        analyzeExpression(expr.object);
                        analyzeExpression(expr.index);
                        analyzeExpression(expr.value);
                        return null;
                    }
                    
                    // Simple expressions
                    @Override public Void visitLiteralExpr(Expr.Literal expr) { return null; }
                    @Override public Void visitThisExpr(Expr.This expr) { return null; }
                    @Override public Void visitLambdaExpr(Expr.Lambda expr) { return null; }
                    @Override public Void visitMatchExpr(Expr.Match expr) { return null; }
                    @Override public Void visitTypeExpr(Expr.Type expr) { return null; }
                    @Override public Void visitGenericTypeExpr(Expr.GenericType expr) { return null; }
                    @Override public Void visitFunctionTypeExpr(Expr.FunctionType expr) { return null; }
                    @Override public Void visitArrayTypeExpr(Expr.ArrayType expr) { return null; }
                });
            }
            
            private void recordStore(String varName) {
                variableStores.computeIfAbsent(varName, k -> new ArrayList<>()).add(statementIndex);
            }
            
            private void recordUse(String varName) {
                variableUses.computeIfAbsent(varName, k -> new ArrayList<>()).add(statementIndex);
            }
        }
        
        /**
         * Checker that determines if a variable is used in a statement.
         */
        private static class UsageChecker {
            private final String targetVariable;
            
            public UsageChecker(String targetVariable) {
                this.targetVariable = targetVariable;
            }
            
            public boolean checkStatement(Stmt stmt) {
                return stmt.accept(new Stmt.Visitor<Boolean>() {
                    @Override
                    public Boolean visitExpressionStmt(Stmt.Expression stmt) {
                        return checkExpression(stmt.expression);
                    }
                    
                    @Override
                    public Boolean visitVarStmt(Stmt.Var stmt) {
                        return stmt.initializer != null && checkExpression(stmt.initializer);
                    }
                    
                    @Override
                    public Boolean visitReturnStmt(Stmt.Return stmt) {
                        return stmt.value != null && checkExpression(stmt.value);
                    }
                    
                    @Override
                    public Boolean visitIfStmt(Stmt.If stmt) {
                        return checkExpression(stmt.condition) ||
                               checkStatement(stmt.thenBranch) ||
                               (stmt.elseBranch != null && checkStatement(stmt.elseBranch));
                    }
                    
                    @Override
                    public Boolean visitWhileStmt(Stmt.While stmt) {
                        return checkExpression(stmt.condition) || checkStatement(stmt.body);
                    }
                    
                    @Override
                    public Boolean visitForStmt(Stmt.For stmt) {
                        return checkExpression(stmt.iterable) || checkStatement(stmt.body);
                    }
                    
                    @Override
                    public Boolean visitBlockStmt(Stmt.Block stmt) {
                        for (Stmt s : stmt.statements) {
                            if (checkStatement(s)) return true;
                        }
                        return false;
                    }
                    
                    @Override
                    public Boolean visitFunctionStmt(Stmt.Function stmt) {
                        // Don't check inside function bodies
                        return false;
                    }
                    
                    @Override
                    public Boolean visitClassStmt(Stmt.Class stmt) {
                        return false;
                    }
                    
                    @Override
                    public Boolean visitImportStmt(Stmt.Import stmt) {
                        return false;
                    }
                    
                    @Override
                    public Boolean visitExportStmt(Stmt.Export stmt) {
                        return checkStatement(stmt.declaration);
                    }
                });
            }
            
            public boolean checkExpression(Expr expr) {
                return expr.accept(new Expr.Visitor<Boolean>() {
                    @Override
                    public Boolean visitVariableExpr(Expr.Variable expr) {
                        return expr.name.lexeme.equals(targetVariable);
                    }
                    
                    @Override
                    public Boolean visitBinaryExpr(Expr.Binary expr) {
                        return checkExpression(expr.left) || checkExpression(expr.right);
                    }
                    
                    @Override
                    public Boolean visitUnaryExpr(Expr.Unary expr) {
                        return checkExpression(expr.right);
                    }
                    
                    @Override
                    public Boolean visitCallExpr(Expr.Call expr) {
                        if (checkExpression(expr.callee)) return true;
                        for (Expr arg : expr.arguments) {
                            if (checkExpression(arg)) return true;
                        }
                        return false;
                    }
                    
                    @Override
                    public Boolean visitAssignExpr(Expr.Assign expr) {
                        // Assignment to our variable doesn't count as a use
                        return checkExpression(expr.value);
                    }
                    
                    @Override
                    public Boolean visitGroupingExpr(Expr.Grouping expr) {
                        return checkExpression(expr.expression);
                    }
                    
                    @Override
                    public Boolean visitLogicalExpr(Expr.Logical expr) {
                        return checkExpression(expr.left) || checkExpression(expr.right);
                    }
                    
                    @Override
                    public Boolean visitListExpr(Expr.ListExpr expr) {
                        for (Expr element : expr.elements) {
                            if (checkExpression(element)) return true;
                        }
                        return false;
                    }
                    
                    @Override
                    public Boolean visitDictExpr(Expr.Dict expr) {
                        for (Expr key : expr.keys) {
                            if (checkExpression(key)) return true;
                        }
                        for (Expr value : expr.values) {
                            if (checkExpression(value)) return true;
                        }
                        return false;
                    }
                    
                    @Override
                    public Boolean visitIndexExpr(Expr.Index expr) {
                        return checkExpression(expr.object) || checkExpression(expr.index);
                    }
                    
                    @Override
                    public Boolean visitGetExpr(Expr.Get expr) {
                        return checkExpression(expr.object);
                    }
                    
                    @Override
                    public Boolean visitSetExpr(Expr.Set expr) {
                        return checkExpression(expr.object) || checkExpression(expr.value);
                    }
                    
                    @Override
                    public Boolean visitIndexSetExpr(Expr.IndexSet expr) {
                        return checkExpression(expr.object) || checkExpression(expr.index) || 
                               checkExpression(expr.value);
                    }
                    
                    // Simple expressions
                    @Override public Boolean visitLiteralExpr(Expr.Literal expr) { return false; }
                    @Override public Boolean visitThisExpr(Expr.This expr) { return false; }
                    @Override public Boolean visitLambdaExpr(Expr.Lambda expr) { return false; }
                    @Override public Boolean visitMatchExpr(Expr.Match expr) { return false; }
                    @Override public Boolean visitTypeExpr(Expr.Type expr) { return false; }
                    @Override public Boolean visitGenericTypeExpr(Expr.GenericType expr) { return false; }
                    @Override public Boolean visitFunctionTypeExpr(Expr.FunctionType expr) { return false; }
                    @Override public Boolean visitArrayTypeExpr(Expr.ArrayType expr) { return false; }
                });
            }
        }
    }
}