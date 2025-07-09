package com.thorn;

import java.util.*;

/**
 * Optimization pass that propagates copy assignments to eliminate redundant variables.
 * This pass replaces uses of copied variables with their source values when safe.
 * Example: x = y; z = x + 1; becomes x = y; z = y + 1;
 */
public class CopyPropagationPass extends OptimizationPass {
    
    @Override
    public String getName() {
        return "copy-propagation";
    }
    
    @Override
    public PassType getType() {
        return PassType.TRANSFORMATION;
    }
    
    @Override
    public List<String> getDependencies() {
        return Arrays.asList("constant-folding");
    }
    
    @Override
    public boolean shouldRun(OptimizationContext context) {
        // Run at O1 and above
        return context.getLevel().includes(OptimizationLevel.O1);
    }
    
    @Override
    public List<Stmt> transform(List<Stmt> statements, OptimizationContext context) {
        CopyPropagator propagator = new CopyPropagator(context);
        return propagator.propagate(statements);
    }
    
    @Override
    public int getEstimatedCost() {
        return 4; // Moderate cost
    }
    
    @Override
    public String getDescription() {
        return "Propagates copy assignments to eliminate redundant variable accesses";
    }
    
    /**
     * Propagator that performs copy propagation transformations.
     */
    private static class CopyPropagator {
        private final OptimizationContext context;
        private final Map<String, String> copyMap; // Maps variable to its copy source
        private final Set<String> modifiedVariables; // Variables that have been modified
        private int copiesPropagated = 0;
        
        public CopyPropagator(OptimizationContext context) {
            this.context = context;
            this.copyMap = new HashMap<>();
            this.modifiedVariables = new HashSet<>();
        }
        
        public List<Stmt> propagate(List<Stmt> statements) {
            List<Stmt> result = new ArrayList<>();
            
            for (Stmt stmt : statements) {
                result.add(propagateInStatement(stmt));
            }
            
            if (context.isDebugMode()) {
                System.out.println("=== Copy Propagation ===");
                System.out.println("Copies propagated: " + copiesPropagated);
                if (!copyMap.isEmpty()) {
                    System.out.println("Copy relationships found: " + copyMap.size());
                }
            }
            
            return result;
        }
        
        private Stmt propagateInStatement(Stmt stmt) {
            return stmt.accept(new Stmt.Visitor<Stmt>() {
                @Override
                public Stmt visitVarStmt(Stmt.Var stmt) {
                    String varName = stmt.name.lexeme;
                    
                    if (stmt.initializer != null) {
                        // First propagate in the initializer
                        Expr propagatedInit = propagateInExpression(stmt.initializer);
                        
                        // Check if this is a simple copy assignment
                        if (propagatedInit instanceof Expr.Variable) {
                            Expr.Variable sourceVar = (Expr.Variable) propagatedInit;
                            String sourceName = sourceVar.name.lexeme;
                            
                            // Don't create cycles
                            if (!sourceName.equals(varName) && !isInCopyChain(sourceName, varName)) {
                                copyMap.put(varName, sourceName);
                            }
                        } else {
                            // Not a simple copy, clear any existing mapping
                            copyMap.remove(varName);
                        }
                        
                        return new Stmt.Var(stmt.name, stmt.type, propagatedInit, stmt.isImmutable);
                    }
                    
                    return stmt;
                }
                
                @Override
                public Stmt visitExpressionStmt(Stmt.Expression stmt) {
                    // Check for assignments that might invalidate copy relationships
                    if (stmt.expression instanceof Expr.Assign) {
                        Expr.Assign assign = (Expr.Assign) stmt.expression;
                        String varName = assign.name.lexeme;
                        
                        // First propagate in the value
                        Expr propagatedValue = propagateInExpression(assign.value);
                        
                        // Check if this is a simple copy assignment
                        if (propagatedValue instanceof Expr.Variable) {
                            Expr.Variable sourceVar = (Expr.Variable) propagatedValue;
                            String sourceName = sourceVar.name.lexeme;
                            
                            if (!sourceName.equals(varName) && !isInCopyChain(sourceName, varName)) {
                                copyMap.put(varName, sourceName);
                            }
                        } else {
                            // Variable is being assigned a non-variable value
                            copyMap.remove(varName);
                            modifiedVariables.add(varName);
                            invalidateCopiesOf(varName);
                        }
                        
                        Expr newAssign = new Expr.Assign(assign.name, propagatedValue);
                        return new Stmt.Expression(newAssign);
                    }
                    
                    Expr propagated = propagateInExpression(stmt.expression);
                    return new Stmt.Expression(propagated);
                }
                
                @Override
                public Stmt visitReturnStmt(Stmt.Return stmt) {
                    Expr propagatedValue = stmt.value != null ? 
                        propagateInExpression(stmt.value) : null;
                    return new Stmt.Return(stmt.keyword, propagatedValue);
                }
                
                @Override
                public Stmt visitIfStmt(Stmt.If stmt) {
                    Expr propagatedCondition = propagateInExpression(stmt.condition);
                    
                    // Save copy map state
                    Map<String, String> savedCopyMap = new HashMap<>(copyMap);
                    Set<String> savedModified = new HashSet<>(modifiedVariables);
                    
                    // Process then branch
                    Stmt propagatedThen = propagateInStatement(stmt.thenBranch);
                    
                    // Save then branch state
                    Map<String, String> thenCopyMap = new HashMap<>(copyMap);
                    Set<String> thenModified = new HashSet<>(modifiedVariables);
                    
                    // Restore state for else branch
                    copyMap.clear();
                    copyMap.putAll(savedCopyMap);
                    modifiedVariables.clear();
                    modifiedVariables.addAll(savedModified);
                    
                    Stmt propagatedElse = null;
                    if (stmt.elseBranch != null) {
                        propagatedElse = propagateInStatement(stmt.elseBranch);
                    }
                    
                    // Merge states conservatively - only keep copies valid in both branches
                    mergeCopyMaps(thenCopyMap, copyMap);
                    modifiedVariables.addAll(thenModified);
                    
                    return new Stmt.If(propagatedCondition, propagatedThen, propagatedElse);
                }
                
                @Override
                public Stmt visitWhileStmt(Stmt.While stmt) {
                    // Clear copy map for loop body (conservative approach)
                    Map<String, String> savedCopyMap = new HashMap<>(copyMap);
                    copyMap.clear();
                    
                    Expr propagatedCondition = propagateInExpression(stmt.condition);
                    Stmt propagatedBody = propagateInStatement(stmt.body);
                    
                    // Restore copy map
                    copyMap.clear();
                    copyMap.putAll(savedCopyMap);
                    
                    return new Stmt.While(propagatedCondition, propagatedBody);
                }
                
                @Override
                public Stmt visitForStmt(Stmt.For stmt) {
                    // Clear copy map for loop body
                    Map<String, String> savedCopyMap = new HashMap<>(copyMap);
                    copyMap.clear();
                    
                    Expr propagatedIterable = propagateInExpression(stmt.iterable);
                    Stmt propagatedBody = propagateInStatement(stmt.body);
                    
                    // Restore copy map
                    copyMap.clear();
                    copyMap.putAll(savedCopyMap);
                    
                    return new Stmt.For(stmt.variable, propagatedIterable, propagatedBody);
                }
                
                @Override
                public Stmt visitBlockStmt(Stmt.Block stmt) {
                    List<Stmt> propagatedStatements = new ArrayList<>();
                    
                    // Save copy map state
                    Map<String, String> savedCopyMap = new HashMap<>(copyMap);
                    
                    for (Stmt s : stmt.statements) {
                        propagatedStatements.add(propagateInStatement(s));
                    }
                    
                    // Restore copy map (block scope)
                    copyMap.clear();
                    copyMap.putAll(savedCopyMap);
                    
                    return new Stmt.Block(propagatedStatements);
                }
                
                @Override
                public Stmt visitFunctionStmt(Stmt.Function stmt) {
                    // Don't propagate across function boundaries
                    return stmt;
                }
                
                @Override
                public Stmt visitClassStmt(Stmt.Class stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitImportStmt(Stmt.Import stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitExportStmt(Stmt.Export stmt) {
                    Stmt propagatedDeclaration = propagateInStatement(stmt.declaration);
                    return new Stmt.Export(propagatedDeclaration);
                }
            });
        }
        
        private Expr propagateInExpression(Expr expr) {
            return expr.accept(new Expr.Visitor<Expr>() {
                @Override
                public Expr visitVariableExpr(Expr.Variable expr) {
                    String varName = expr.name.lexeme;
                    
                    // Follow copy chain to find ultimate source
                    String source = varName;
                    Set<String> visited = new HashSet<>();
                    
                    while (copyMap.containsKey(source) && !visited.contains(source)) {
                        visited.add(source);
                        source = copyMap.get(source);
                        copiesPropagated++;
                    }
                    
                    if (!source.equals(varName)) {
                        return new Expr.Variable(new Token(expr.name.type, source, null, expr.name.line));
                    }
                    
                    return expr;
                }
                
                @Override
                public Expr visitBinaryExpr(Expr.Binary expr) {
                    Expr left = propagateInExpression(expr.left);
                    Expr right = propagateInExpression(expr.right);
                    return new Expr.Binary(left, expr.operator, right);
                }
                
                @Override
                public Expr visitUnaryExpr(Expr.Unary expr) {
                    Expr right = propagateInExpression(expr.right);
                    return new Expr.Unary(expr.operator, right);
                }
                
                @Override
                public Expr visitCallExpr(Expr.Call expr) {
                    Expr callee = propagateInExpression(expr.callee);
                    List<Expr> args = new ArrayList<>();
                    for (Expr arg : expr.arguments) {
                        args.add(propagateInExpression(arg));
                    }
                    return new Expr.Call(callee, expr.paren, args);
                }
                
                @Override
                public Expr visitGroupingExpr(Expr.Grouping expr) {
                    Expr expression = propagateInExpression(expr.expression);
                    return new Expr.Grouping(expression);
                }
                
                @Override
                public Expr visitLogicalExpr(Expr.Logical expr) {
                    Expr left = propagateInExpression(expr.left);
                    Expr right = propagateInExpression(expr.right);
                    return new Expr.Logical(left, expr.operator, right);
                }
                
                @Override
                public Expr visitAssignExpr(Expr.Assign expr) {
                    Expr value = propagateInExpression(expr.value);
                    return new Expr.Assign(expr.name, value);
                }
                
                @Override
                public Expr visitListExpr(Expr.ListExpr expr) {
                    List<Expr> elements = new ArrayList<>();
                    for (Expr element : expr.elements) {
                        elements.add(propagateInExpression(element));
                    }
                    return new Expr.ListExpr(elements);
                }
                
                @Override
                public Expr visitIndexExpr(Expr.Index expr) {
                    Expr object = propagateInExpression(expr.object);
                    Expr index = propagateInExpression(expr.index);
                    return new Expr.Index(object, expr.bracket, index);
                }
                
                @Override
                public Expr visitGetExpr(Expr.Get expr) {
                    Expr object = propagateInExpression(expr.object);
                    return new Expr.Get(object, expr.name);
                }
                
                @Override
                public Expr visitSetExpr(Expr.Set expr) {
                    Expr object = propagateInExpression(expr.object);
                    Expr value = propagateInExpression(expr.value);
                    return new Expr.Set(object, expr.name, value);
                }
                
                @Override
                public Expr visitIndexSetExpr(Expr.IndexSet expr) {
                    Expr object = propagateInExpression(expr.object);
                    Expr index = propagateInExpression(expr.index);
                    Expr value = propagateInExpression(expr.value);
                    return new Expr.IndexSet(object, expr.bracket, index, value);
                }
                
                // Simple expressions - return as-is
                @Override public Expr visitLiteralExpr(Expr.Literal expr) { return expr; }
                @Override public Expr visitThisExpr(Expr.This expr) { return expr; }
                @Override public Expr visitLambdaExpr(Expr.Lambda expr) { return expr; }
                @Override public Expr visitMatchExpr(Expr.Match expr) { return expr; }
                @Override public Expr visitDictExpr(Expr.Dict expr) { return expr; }
                @Override public Expr visitTypeExpr(Expr.Type expr) { return expr; }
                @Override public Expr visitGenericTypeExpr(Expr.GenericType expr) { return expr; }
                @Override public Expr visitFunctionTypeExpr(Expr.FunctionType expr) { return expr; }
                @Override public Expr visitArrayTypeExpr(Expr.ArrayType expr) { return expr; }
            });
        }
        
        private boolean isInCopyChain(String var1, String var2) {
            Set<String> visited = new HashSet<>();
            String current = var1;
            
            while (copyMap.containsKey(current) && !visited.contains(current)) {
                visited.add(current);
                current = copyMap.get(current);
                if (current.equals(var2)) return true;
            }
            
            return false;
        }
        
        private void invalidateCopiesOf(String varName) {
            // Remove any mappings that point to this variable
            copyMap.entrySet().removeIf(entry -> entry.getValue().equals(varName));
        }
        
        private void mergeCopyMaps(Map<String, String> map1, Map<String, String> map2) {
            // Only keep copies that exist in both maps with same value
            Set<String> toRemove = new HashSet<>();
            
            for (Map.Entry<String, String> entry : copyMap.entrySet()) {
                String var = entry.getKey();
                String source = entry.getValue();
                
                if (!map1.containsKey(var) || !map1.get(var).equals(source)) {
                    toRemove.add(var);
                }
            }
            
            for (String var : toRemove) {
                copyMap.remove(var);
            }
        }
    }
}