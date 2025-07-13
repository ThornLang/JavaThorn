package com.thorn;

import java.util.*;

/**
 * Dead code elimination optimizer for Thorn language.
 * Removes unused functions, variables, lambdas, classes, and imports.
 */
public class DeadCodeEliminator {
    
    private final Set<String> usedSymbols = new HashSet<>();
    private final Set<String> definedSymbols = new HashSet<>();
    private final Map<String, Stmt> symbolDefinitions = new HashMap<>();
    private final Set<String> exportedSymbols = new HashSet<>();
    
    // Local scope tracking for function/method body optimization
    private static class LocalScope {
        final Set<String> localDefinitions = new HashSet<>();
        final Set<String> localUsages = new HashSet<>();
        final String scopeName;
        
        LocalScope(String scopeName) {
            this.scopeName = scopeName;
        }
    }
    
    /**
     * Optimize the AST by removing dead code.
     * @param statements List of statements to optimize
     * @return Optimized list of statements with dead code removed
     */
    public List<Stmt> optimize(List<Stmt> statements) {
        // Reset state
        usedSymbols.clear();
        definedSymbols.clear();
        symbolDefinitions.clear();
        exportedSymbols.clear();
        
        // First pass: collect all symbol definitions and exports
        for (Stmt stmt : statements) {
            collectDefinitions(stmt);
            collectExports(stmt);
        }
        
        // Second pass: collect all symbol usages
        for (Stmt stmt : statements) {
            collectUsages(stmt);
        }
        
        // Mark exported symbols as used
        usedSymbols.addAll(exportedSymbols);
        
        // Third pass: filter out unused symbols, handle side effects, and apply local optimizations
        List<Stmt> optimized = new ArrayList<>();
        for (Stmt stmt : statements) {
            Stmt processedStmt = processStatementWithLocalOptimization(stmt);
            if (processedStmt != null) {
                optimized.add(processedStmt);
            }
        }
        
        return optimized;
    }
    
    /**
     * Collect all symbol definitions from statements.
     */
    private void collectDefinitions(Stmt stmt) {
        stmt.accept(new Stmt.Visitor<Void>() {
            @Override
            public Void visitFunctionStmt(Stmt.Function stmt) {
                String name = stmt.name.lexeme;
                definedSymbols.add(name);
                symbolDefinitions.put(name, stmt);
                
                // Process function body for nested definitions
                for (Stmt bodyStmt : stmt.body) {
                    collectDefinitions(bodyStmt);
                }
                return null;
            }
            
            @Override
            public Void visitVarStmt(Stmt.Var stmt) {
                String name = stmt.name.lexeme;
                definedSymbols.add(name);
                symbolDefinitions.put(name, stmt);
                
                // Process initializer for nested definitions
                if (stmt.initializer != null) {
                    collectDefinitionsFromExpr(stmt.initializer);
                }
                return null;
            }
            
            @Override
            public Void visitClassStmt(Stmt.Class stmt) {
                String name = stmt.name.lexeme;
                definedSymbols.add(name);
                symbolDefinitions.put(name, stmt);
                
                // Process class methods
                for (Stmt.Function method : stmt.methods) {
                    collectDefinitions(method);
                }
                return null;
            }
            
            @Override
            public Void visitImportStmt(Stmt.Import stmt) {
                if (stmt.names != null) {
                    for (Token name : stmt.names) {
                        definedSymbols.add(name.lexeme);
                        symbolDefinitions.put(name.lexeme, stmt);
                    }
                } else {
                    // Import entire module
                    definedSymbols.add(stmt.module.lexeme);
                    symbolDefinitions.put(stmt.module.lexeme, stmt);
                }
                return null;
            }
            
            @Override
            public Void visitExportStmt(Stmt.Export stmt) {
                collectDefinitions(stmt.declaration);
                return null;
            }
            
            @Override
            public Void visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                return null;
            }
            
            @Override
            public Void visitTryCatchStmt(Stmt.TryCatch stmt) {
                collectDefinitions(stmt.tryBlock);
                collectDefinitions(stmt.catchBlock);
                return null;
            }
            
            
            @Override
            public Void visitBlockStmt(Stmt.Block stmt) {
                for (Stmt blockStmt : stmt.statements) {
                    collectDefinitions(blockStmt);
                }
                return null;
            }
            
            @Override
            public Void visitIfStmt(Stmt.If stmt) {
                collectDefinitions(stmt.thenBranch);
                if (stmt.elseBranch != null) {
                    collectDefinitions(stmt.elseBranch);
                }
                return null;
            }
            
            @Override
            public Void visitWhileStmt(Stmt.While stmt) {
                collectDefinitions(stmt.body);
                return null;
            }
            
            @Override
            public Void visitForStmt(Stmt.For stmt) {
                collectDefinitions(stmt.body);
                return null;
            }
            
            @Override
            public Void visitExpressionStmt(Stmt.Expression stmt) {
                collectDefinitionsFromExpr(stmt.expression);
                return null;
            }
            
            @Override
            public Void visitReturnStmt(Stmt.Return stmt) {
                if (stmt.value != null) {
                    collectDefinitionsFromExpr(stmt.value);
                }
                return null;
            }
            
            @Override
            public Void visitThrowStmt(Stmt.Throw stmt) {
                if (stmt.value != null) {
                    collectDefinitionsFromExpr(stmt.value);
                }
                return null;
            }
            
            @Override
            public Void visitTypeAliasStmt(Stmt.TypeAlias stmt) {
                // Type aliases are compile-time only, no optimization needed
                return null;
            }
        });
    }
    
    /**
     * Collect definitions from expressions (e.g., lambdas).
     */
    private void collectDefinitionsFromExpr(Expr expr) {
        expr.accept(new Expr.Visitor<Void>() {
            @Override
            public Void visitLambdaExpr(Expr.Lambda expr) {
                // Lambda parameters are local to lambda scope, not global
                // They should not be added to global definedSymbols
                
                // Process lambda body for nested definitions
                for (Stmt stmt : expr.body) {
                    collectDefinitions(stmt);
                }
                return null;
            }
            
            @Override
            public Void visitCallExpr(Expr.Call expr) {
                collectDefinitionsFromExpr(expr.callee);
                for (Expr arg : expr.arguments) {
                    collectDefinitionsFromExpr(arg);
                }
                return null;
            }
            
            @Override
            public Void visitBinaryExpr(Expr.Binary expr) {
                collectDefinitionsFromExpr(expr.left);
                collectDefinitionsFromExpr(expr.right);
                return null;
            }
            
            @Override
            public Void visitUnaryExpr(Expr.Unary expr) {
                collectDefinitionsFromExpr(expr.right);
                return null;
            }
            
            @Override
            public Void visitGroupingExpr(Expr.Grouping expr) {
                collectDefinitionsFromExpr(expr.expression);
                return null;
            }
            
            @Override
            public Void visitAssignExpr(Expr.Assign expr) {
                collectDefinitionsFromExpr(expr.value);
                return null;
            }
            
            @Override
            public Void visitLogicalExpr(Expr.Logical expr) {
                collectDefinitionsFromExpr(expr.left);
                collectDefinitionsFromExpr(expr.right);
                return null;
            }
            
            @Override
            public Void visitListExpr(Expr.ListExpr expr) {
                for (Expr element : expr.elements) {
                    collectDefinitionsFromExpr(element);
                }
                return null;
            }
            
            @Override
            public Void visitDictExpr(Expr.Dict expr) {
                for (Expr key : expr.keys) {
                    collectDefinitionsFromExpr(key);
                }
                for (Expr value : expr.values) {
                    collectDefinitionsFromExpr(value);
                }
                return null;
            }
            
            @Override
            public Void visitIndexExpr(Expr.Index expr) {
                collectDefinitionsFromExpr(expr.object);
                collectDefinitionsFromExpr(expr.index);
                return null;
            }
            
            @Override
            public Void visitIndexSetExpr(Expr.IndexSet expr) {
                collectDefinitionsFromExpr(expr.object);
                collectDefinitionsFromExpr(expr.index);
                collectDefinitionsFromExpr(expr.value);
                return null;
            }
            
            @Override
            public Void visitSliceExpr(Expr.Slice expr) {
                collectDefinitionsFromExpr(expr.object);
                if (expr.start != null) {
                    collectDefinitionsFromExpr(expr.start);
                }
                if (expr.end != null) {
                    collectDefinitionsFromExpr(expr.end);
                }
                return null;
            }
            
            @Override
            public Void visitMatchExpr(Expr.Match expr) {
                collectDefinitionsFromExpr(expr.expr);
                for (Expr.Match.Case matchCase : expr.cases) {
                    collectDefinitionsFromExpr(matchCase.pattern);
                    if (matchCase.guard != null) {
                        collectDefinitionsFromExpr(matchCase.guard);
                    }
                    collectDefinitionsFromExpr(matchCase.value);
                }
                return null;
            }
            
            @Override
            public Void visitGetExpr(Expr.Get expr) {
                collectDefinitionsFromExpr(expr.object);
                return null;
            }
            
            @Override
            public Void visitSetExpr(Expr.Set expr) {
                collectDefinitionsFromExpr(expr.object);
                collectDefinitionsFromExpr(expr.value);
                return null;
            }
            
            // Simple expressions that don't define symbols
            @Override public Void visitLiteralExpr(Expr.Literal expr) { return null; }
            @Override public Void visitVariableExpr(Expr.Variable expr) { return null; }
            @Override public Void visitThisExpr(Expr.This expr) { return null; }
            @Override public Void visitTypeExpr(Expr.Type expr) { return null; }
            @Override public Void visitGenericTypeExpr(Expr.GenericType expr) { return null; }
            @Override public Void visitFunctionTypeExpr(Expr.FunctionType expr) { return null; }
            @Override public Void visitArrayTypeExpr(Expr.ArrayType expr) { return null; }
        });
    }
    
    /**
     * Collect all symbol usages from statements.
     */
    private void collectUsages(Stmt stmt) {
        stmt.accept(new Stmt.Visitor<Void>() {
            @Override
            public Void visitFunctionStmt(Stmt.Function stmt) {
                // Process function body for usages
                for (Stmt bodyStmt : stmt.body) {
                    collectUsages(bodyStmt);
                }
                return null;
            }
            
            @Override
            public Void visitVarStmt(Stmt.Var stmt) {
                if (stmt.initializer != null) {
                    collectUsagesFromExpr(stmt.initializer);
                }
                return null;
            }
            
            @Override
            public Void visitClassStmt(Stmt.Class stmt) {
                for (Stmt.Function method : stmt.methods) {
                    collectUsages(method);
                }
                return null;
            }
            
            @Override
            public Void visitExpressionStmt(Stmt.Expression stmt) {
                collectUsagesFromExpr(stmt.expression);
                return null;
            }
            
            @Override
            public Void visitIfStmt(Stmt.If stmt) {
                collectUsagesFromExpr(stmt.condition);
                collectUsages(stmt.thenBranch);
                if (stmt.elseBranch != null) {
                    collectUsages(stmt.elseBranch);
                }
                return null;
            }
            
            @Override
            public Void visitWhileStmt(Stmt.While stmt) {
                collectUsagesFromExpr(stmt.condition);
                collectUsages(stmt.body);
                return null;
            }
            
            @Override
            public Void visitForStmt(Stmt.For stmt) {
                collectUsagesFromExpr(stmt.iterable);
                collectUsages(stmt.body);
                return null;
            }
            
            @Override
            public Void visitReturnStmt(Stmt.Return stmt) {
                if (stmt.value != null) {
                    collectUsagesFromExpr(stmt.value);
                }
                return null;
            }
            
            @Override
            public Void visitThrowStmt(Stmt.Throw stmt) {
                if (stmt.value != null) {
                    collectUsagesFromExpr(stmt.value);
                }
                return null;
            }
            
            @Override
            public Void visitBlockStmt(Stmt.Block stmt) {
                for (Stmt blockStmt : stmt.statements) {
                    collectUsages(blockStmt);
                }
                return null;
            }
            
            @Override
            public Void visitImportStmt(Stmt.Import stmt) {
                // Import statements don't use symbols, they define them
                return null;
            }
            
            @Override
            public Void visitExportStmt(Stmt.Export stmt) {
                collectUsages(stmt.declaration);
                return null;
            }
            
            @Override
            public Void visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                return null;
            }
            
            @Override
            public Void visitTryCatchStmt(Stmt.TryCatch stmt) {
                collectUsages(stmt.tryBlock);
                collectUsages(stmt.catchBlock);
                return null;
            }
            
            @Override
            public Void visitTypeAliasStmt(Stmt.TypeAlias stmt) {
                // Type aliases are compile-time only, no optimization needed
                return null;
            }
        });
    }
    
    /**
     * Collect usages from expressions.
     */
    private void collectUsagesFromExpr(Expr expr) {
        expr.accept(new Expr.Visitor<Void>() {
            @Override
            public Void visitVariableExpr(Expr.Variable expr) {
                usedSymbols.add(expr.name.lexeme);
                return null;
            }
            
            @Override
            public Void visitCallExpr(Expr.Call expr) {
                collectUsagesFromExpr(expr.callee);
                for (Expr arg : expr.arguments) {
                    collectUsagesFromExpr(arg);
                }
                return null;
            }
            
            @Override
            public Void visitLambdaExpr(Expr.Lambda expr) {
                for (Stmt stmt : expr.body) {
                    collectUsages(stmt);
                }
                return null;
            }
            
            @Override
            public Void visitBinaryExpr(Expr.Binary expr) {
                collectUsagesFromExpr(expr.left);
                collectUsagesFromExpr(expr.right);
                return null;
            }
            
            @Override
            public Void visitUnaryExpr(Expr.Unary expr) {
                collectUsagesFromExpr(expr.right);
                return null;
            }
            
            @Override
            public Void visitGroupingExpr(Expr.Grouping expr) {
                collectUsagesFromExpr(expr.expression);
                return null;
            }
            
            @Override
            public Void visitAssignExpr(Expr.Assign expr) {
                // Note: We don't add the assigned variable to usedSymbols here
                // because assignment alone doesn't constitute "usage" - only reading does
                collectUsagesFromExpr(expr.value);
                return null;
            }
            
            @Override
            public Void visitLogicalExpr(Expr.Logical expr) {
                collectUsagesFromExpr(expr.left);
                collectUsagesFromExpr(expr.right);
                return null;
            }
            
            @Override
            public Void visitListExpr(Expr.ListExpr expr) {
                for (Expr element : expr.elements) {
                    collectUsagesFromExpr(element);
                }
                return null;
            }
            
            @Override
            public Void visitDictExpr(Expr.Dict expr) {
                for (Expr key : expr.keys) {
                    collectUsagesFromExpr(key);
                }
                for (Expr value : expr.values) {
                    collectUsagesFromExpr(value);
                }
                return null;
            }
            
            @Override
            public Void visitIndexExpr(Expr.Index expr) {
                collectUsagesFromExpr(expr.object);
                collectUsagesFromExpr(expr.index);
                return null;
            }
            
            @Override
            public Void visitIndexSetExpr(Expr.IndexSet expr) {
                collectUsagesFromExpr(expr.object);
                collectUsagesFromExpr(expr.index);
                collectUsagesFromExpr(expr.value);
                return null;
            }
            
            @Override
            public Void visitSliceExpr(Expr.Slice expr) {
                collectUsagesFromExpr(expr.object);
                if (expr.start != null) {
                    collectUsagesFromExpr(expr.start);
                }
                if (expr.end != null) {
                    collectUsagesFromExpr(expr.end);
                }
                return null;
            }
            
            @Override
            public Void visitMatchExpr(Expr.Match expr) {
                collectUsagesFromExpr(expr.expr);
                for (Expr.Match.Case matchCase : expr.cases) {
                    collectUsagesFromExpr(matchCase.pattern);
                    if (matchCase.guard != null) {
                        collectUsagesFromExpr(matchCase.guard);
                    }
                    collectUsagesFromExpr(matchCase.value);
                }
                return null;
            }
            
            @Override
            public Void visitGetExpr(Expr.Get expr) {
                collectUsagesFromExpr(expr.object);
                return null;
            }
            
            @Override
            public Void visitSetExpr(Expr.Set expr) {
                collectUsagesFromExpr(expr.object);
                collectUsagesFromExpr(expr.value);
                return null;
            }
            
            // Simple expressions that don't use symbols
            @Override public Void visitLiteralExpr(Expr.Literal expr) { return null; }
            @Override public Void visitThisExpr(Expr.This expr) { return null; }
            @Override public Void visitTypeExpr(Expr.Type expr) { return null; }
            @Override public Void visitGenericTypeExpr(Expr.GenericType expr) { return null; }
            @Override public Void visitFunctionTypeExpr(Expr.FunctionType expr) { return null; }
            @Override public Void visitArrayTypeExpr(Expr.ArrayType expr) { return null; }
        });
    }
    
    /**
     * Collect exported symbols from statements.
     */
    private void collectExports(Stmt stmt) {
        if (stmt instanceof Stmt.Export) {
            Stmt.Export export = (Stmt.Export) stmt;
            if (export.declaration instanceof Stmt.Function) {
                Stmt.Function func = (Stmt.Function) export.declaration;
                exportedSymbols.add(func.name.lexeme);
            } else if (export.declaration instanceof Stmt.Var) {
                Stmt.Var var = (Stmt.Var) export.declaration;
                exportedSymbols.add(var.name.lexeme);
            } else if (export.declaration instanceof Stmt.Class) {
                Stmt.Class cls = (Stmt.Class) export.declaration;
                exportedSymbols.add(cls.name.lexeme);
            }
        }
    }
    
    /**
     * Process a statement with both global and local optimizations.
     */
    private Stmt processStatementWithLocalOptimization(Stmt stmt) {
        // First apply global optimizations
        if (!shouldKeepStatement(stmt)) {
            // Handle side effects for unused variable assignments
            if (stmt instanceof Stmt.Expression) {
                Stmt.Expression exprStmt = (Stmt.Expression) stmt;
                if (exprStmt.expression instanceof Expr.Assign) {
                    Expr.Assign assign = (Expr.Assign) exprStmt.expression;
                    if (!usedSymbols.contains(assign.name.lexeme)) {
                        // Check if the assignment has side effects (function calls)
                        if (hasSideEffects(assign.value)) {
                            // Convert to expression statement without assignment
                            return new Stmt.Expression(assign.value);
                        }
                    }
                }
            }
            return null; // Remove statement
        }
        
        // Apply local optimizations for functions and classes
        if (stmt instanceof Stmt.Function) {
            return optimizeFunction((Stmt.Function) stmt);
        } else if (stmt instanceof Stmt.Class) {
            return optimizeClass((Stmt.Class) stmt);
        }
        
        return stmt;
    }
    
    /**
     * Optimize a function by applying local dead code elimination to its body.
     */
    private Stmt.Function optimizeFunction(Stmt.Function func) {
        List<Stmt> optimizedBody = optimizeLocalScope(func.body, "function:" + func.name.lexeme);
        
        // Return new function with optimized body
        return new Stmt.Function(func.name, func.params, func.returnType, optimizedBody);
    }
    
    /**
     * Optimize a class by applying local dead code elimination to its methods.
     */
    private Stmt.Class optimizeClass(Stmt.Class cls) {
        List<Stmt.Function> optimizedMethods = new ArrayList<>();
        
        for (Stmt.Function method : cls.methods) {
            List<Stmt> optimizedBody = optimizeLocalScope(method.body, "method:" + cls.name.lexeme + "." + method.name.lexeme);
            optimizedMethods.add(new Stmt.Function(method.name, method.params, method.returnType, optimizedBody));
        }
        
        // Return new class with optimized methods
        return new Stmt.Class(cls.name, optimizedMethods);
    }
    
    /**
     * Optimize a local scope (function body or method body) by removing unused local variables.
     */
    private List<Stmt> optimizeLocalScope(List<Stmt> statements, String scopeName) {
        LocalScope scope = new LocalScope(scopeName);
        
        // First pass: collect local definitions and usages
        for (Stmt stmt : statements) {
            analyzeLocalStatement(stmt, scope);
        }
        
        // Second pass: filter and optimize statements
        List<Stmt> optimized = new ArrayList<>();
        for (Stmt stmt : statements) {
            Stmt optimizedStmt = processLocalStatement(stmt, scope);
            if (optimizedStmt != null) {
                optimized.add(optimizedStmt);
            }
        }
        
        return optimized;
    }
    
    /**
     * Analyze a statement within a local scope to track variable definitions and usages.
     */
    private void analyzeLocalStatement(Stmt stmt, LocalScope scope) {
        stmt.accept(new Stmt.Visitor<Void>() {
            @Override
            public Void visitExpressionStmt(Stmt.Expression stmt) {
                analyzeLocalExpression(stmt.expression, scope);
                return null;
            }
            
            @Override
            public Void visitVarStmt(Stmt.Var stmt) {
                scope.localDefinitions.add(stmt.name.lexeme);
                if (stmt.initializer != null) {
                    analyzeLocalExpression(stmt.initializer, scope);
                }
                return null;
            }
            
            @Override
            public Void visitReturnStmt(Stmt.Return stmt) {
                if (stmt.value != null) {
                    analyzeLocalExpression(stmt.value, scope);
                }
                return null;
            }
            
            @Override
            public Void visitThrowStmt(Stmt.Throw stmt) {
                if (stmt.value != null) {
                    analyzeLocalExpression(stmt.value, scope);
                }
                return null;
            }
            
            @Override
            public Void visitIfStmt(Stmt.If stmt) {
                analyzeLocalExpression(stmt.condition, scope);
                analyzeLocalStatement(stmt.thenBranch, scope);
                if (stmt.elseBranch != null) {
                    analyzeLocalStatement(stmt.elseBranch, scope);
                }
                return null;
            }
            
            @Override
            public Void visitWhileStmt(Stmt.While stmt) {
                analyzeLocalExpression(stmt.condition, scope);
                analyzeLocalStatement(stmt.body, scope);
                return null;
            }
            
            @Override
            public Void visitForStmt(Stmt.For stmt) {
                // For loop variable is a local definition
                scope.localDefinitions.add(stmt.variable.lexeme);
                
                analyzeLocalExpression(stmt.iterable, scope);
                analyzeLocalStatement(stmt.body, scope);
                return null;
            }
            
            @Override
            public Void visitBlockStmt(Stmt.Block stmt) {
                for (Stmt blockStmt : stmt.statements) {
                    analyzeLocalStatement(blockStmt, scope);
                }
                return null;
            }
            
            // These shouldn't appear in function bodies, but handle them safely
            @Override public Void visitFunctionStmt(Stmt.Function stmt) { return null; }
            @Override public Void visitClassStmt(Stmt.Class stmt) { return null; }
            @Override public Void visitImportStmt(Stmt.Import stmt) { return null; }
            @Override public Void visitExportStmt(Stmt.Export stmt) { return null; }
            @Override public Void visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) { return null; }
            @Override public Void visitTryCatchStmt(Stmt.TryCatch stmt) { 
                analyzeLocalStatement(stmt.tryBlock, scope);
                analyzeLocalStatement(stmt.catchBlock, scope);
                return null; 
            }
            @Override public Void visitTypeAliasStmt(Stmt.TypeAlias stmt) { return null; }
        });
    }
    
    /**
     * Analyze an expression within a local scope to track variable usages.
     */
    private void analyzeLocalExpression(Expr expr, LocalScope scope) {
        expr.accept(new Expr.Visitor<Void>() {
            @Override
            public Void visitVariableExpr(Expr.Variable expr) {
                scope.localUsages.add(expr.name.lexeme);
                return null;
            }
            
            @Override
            public Void visitAssignExpr(Expr.Assign expr) {
                scope.localDefinitions.add(expr.name.lexeme);
                analyzeLocalExpression(expr.value, scope);
                return null;
            }
            
            @Override
            public Void visitCallExpr(Expr.Call expr) {
                analyzeLocalExpression(expr.callee, scope);
                for (Expr arg : expr.arguments) {
                    analyzeLocalExpression(arg, scope);
                }
                return null;
            }
            
            @Override
            public Void visitBinaryExpr(Expr.Binary expr) {
                analyzeLocalExpression(expr.left, scope);
                analyzeLocalExpression(expr.right, scope);
                return null;
            }
            
            @Override
            public Void visitUnaryExpr(Expr.Unary expr) {
                analyzeLocalExpression(expr.right, scope);
                return null;
            }
            
            @Override
            public Void visitGroupingExpr(Expr.Grouping expr) {
                analyzeLocalExpression(expr.expression, scope);
                return null;
            }
            
            @Override
            public Void visitLogicalExpr(Expr.Logical expr) {
                analyzeLocalExpression(expr.left, scope);
                analyzeLocalExpression(expr.right, scope);
                return null;
            }
            
            @Override
            public Void visitGetExpr(Expr.Get expr) {
                analyzeLocalExpression(expr.object, scope);
                return null;
            }
            
            @Override
            public Void visitSetExpr(Expr.Set expr) {
                analyzeLocalExpression(expr.object, scope);
                analyzeLocalExpression(expr.value, scope);
                return null;
            }
            
            @Override
            public Void visitIndexExpr(Expr.Index expr) {
                analyzeLocalExpression(expr.object, scope);
                analyzeLocalExpression(expr.index, scope);
                return null;
            }
            
            @Override
            public Void visitIndexSetExpr(Expr.IndexSet expr) {
                analyzeLocalExpression(expr.object, scope);
                analyzeLocalExpression(expr.index, scope);
                analyzeLocalExpression(expr.value, scope);
                return null;
            }
            
            @Override
            public Void visitSliceExpr(Expr.Slice expr) {
                analyzeLocalExpression(expr.object, scope);
                if (expr.start != null) {
                    analyzeLocalExpression(expr.start, scope);
                }
                if (expr.end != null) {
                    analyzeLocalExpression(expr.end, scope);
                }
                return null;
            }
            
            @Override
            public Void visitListExpr(Expr.ListExpr expr) {
                for (Expr element : expr.elements) {
                    analyzeLocalExpression(element, scope);
                }
                return null;
            }
            
            @Override
            public Void visitDictExpr(Expr.Dict expr) {
                for (Expr key : expr.keys) {
                    analyzeLocalExpression(key, scope);
                }
                for (Expr value : expr.values) {
                    analyzeLocalExpression(value, scope);
                }
                return null;
            }
            
            @Override
            public Void visitMatchExpr(Expr.Match expr) {
                analyzeLocalExpression(expr.expr, scope);
                for (Expr.Match.Case matchCase : expr.cases) {
                    analyzeLocalExpression(matchCase.pattern, scope);
                    if (matchCase.guard != null) {
                        analyzeLocalExpression(matchCase.guard, scope);
                    }
                    analyzeLocalExpression(matchCase.value, scope);
                }
                return null;
            }
            
            @Override
            public Void visitLambdaExpr(Expr.Lambda expr) {
                // Lambda parameters are local to the lambda, not this scope
                for (Stmt stmt : expr.body) {
                    analyzeLocalStatement(stmt, scope);
                }
                return null;
            }
            
            // Simple expressions that don't affect local scope
            @Override public Void visitLiteralExpr(Expr.Literal expr) { return null; }
            @Override public Void visitThisExpr(Expr.This expr) { return null; }
            @Override public Void visitTypeExpr(Expr.Type expr) { return null; }
            @Override public Void visitGenericTypeExpr(Expr.GenericType expr) { return null; }
            @Override public Void visitFunctionTypeExpr(Expr.FunctionType expr) { return null; }
            @Override public Void visitArrayTypeExpr(Expr.ArrayType expr) { return null; }
        });
    }
    
    /**
     * Process a statement within a local scope, applying local dead code elimination.
     */
    private Stmt processLocalStatement(Stmt stmt, LocalScope scope) {
        if (stmt instanceof Stmt.Expression) {
            Stmt.Expression exprStmt = (Stmt.Expression) stmt;
            if (exprStmt.expression instanceof Expr.Assign) {
                Expr.Assign assign = (Expr.Assign) exprStmt.expression;
                // Check if this local variable is unused
                if (!scope.localUsages.contains(assign.name.lexeme)) {
                    // Check if the assignment has side effects
                    if (hasSideEffects(assign.value)) {
                        // Convert to expression statement without assignment
                        return new Stmt.Expression(assign.value);
                    } else {
                        // Remove the statement entirely
                        return null;
                    }
                }
            }
        }
        
        // Keep all other statements (return, if, while, etc.)
        return stmt;
    }
    
    /**
     * Process a statement for dead code elimination and side effect preservation.
     */
    private Stmt processStatement(Stmt stmt) {
        if (shouldKeepStatement(stmt)) {
            return stmt;
        }
        
        // Handle side effects for unused variable assignments
        if (stmt instanceof Stmt.Expression) {
            Stmt.Expression exprStmt = (Stmt.Expression) stmt;
            if (exprStmt.expression instanceof Expr.Assign) {
                Expr.Assign assign = (Expr.Assign) exprStmt.expression;
                if (!usedSymbols.contains(assign.name.lexeme)) {
                    // Check if the assignment has side effects (function calls)
                    if (hasSideEffects(assign.value)) {
                        // Convert to expression statement without assignment
                        return new Stmt.Expression(assign.value);
                    }
                }
            }
        }
        
        return null; // Remove statement
    }
    
    /**
     * Check if an expression has side effects (function calls, method calls, etc.).
     */
    private boolean hasSideEffects(Expr expr) {
        return expr.accept(new Expr.Visitor<Boolean>() {
            @Override
            public Boolean visitCallExpr(Expr.Call expr) {
                return true; // Function calls have side effects
            }
            
            @Override
            public Boolean visitGetExpr(Expr.Get expr) {
                // Property access itself has no side effects, but the object might
                // Note: In Thorn, method calls are represented as Expr.Call with Expr.Get as callee
                // So this is just property access, not method invocation
                return hasSideEffects(expr.object);
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
            public Boolean visitAssignExpr(Expr.Assign expr) {
                return true; // Assignments have side effects
            }
            
            @Override
            public Boolean visitLogicalExpr(Expr.Logical expr) {
                return hasSideEffects(expr.left) || hasSideEffects(expr.right);
            }
            
            @Override
            public Boolean visitListExpr(Expr.ListExpr expr) {
                return expr.elements.stream().anyMatch(DeadCodeEliminator.this::hasSideEffects);
            }
            
            @Override
            public Boolean visitDictExpr(Expr.Dict expr) {
                return expr.keys.stream().anyMatch(DeadCodeEliminator.this::hasSideEffects) || 
                       expr.values.stream().anyMatch(DeadCodeEliminator.this::hasSideEffects);
            }
            
            @Override
            public Boolean visitIndexExpr(Expr.Index expr) {
                return hasSideEffects(expr.object) || hasSideEffects(expr.index);
            }
            
            @Override
            public Boolean visitIndexSetExpr(Expr.IndexSet expr) {
                return true; // Index assignments have side effects
            }
            
            @Override
            public Boolean visitSliceExpr(Expr.Slice expr) {
                return hasSideEffects(expr.object) || 
                       (expr.start != null && hasSideEffects(expr.start)) ||
                       (expr.end != null && hasSideEffects(expr.end));
            }
            
            @Override
            public Boolean visitMatchExpr(Expr.Match expr) {
                return true; // Match expressions could have side effects
            }
            
            @Override
            public Boolean visitSetExpr(Expr.Set expr) {
                return true; // Property assignments have side effects
            }
            
            @Override
            public Boolean visitLambdaExpr(Expr.Lambda expr) {
                return false; // Lambda creation itself has no side effects
            }
            
            // Simple expressions without side effects
            @Override public Boolean visitLiteralExpr(Expr.Literal expr) { return false; }
            @Override public Boolean visitVariableExpr(Expr.Variable expr) { return false; }
            @Override public Boolean visitThisExpr(Expr.This expr) { return false; }
            @Override public Boolean visitTypeExpr(Expr.Type expr) { return false; }
            @Override public Boolean visitGenericTypeExpr(Expr.GenericType expr) { return false; }
            @Override public Boolean visitFunctionTypeExpr(Expr.FunctionType expr) { return false; }
            @Override public Boolean visitArrayTypeExpr(Expr.ArrayType expr) { return false; }
        });
    }
    
    /**
     * Determine if a statement should be kept (not dead code).
     */
    private boolean shouldKeepStatement(Stmt stmt) {
        if (stmt instanceof Stmt.Function) {
            Stmt.Function func = (Stmt.Function) stmt;
            return usedSymbols.contains(func.name.lexeme);
        } else if (stmt instanceof Stmt.Var) {
            Stmt.Var var = (Stmt.Var) stmt;
            return usedSymbols.contains(var.name.lexeme);
        } else if (stmt instanceof Stmt.Class) {
            Stmt.Class cls = (Stmt.Class) stmt;
            return usedSymbols.contains(cls.name.lexeme);
        } else if (stmt instanceof Stmt.Import) {
            Stmt.Import imp = (Stmt.Import) stmt;
            if (imp.names != null) {
                for (Token name : imp.names) {
                    if (usedSymbols.contains(name.lexeme)) {
                        return true;
                    }
                }
                return false;
            } else {
                return usedSymbols.contains(imp.module.lexeme);
            }
        } else if (stmt instanceof Stmt.Expression) {
            Stmt.Expression exprStmt = (Stmt.Expression) stmt;
            // Check if this is an assignment to an unused variable
            if (exprStmt.expression instanceof Expr.Assign) {
                Expr.Assign assign = (Expr.Assign) exprStmt.expression;
                return usedSymbols.contains(assign.name.lexeme);
            }
            // Keep all other expression statements
            return true;
        } else {
            // Keep all other statements (control flow, etc.)
            return true;
        }
    }
}
// Stub methods for try-catch support - added by script
// These should be properly implemented when optimization support is needed
