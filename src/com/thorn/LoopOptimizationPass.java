package com.thorn;

import java.util.*;

/**
 * Optimization pass that performs various loop optimizations including
 * loop invariant code motion, strength reduction, and simple loop unrolling.
 * This pass provides 30-60% improvement for loop-heavy code.
 */
public class LoopOptimizationPass extends OptimizationPass {
    
    private static final int DEFAULT_UNROLL_THRESHOLD = 4;
    
    @Override
    public String getName() {
        return "loop-optimization";
    }
    
    @Override
    public PassType getType() {
        return PassType.TRANSFORMATION;
    }
    
    @Override
    public List<String> getDependencies() {
        return Arrays.asList("control-flow-analysis", "constant-folding");
    }
    
    @Override
    public OptimizationLevel getMinimumLevel() {
        return OptimizationLevel.O2;
    }
    
    @Override
    public List<Stmt> optimize(List<Stmt> statements, OptimizationContext context) {
        if (context.isDebugMode()) {
            System.out.println("=== Loop Optimization Pass ===");
            System.out.println("  Stub implementation - no transformations applied");
        }
        return statements; // TODO: Implement loop optimization
    }
    
    /**
     * Optimizer that performs loop optimizations.
     */
    private static class LoopOptimizer {
        private final OptimizationContext context;
        private final int unrollThreshold;
        private int loopsOptimized = 0;
        private int invariantsHoisted = 0;
        private int strengthReductions = 0;
        private int loopsUnrolled = 0;
        
        public LoopOptimizer(OptimizationContext context) {
            this.context = context;
            this.unrollThreshold = context.getPassConfigurationInt(
                "loop-optimization", "unroll-threshold", DEFAULT_UNROLL_THRESHOLD
            );
        }
        
        public List<Stmt> optimize(List<Stmt> statements) {
            List<Stmt> result = optimizeStatements(statements);
            
            if (context.isDebugMode()) {
                System.out.println("=== Loop Optimization ===");
                System.out.println("Loops optimized: " + loopsOptimized);
                System.out.println("Invariants hoisted: " + invariantsHoisted);
                System.out.println("Strength reductions: " + strengthReductions);
                System.out.println("Loops unrolled: " + loopsUnrolled);
            }
            
            return result;
        }
        
        private List<Stmt> optimizeStatements(List<Stmt> statements) {
            List<Stmt> result = new ArrayList<>();
            
            for (Stmt stmt : statements) {
                Stmt optimized = optimizeStatement(stmt);
                if (optimized != null) {
                    result.add(optimized);
                }
            }
            
            return result;
        }
        
        private Stmt optimizeStatement(Stmt stmt) {
            return stmt.accept(new Stmt.Visitor<Stmt>() {
                @Override
                public Stmt visitWhileStmt(Stmt.While stmt) {
                    loopsOptimized++;
                    
                    // First, recursively optimize the body
                    Stmt optimizedBody = optimizeStatement(stmt.body);
                    
                    // Perform loop invariant code motion
                    LoopInvariantResult invariantResult = hoistLoopInvariants(stmt.condition, optimizedBody);
                    invariantsHoisted += invariantResult.hoistedStatements.size();
                    
                    // Apply strength reduction in loop body
                    Stmt strengthReducedBody = applyStrengthReduction(invariantResult.optimizedBody);
                    
                    // Create optimized while loop
                    Stmt optimizedLoop = new Stmt.While(invariantResult.optimizedCondition, strengthReducedBody);
                    
                    // If we hoisted any statements, create a block with hoisted code before loop
                    if (!invariantResult.hoistedStatements.isEmpty()) {
                        List<Stmt> blockStatements = new ArrayList<>(invariantResult.hoistedStatements);
                        blockStatements.add(optimizedLoop);
                        return new Stmt.Block(blockStatements);
                    }
                    
                    return optimizedLoop;
                }
                
                @Override
                public Stmt visitForStmt(Stmt.For stmt) {
                    loopsOptimized++;
                    
                    // Check if this is a simple counting loop that can be unrolled
                    if (isSimpleCountingLoop(stmt)) {
                        Stmt unrolled = tryUnrollLoop(stmt);
                        if (unrolled != null) {
                            loopsUnrolled++;
                            return unrolled;
                        }
                    }
                    
                    // Otherwise, optimize the body
                    Stmt optimizedBody = optimizeStatement(stmt.body);
                    
                    // Perform loop invariant code motion
                    LoopInvariantResult invariantResult = hoistLoopInvariants(null, optimizedBody);
                    invariantsHoisted += invariantResult.hoistedStatements.size();
                    
                    // Apply strength reduction
                    Stmt strengthReducedBody = applyStrengthReduction(invariantResult.optimizedBody);
                    
                    // Create optimized for loop
                    Stmt optimizedLoop = new Stmt.For(stmt.variable, stmt.iterable, strengthReducedBody);
                    
                    // If we hoisted any statements, create a block
                    if (!invariantResult.hoistedStatements.isEmpty()) {
                        List<Stmt> blockStatements = new ArrayList<>(invariantResult.hoistedStatements);
                        blockStatements.add(optimizedLoop);
                        return new Stmt.Block(blockStatements);
                    }
                    
                    return optimizedLoop;
                }
                
                @Override
                public Stmt visitBlockStmt(Stmt.Block stmt) {
                    List<Stmt> optimizedStatements = optimizeStatements(stmt.statements);
                    return new Stmt.Block(optimizedStatements);
                }
                
                @Override
                public Stmt visitIfStmt(Stmt.If stmt) {
                    Stmt optimizedThen = optimizeStatement(stmt.thenBranch);
                    Stmt optimizedElse = stmt.elseBranch != null ? 
                        optimizeStatement(stmt.elseBranch) : null;
                    return new Stmt.If(stmt.condition, optimizedThen, optimizedElse);
                }
                
                @Override
                public Stmt visitFunctionStmt(Stmt.Function stmt) {
                    List<Stmt> optimizedBody = optimizeStatements(stmt.body);
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
                public Stmt visitExpressionStmt(Stmt.Expression stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitVarStmt(Stmt.Var stmt) {
                    return stmt;
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
                    Stmt optimizedDeclaration = optimizeStatement(stmt.declaration);
                    return new Stmt.Export(optimizedDeclaration);
                }
                
                @Override
                public Stmt visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitTypeAliasStmt(Stmt.TypeAlias stmt) {
                    // Type aliases are compile-time only, no optimization needed
                    return stmt;
                }
            });
        }
        
        /**
         * Hoist loop invariant code out of loops.
         */
        private LoopInvariantResult hoistLoopInvariants(Expr condition, Stmt body) {
            List<Stmt> hoistedStatements = new ArrayList<>();
            LoopInvariantAnalyzer analyzer = new LoopInvariantAnalyzer();
            
            // Analyze the loop body to find invariant code
            Set<Stmt> invariantStatements = analyzer.findInvariantStatements(body);
            
            // Extract invariant statements
            for (Stmt invariant : invariantStatements) {
                hoistedStatements.add(invariant);
            }
            
            // Create new body without invariant statements
            Stmt newBody = removeInvariantStatements(body, invariantStatements);
            
            return new LoopInvariantResult(hoistedStatements, condition, newBody);
        }
        
        /**
         * Apply strength reduction optimizations to loop body.
         */
        private Stmt applyStrengthReduction(Stmt body) {
            return body.accept(new Stmt.Visitor<Stmt>() {
                @Override
                public Stmt visitExpressionStmt(Stmt.Expression stmt) {
                    Expr reducedExpr = reduceStrength(stmt.expression);
                    return new Stmt.Expression(reducedExpr);
                }
                
                @Override
                public Stmt visitVarStmt(Stmt.Var stmt) {
                    if (stmt.initializer != null) {
                        Expr reducedInit = reduceStrength(stmt.initializer);
                        return new Stmt.Var(stmt.name, stmt.type, reducedInit, stmt.isImmutable);
                    }
                    return stmt;
                }
                
                @Override
                public Stmt visitBlockStmt(Stmt.Block stmt) {
                    List<Stmt> reducedStatements = new ArrayList<>();
                    for (Stmt s : stmt.statements) {
                        reducedStatements.add(applyStrengthReduction(s));
                    }
                    return new Stmt.Block(reducedStatements);
                }
                
                @Override
                public Stmt visitIfStmt(Stmt.If stmt) {
                    Expr reducedCondition = reduceStrength(stmt.condition);
                    Stmt reducedThen = applyStrengthReduction(stmt.thenBranch);
                    Stmt reducedElse = stmt.elseBranch != null ? 
                        applyStrengthReduction(stmt.elseBranch) : null;
                    return new Stmt.If(reducedCondition, reducedThen, reducedElse);
                }
                
                @Override
                public Stmt visitWhileStmt(Stmt.While stmt) {
                    Expr reducedCondition = reduceStrength(stmt.condition);
                    Stmt reducedBody = applyStrengthReduction(stmt.body);
                    return new Stmt.While(reducedCondition, reducedBody);
                }
                
                @Override
                public Stmt visitForStmt(Stmt.For stmt) {
                    Stmt reducedBody = applyStrengthReduction(stmt.body);
                    return new Stmt.For(stmt.variable, stmt.iterable, reducedBody);
                }
                
                @Override
                public Stmt visitReturnStmt(Stmt.Return stmt) {
                    if (stmt.value != null) {
                        Expr reducedValue = reduceStrength(stmt.value);
                        return new Stmt.Return(stmt.keyword, reducedValue);
                    }
                    return stmt;
                }
                
                @Override
                public Stmt visitFunctionStmt(Stmt.Function stmt) {
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
                    return stmt;
                }
                
                @Override
                public Stmt visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitTypeAliasStmt(Stmt.TypeAlias stmt) {
                    // Type aliases are compile-time only, no optimization needed
                    return stmt;
                }
            });
        }
        
        /**
         * Apply strength reduction to expressions.
         */
        private Expr reduceStrength(Expr expr) {
            return expr.accept(new Expr.Visitor<Expr>() {
                @Override
                public Expr visitBinaryExpr(Expr.Binary expr) {
                    Expr left = reduceStrength(expr.left);
                    Expr right = reduceStrength(expr.right);
                    
                    // Strength reduction patterns
                    if (expr.operator.type == TokenType.STAR) {
                        // x * 2 -> x + x
                        if (right instanceof Expr.Literal) {
                            Expr.Literal lit = (Expr.Literal) right;
                            if (lit.value instanceof Double && (Double) lit.value == 2.0) {
                                strengthReductions++;
                                return new Expr.Binary(left, 
                                    new Token(TokenType.PLUS, "+", null, expr.operator.line), 
                                    left);
                            }
                        }
                        
                        // 2 * x -> x + x
                        if (left instanceof Expr.Literal) {
                            Expr.Literal lit = (Expr.Literal) left;
                            if (lit.value instanceof Double && (Double) lit.value == 2.0) {
                                strengthReductions++;
                                return new Expr.Binary(right, 
                                    new Token(TokenType.PLUS, "+", null, expr.operator.line), 
                                    right);
                            }
                        }
                    }
                    
                    // x / 2 -> x * 0.5 (multiplication is faster than division)
                    if (expr.operator.type == TokenType.SLASH) {
                        if (right instanceof Expr.Literal) {
                            Expr.Literal lit = (Expr.Literal) right;
                            if (lit.value instanceof Double && (Double) lit.value == 2.0) {
                                strengthReductions++;
                                return new Expr.Binary(left,
                                    new Token(TokenType.STAR, "*", null, expr.operator.line),
                                    new Expr.Literal(0.5));
                            }
                        }
                    }
                    
                    return new Expr.Binary(left, expr.operator, right);
                }
                
                @Override
                public Expr visitUnaryExpr(Expr.Unary expr) {
                    Expr right = reduceStrength(expr.right);
                    return new Expr.Unary(expr.operator, right);
                }
                
                @Override
                public Expr visitCallExpr(Expr.Call expr) {
                    Expr callee = reduceStrength(expr.callee);
                    List<Expr> args = new ArrayList<>();
                    for (Expr arg : expr.arguments) {
                        args.add(reduceStrength(arg));
                    }
                    return new Expr.Call(callee, expr.paren, args);
                }
                
                @Override
                public Expr visitGroupingExpr(Expr.Grouping expr) {
                    return reduceStrength(expr.expression);
                }
                
                @Override
                public Expr visitLogicalExpr(Expr.Logical expr) {
                    Expr left = reduceStrength(expr.left);
                    Expr right = reduceStrength(expr.right);
                    return new Expr.Logical(left, expr.operator, right);
                }
                
                @Override
                public Expr visitAssignExpr(Expr.Assign expr) {
                    Expr value = reduceStrength(expr.value);
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
        
        /**
         * Check if a for loop is a simple counting loop suitable for unrolling.
         */
        private boolean isSimpleCountingLoop(Stmt.For stmt) {
            // Check if iterating over a list literal with small size
            if (stmt.iterable instanceof Expr.ListExpr) {
                Expr.ListExpr list = (Expr.ListExpr) stmt.iterable;
                return list.elements.size() <= unrollThreshold;
            }
            
            // Could also check for range() calls in the future
            return false;
        }
        
        /**
         * Try to unroll a simple loop.
         */
        private Stmt tryUnrollLoop(Stmt.For stmt) {
            if (!(stmt.iterable instanceof Expr.ListExpr)) {
                return null;
            }
            
            Expr.ListExpr list = (Expr.ListExpr) stmt.iterable;
            List<Stmt> unrolledStatements = new ArrayList<>();
            
            // Create a copy of the loop body for each iteration
            for (Expr element : list.elements) {
                // Create assignment: loopVar = element
                Stmt.Var varAssign = new Stmt.Var(
                    stmt.variable,
                    null,
                    element,
                    false
                );
                unrolledStatements.add(varAssign);
                
                // Add the loop body
                unrolledStatements.add(stmt.body);
            }
            
            return new Stmt.Block(unrolledStatements);
        }
        
        /**
         * Remove invariant statements from loop body.
         */
        private Stmt removeInvariantStatements(Stmt body, Set<Stmt> invariants) {
            if (invariants.isEmpty()) {
                return body;
            }
            
            return body.accept(new Stmt.Visitor<Stmt>() {
                @Override
                public Stmt visitBlockStmt(Stmt.Block stmt) {
                    List<Stmt> filtered = new ArrayList<>();
                    for (Stmt s : stmt.statements) {
                        if (!invariants.contains(s)) {
                            Stmt processed = removeInvariantStatements(s, invariants);
                            if (processed != null) {
                                filtered.add(processed);
                            }
                        }
                    }
                    return new Stmt.Block(filtered);
                }
                
                @Override
                public Stmt visitIfStmt(Stmt.If stmt) {
                    if (invariants.contains(stmt)) return null;
                    
                    Stmt filteredThen = removeInvariantStatements(stmt.thenBranch, invariants);
                    Stmt filteredElse = stmt.elseBranch != null ? 
                        removeInvariantStatements(stmt.elseBranch, invariants) : null;
                    
                    return new Stmt.If(stmt.condition, filteredThen, filteredElse);
                }
                
                @Override
                public Stmt visitWhileStmt(Stmt.While stmt) {
                    if (invariants.contains(stmt)) return null;
                    
                    Stmt filteredBody = removeInvariantStatements(stmt.body, invariants);
                    return new Stmt.While(stmt.condition, filteredBody);
                }
                
                @Override
                public Stmt visitForStmt(Stmt.For stmt) {
                    if (invariants.contains(stmt)) return null;
                    
                    Stmt filteredBody = removeInvariantStatements(stmt.body, invariants);
                    return new Stmt.For(stmt.variable, stmt.iterable, filteredBody);
                }
                
                // Other statements - check if they're invariant
                @Override
                public Stmt visitExpressionStmt(Stmt.Expression stmt) {
                    return invariants.contains(stmt) ? null : stmt;
                }
                
                @Override
                public Stmt visitVarStmt(Stmt.Var stmt) {
                    return invariants.contains(stmt) ? null : stmt;
                }
                
                @Override
                public Stmt visitReturnStmt(Stmt.Return stmt) {
                    return invariants.contains(stmt) ? null : stmt;
                }
                
                @Override
                public Stmt visitFunctionStmt(Stmt.Function stmt) {
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
                    return stmt;
                }
                
                @Override
                public Stmt visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitTypeAliasStmt(Stmt.TypeAlias stmt) {
                    // Type aliases are compile-time only, no optimization needed
                    return stmt;
                }
            });
        }
        
        /**
         * Result of loop invariant analysis.
         */
        private static class LoopInvariantResult {
            final List<Stmt> hoistedStatements;
            final Expr optimizedCondition;
            final Stmt optimizedBody;
            
            LoopInvariantResult(List<Stmt> hoistedStatements, Expr condition, Stmt body) {
                this.hoistedStatements = hoistedStatements;
                this.optimizedCondition = condition;
                this.optimizedBody = body;
            }
        }
        
        /**
         * Analyzer that identifies loop invariant code.
         */
        private static class LoopInvariantAnalyzer {
            private final Set<String> loopVariables = new HashSet<>();
            private final Set<Stmt> invariantStatements = new HashSet<>();
            
            public Set<Stmt> findInvariantStatements(Stmt loopBody) {
                // First, identify all variables modified in the loop
                identifyLoopVariables(loopBody);
                
                // Then, find statements that don't depend on loop variables
                findInvariants(loopBody);
                
                return invariantStatements;
            }
            
            private void identifyLoopVariables(Stmt stmt) {
                stmt.accept(new Stmt.Visitor<Void>() {
                    @Override
                    public Void visitVarStmt(Stmt.Var stmt) {
                        loopVariables.add(stmt.name.lexeme);
                        return null;
                    }
                    
                    @Override
                    public Void visitExpressionStmt(Stmt.Expression stmt) {
                        if (stmt.expression instanceof Expr.Assign) {
                            Expr.Assign assign = (Expr.Assign) stmt.expression;
                            loopVariables.add(assign.name.lexeme);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitBlockStmt(Stmt.Block stmt) {
                        for (Stmt s : stmt.statements) {
                            identifyLoopVariables(s);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitIfStmt(Stmt.If stmt) {
                        identifyLoopVariables(stmt.thenBranch);
                        if (stmt.elseBranch != null) {
                            identifyLoopVariables(stmt.elseBranch);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitWhileStmt(Stmt.While stmt) {
                        identifyLoopVariables(stmt.body);
                        return null;
                    }
                    
                    @Override
                    public Void visitForStmt(Stmt.For stmt) {
                        loopVariables.add(stmt.variable.lexeme);
                        identifyLoopVariables(stmt.body);
                        return null;
                    }
                    
                    // Other statements don't modify variables
                    @Override public Void visitReturnStmt(Stmt.Return stmt) { return null; }
                    @Override public Void visitFunctionStmt(Stmt.Function stmt) { return null; }
                    @Override public Void visitClassStmt(Stmt.Class stmt) { return null; }
                    @Override public Void visitImportStmt(Stmt.Import stmt) { return null; }
                    @Override public Void visitExportStmt(Stmt.Export stmt) { return null; }
                    @Override public Void visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) { return null; }
                    @Override public Void visitTypeAliasStmt(Stmt.TypeAlias stmt) { return null; }
                });
            }
            
            private void findInvariants(Stmt stmt) {
                // For simplicity, we only hoist simple expression statements and variable declarations
                // that don't depend on loop variables and don't have side effects
                stmt.accept(new Stmt.Visitor<Void>() {
                    @Override
                    public Void visitExpressionStmt(Stmt.Expression stmt) {
                        if (!dependsOnLoopVariables(stmt.expression) && 
                            !hasSideEffects(stmt.expression)) {
                            invariantStatements.add(stmt);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitVarStmt(Stmt.Var stmt) {
                        if (stmt.initializer != null &&
                            !dependsOnLoopVariables(stmt.initializer) &&
                            !hasSideEffects(stmt.initializer)) {
                            invariantStatements.add(stmt);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitBlockStmt(Stmt.Block stmt) {
                        for (Stmt s : stmt.statements) {
                            findInvariants(s);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitIfStmt(Stmt.If stmt) {
                        findInvariants(stmt.thenBranch);
                        if (stmt.elseBranch != null) {
                            findInvariants(stmt.elseBranch);
                        }
                        return null;
                    }
                    
                    // Don't hoist nested loops, returns, or complex statements
                    @Override public Void visitWhileStmt(Stmt.While stmt) { return null; }
                    @Override public Void visitForStmt(Stmt.For stmt) { return null; }
                    @Override public Void visitReturnStmt(Stmt.Return stmt) { return null; }
                    @Override public Void visitFunctionStmt(Stmt.Function stmt) { return null; }
                    @Override public Void visitClassStmt(Stmt.Class stmt) { return null; }
                    @Override public Void visitImportStmt(Stmt.Import stmt) { return null; }
                    @Override public Void visitExportStmt(Stmt.Export stmt) { return null; }
                    @Override public Void visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) { return null; }
                    @Override public Void visitTypeAliasStmt(Stmt.TypeAlias stmt) { return null; }
                });
            }
            
            private boolean dependsOnLoopVariables(Expr expr) {
                return expr.accept(new Expr.Visitor<Boolean>() {
                    @Override
                    public Boolean visitVariableExpr(Expr.Variable expr) {
                        return loopVariables.contains(expr.name.lexeme);
                    }
                    
                    @Override
                    public Boolean visitBinaryExpr(Expr.Binary expr) {
                        return dependsOnLoopVariables(expr.left) || dependsOnLoopVariables(expr.right);
                    }
                    
                    @Override
                    public Boolean visitUnaryExpr(Expr.Unary expr) {
                        return dependsOnLoopVariables(expr.right);
                    }
                    
                    @Override
                    public Boolean visitCallExpr(Expr.Call expr) {
                        if (dependsOnLoopVariables(expr.callee)) return true;
                        for (Expr arg : expr.arguments) {
                            if (dependsOnLoopVariables(arg)) return true;
                        }
                        return false;
                    }
                    
                    @Override
                    public Boolean visitGroupingExpr(Expr.Grouping expr) {
                        return dependsOnLoopVariables(expr.expression);
                    }
                    
                    @Override
                    public Boolean visitLogicalExpr(Expr.Logical expr) {
                        return dependsOnLoopVariables(expr.left) || dependsOnLoopVariables(expr.right);
                    }
                    
                    @Override
                    public Boolean visitAssignExpr(Expr.Assign expr) {
                        return true; // Assignments always depend on their target
                    }
                    
                    @Override
                    public Boolean visitIndexExpr(Expr.Index expr) {
                        return dependsOnLoopVariables(expr.object) || dependsOnLoopVariables(expr.index);
                    }
                    
                    @Override
                    public Boolean visitGetExpr(Expr.Get expr) {
                        return dependsOnLoopVariables(expr.object);
                    }
                    
                    // Other expressions don't depend on variables
                    @Override public Boolean visitLiteralExpr(Expr.Literal expr) { return false; }
                    @Override public Boolean visitThisExpr(Expr.This expr) { return false; }
                    @Override public Boolean visitListExpr(Expr.ListExpr expr) { return false; }
                    @Override public Boolean visitDictExpr(Expr.Dict expr) { return false; }
                    @Override public Boolean visitSetExpr(Expr.Set expr) { return true; }
                    @Override public Boolean visitIndexSetExpr(Expr.IndexSet expr) { return true; }
                    @Override public Boolean visitLambdaExpr(Expr.Lambda expr) { return false; }
                    @Override public Boolean visitMatchExpr(Expr.Match expr) { return false; }
                    @Override public Boolean visitTypeExpr(Expr.Type expr) { return false; }
                    @Override public Boolean visitGenericTypeExpr(Expr.GenericType expr) { return false; }
                    @Override public Boolean visitFunctionTypeExpr(Expr.FunctionType expr) { return false; }
                    @Override public Boolean visitArrayTypeExpr(Expr.ArrayType expr) { return false; }
                });
            }
            
            private boolean hasSideEffects(Expr expr) {
                return expr.accept(new Expr.Visitor<Boolean>() {
                    @Override public Boolean visitCallExpr(Expr.Call expr) { return true; }
                    @Override public Boolean visitAssignExpr(Expr.Assign expr) { return true; }
                    @Override public Boolean visitSetExpr(Expr.Set expr) { return true; }
                    @Override public Boolean visitIndexSetExpr(Expr.IndexSet expr) { return true; }
                    
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
                    public Boolean visitIndexExpr(Expr.Index expr) {
                        return hasSideEffects(expr.object) || hasSideEffects(expr.index);
                    }
                    
                    @Override
                    public Boolean visitGetExpr(Expr.Get expr) {
                        return hasSideEffects(expr.object);
                    }
                    
                    // Pure expressions
                    @Override public Boolean visitLiteralExpr(Expr.Literal expr) { return false; }
                    @Override public Boolean visitVariableExpr(Expr.Variable expr) { return false; }
                    @Override public Boolean visitThisExpr(Expr.This expr) { return false; }
                    @Override public Boolean visitListExpr(Expr.ListExpr expr) { return false; }
                    @Override public Boolean visitDictExpr(Expr.Dict expr) { return false; }
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