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
        
        // First pass: collect all symbol definitions
        for (Stmt stmt : statements) {
            collectDefinitions(stmt);
        }
        
        // Second pass: collect all symbol usages
        for (Stmt stmt : statements) {
            collectUsages(stmt);
        }
        
        // Third pass: filter out unused symbols
        List<Stmt> optimized = new ArrayList<>();
        for (Stmt stmt : statements) {
            if (shouldKeepStatement(stmt)) {
                optimized.add(stmt);
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
        });
    }
    
    /**
     * Collect definitions from expressions (e.g., lambdas).
     */
    private void collectDefinitionsFromExpr(Expr expr) {
        expr.accept(new Expr.Visitor<Void>() {
            @Override
            public Void visitLambdaExpr(Expr.Lambda expr) {
                // Lambda parameters are local definitions
                for (Token param : expr.params) {
                    definedSymbols.add(param.lexeme);
                }
                
                // Process lambda body
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