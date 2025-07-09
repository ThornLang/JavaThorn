package com.thorn;

import java.util.*;

/**
 * Optimization pass that simplifies conditional branches and eliminates dead code.
 * This pass optimizes if statements with constant conditions, removes unreachable
 * code after return statements, and simplifies boolean expressions.
 */
public class BranchOptimizationPass extends OptimizationPass {
    
    @Override
    public String getName() {
        return "branch-optimization";
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
        BranchOptimizer optimizer = new BranchOptimizer(context);
        return optimizer.optimize(statements);
    }
    
    @Override
    public int getEstimatedCost() {
        return 3; // Moderate cost
    }
    
    @Override
    public String getDescription() {
        return "Simplifies conditional branches and removes dead code after returns";
    }
    
    /**
     * Optimizer that performs branch optimization transformations.
     */
    private static class BranchOptimizer {
        private final OptimizationContext context;
        private int branchesEliminated = 0;
        private int deadCodeRemoved = 0;
        
        public BranchOptimizer(OptimizationContext context) {
            this.context = context;
        }
        
        public List<Stmt> optimize(List<Stmt> statements) {
            List<Stmt> result = optimizeStatementList(statements);
            
            if (context.isDebugMode()) {
                System.out.println("=== Branch Optimization ===");
                System.out.println("Branches eliminated: " + branchesEliminated);
                System.out.println("Dead code statements removed: " + deadCodeRemoved);
            }
            
            return result;
        }
        
        private List<Stmt> optimizeStatementList(List<Stmt> statements) {
            List<Stmt> result = new ArrayList<>();
            boolean foundReturn = false;
            
            for (Stmt stmt : statements) {
                if (foundReturn) {
                    // Dead code after return
                    deadCodeRemoved++;
                    continue;
                }
                
                Stmt optimized = optimizeStatement(stmt);
                if (optimized != null) {
                    result.add(optimized);
                    
                    // Check if this is a return statement
                    if (stmt instanceof Stmt.Return) {
                        foundReturn = true;
                    }
                }
            }
            
            return result;
        }
        
        private Stmt optimizeStatement(Stmt stmt) {
            return stmt.accept(new Stmt.Visitor<Stmt>() {
                @Override
                public Stmt visitIfStmt(Stmt.If stmt) {
                    // First check if condition is a constant
                    if (stmt.condition instanceof Expr.Literal) {
                        Expr.Literal literal = (Expr.Literal) stmt.condition;
                        
                        if (isTruthy(literal.value)) {
                            // Condition is always true, replace with then branch
                            branchesEliminated++;
                            return optimizeStatement(stmt.thenBranch);
                        } else {
                            // Condition is always false
                            branchesEliminated++;
                            if (stmt.elseBranch != null) {
                                return optimizeStatement(stmt.elseBranch);
                            } else {
                                // No else branch, remove entire if statement
                                return null;
                            }
                        }
                    }
                    
                    // Optimize condition and branches
                    Expr optimizedCondition = optimizeExpression(stmt.condition);
                    Stmt optimizedThen = optimizeStatement(stmt.thenBranch);
                    Stmt optimizedElse = stmt.elseBranch != null ? 
                        optimizeStatement(stmt.elseBranch) : null;
                    
                    // Check if optimization resulted in a constant condition
                    if (optimizedCondition instanceof Expr.Literal) {
                        Expr.Literal literal = (Expr.Literal) optimizedCondition;
                        if (isTruthy(literal.value)) {
                            branchesEliminated++;
                            return optimizedThen;
                        } else {
                            branchesEliminated++;
                            return optimizedElse;
                        }
                    }
                    
                    return new Stmt.If(optimizedCondition, optimizedThen, optimizedElse);
                }
                
                @Override
                public Stmt visitBlockStmt(Stmt.Block stmt) {
                    List<Stmt> optimizedStatements = optimizeStatementList(stmt.statements);
                    if (optimizedStatements.isEmpty()) {
                        return null; // Empty block can be removed
                    }
                    return new Stmt.Block(optimizedStatements);
                }
                
                @Override
                public Stmt visitWhileStmt(Stmt.While stmt) {
                    // Check for constant false condition
                    if (stmt.condition instanceof Expr.Literal) {
                        Expr.Literal literal = (Expr.Literal) stmt.condition;
                        if (!isTruthy(literal.value)) {
                            // Loop never executes
                            branchesEliminated++;
                            return null;
                        }
                    }
                    
                    Expr optimizedCondition = optimizeExpression(stmt.condition);
                    Stmt optimizedBody = optimizeStatement(stmt.body);
                    
                    return new Stmt.While(optimizedCondition, optimizedBody);
                }
                
                @Override
                public Stmt visitForStmt(Stmt.For stmt) {
                    Expr optimizedIterable = optimizeExpression(stmt.iterable);
                    Stmt optimizedBody = optimizeStatement(stmt.body);
                    return new Stmt.For(stmt.variable, optimizedIterable, optimizedBody);
                }
                
                @Override
                public Stmt visitExpressionStmt(Stmt.Expression stmt) {
                    Expr optimizedExpr = optimizeExpression(stmt.expression);
                    return new Stmt.Expression(optimizedExpr);
                }
                
                @Override
                public Stmt visitVarStmt(Stmt.Var stmt) {
                    Expr optimizedInitializer = stmt.initializer != null ? 
                        optimizeExpression(stmt.initializer) : null;
                    return new Stmt.Var(stmt.name, stmt.type, optimizedInitializer, stmt.isImmutable);
                }
                
                @Override
                public Stmt visitReturnStmt(Stmt.Return stmt) {
                    Expr optimizedValue = stmt.value != null ? 
                        optimizeExpression(stmt.value) : null;
                    return new Stmt.Return(stmt.keyword, optimizedValue);
                }
                
                @Override
                public Stmt visitFunctionStmt(Stmt.Function stmt) {
                    List<Stmt> optimizedBody = optimizeStatementList(stmt.body);
                    return new Stmt.Function(stmt.name, stmt.params, stmt.returnType, optimizedBody);
                }
                
                @Override
                public Stmt visitClassStmt(Stmt.Class stmt) {
                    List<Stmt.Function> optimizedMethods = new ArrayList<>();
                    for (Stmt.Function method : stmt.methods) {
                        optimizedMethods.add((Stmt.Function) optimizeStatement(method));
                    }
                    return new Stmt.Class(stmt.name, optimizedMethods);
                }
                
                @Override
                public Stmt visitImportStmt(Stmt.Import stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitExportStmt(Stmt.Export stmt) {
                    Stmt optimizedDeclaration = optimizeStatement(stmt.declaration);
                    return new Stmt.Export(optimizedDeclaration);
                }
            });
        }
        
        private Expr optimizeExpression(Expr expr) {
            return expr.accept(new Expr.Visitor<Expr>() {
                @Override
                public Expr visitBinaryExpr(Expr.Binary expr) {
                    Expr left = optimizeExpression(expr.left);
                    Expr right = optimizeExpression(expr.right);
                    
                    // Optimize boolean operations with constants
                    if (left instanceof Expr.Literal && right instanceof Expr.Literal) {
                        Expr.Literal leftLit = (Expr.Literal) left;
                        Expr.Literal rightLit = (Expr.Literal) right;
                        
                        switch (expr.operator.type) {
                            case EQUAL_EQUAL:
                                return new Expr.Literal(isEqual(leftLit.value, rightLit.value));
                            case BANG_EQUAL:
                                return new Expr.Literal(!isEqual(leftLit.value, rightLit.value));
                            case GREATER:
                                if (leftLit.value instanceof Double && rightLit.value instanceof Double) {
                                    return new Expr.Literal((Double) leftLit.value > (Double) rightLit.value);
                                }
                                break;
                            case GREATER_EQUAL:
                                if (leftLit.value instanceof Double && rightLit.value instanceof Double) {
                                    return new Expr.Literal((Double) leftLit.value >= (Double) rightLit.value);
                                }
                                break;
                            case LESS:
                                if (leftLit.value instanceof Double && rightLit.value instanceof Double) {
                                    return new Expr.Literal((Double) leftLit.value < (Double) rightLit.value);
                                }
                                break;
                            case LESS_EQUAL:
                                if (leftLit.value instanceof Double && rightLit.value instanceof Double) {
                                    return new Expr.Literal((Double) leftLit.value <= (Double) rightLit.value);
                                }
                                break;
                        }
                    }
                    
                    return new Expr.Binary(left, expr.operator, right);
                }
                
                @Override
                public Expr visitLogicalExpr(Expr.Logical expr) {
                    Expr left = optimizeExpression(expr.left);
                    Expr right = optimizeExpression(expr.right);
                    
                    // Short-circuit evaluation optimization
                    if (left instanceof Expr.Literal) {
                        Expr.Literal leftLit = (Expr.Literal) left;
                        
                        switch (expr.operator.type) {
                            case AND:
                                if (!isTruthy(leftLit.value)) {
                                    // false && anything = false
                                    return new Expr.Literal(false);
                                }
                                // true && x = x
                                return right;
                                
                            case OR:
                                if (isTruthy(leftLit.value)) {
                                    // true || anything = true
                                    return new Expr.Literal(true);
                                }
                                // false || x = x
                                return right;
                        }
                    }
                    
                    return new Expr.Logical(left, expr.operator, right);
                }
                
                @Override
                public Expr visitUnaryExpr(Expr.Unary expr) {
                    Expr right = optimizeExpression(expr.right);
                    
                    // Optimize boolean NOT with constants
                    if (expr.operator.type == TokenType.BANG && right instanceof Expr.Literal) {
                        Expr.Literal lit = (Expr.Literal) right;
                        return new Expr.Literal(!isTruthy(lit.value));
                    }
                    
                    return new Expr.Unary(expr.operator, right);
                }
                
                @Override
                public Expr visitCallExpr(Expr.Call expr) {
                    Expr callee = optimizeExpression(expr.callee);
                    List<Expr> args = new ArrayList<>();
                    for (Expr arg : expr.arguments) {
                        args.add(optimizeExpression(arg));
                    }
                    return new Expr.Call(callee, expr.paren, args);
                }
                
                @Override
                public Expr visitGroupingExpr(Expr.Grouping expr) {
                    return optimizeExpression(expr.expression);
                }
                
                @Override
                public Expr visitAssignExpr(Expr.Assign expr) {
                    Expr value = optimizeExpression(expr.value);
                    return new Expr.Assign(expr.name, value);
                }
                
                // Simple expressions - return as-is
                @Override public Expr visitLiteralExpr(Expr.Literal expr) { return expr; }
                @Override public Expr visitVariableExpr(Expr.Variable expr) { return expr; }
                @Override public Expr visitThisExpr(Expr.This expr) { return expr; }
                @Override public Expr visitListExpr(Expr.ListExpr expr) { return expr; }
                @Override public Expr visitDictExpr(Expr.Dict expr) { return expr; }
                @Override public Expr visitIndexExpr(Expr.Index expr) { return expr; }
                @Override public Expr visitGetExpr(Expr.Get expr) { return expr; }
                @Override public Expr visitSetExpr(Expr.Set expr) { return expr; }
                @Override public Expr visitIndexSetExpr(Expr.IndexSet expr) { return expr; }
                @Override public Expr visitLambdaExpr(Expr.Lambda expr) { return expr; }
                @Override public Expr visitMatchExpr(Expr.Match expr) { return expr; }
                @Override public Expr visitTypeExpr(Expr.Type expr) { return expr; }
                @Override public Expr visitGenericTypeExpr(Expr.GenericType expr) { return expr; }
                @Override public Expr visitFunctionTypeExpr(Expr.FunctionType expr) { return expr; }
                @Override public Expr visitArrayTypeExpr(Expr.ArrayType expr) { return expr; }
            });
        }
        
        private boolean isTruthy(Object value) {
            if (value == null) return false;
            if (value instanceof Boolean) return (Boolean) value;
            if (value instanceof Double) return ((Double) value) != 0.0;
            if (value instanceof String) return !((String) value).isEmpty();
            return true;
        }
        
        private boolean isEqual(Object a, Object b) {
            if (a == null && b == null) return true;
            if (a == null) return false;
            return a.equals(b);
        }
    }
}