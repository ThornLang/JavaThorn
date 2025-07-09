package com.thorn;

import java.util.*;

/**
 * Removes unreachable code based on control flow analysis.
 * This pass depends on ControlFlowAnalysisPass to identify unreachable blocks.
 */
public class UnreachableCodeEliminationPass extends OptimizationPass {
    
    private int blocksRemoved = 0;
    private int statementsRemoved = 0;
    
    @Override
    public String getName() {
        return "unreachable-code-elimination";
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
    public List<String> getDependencies() {
        return Arrays.asList("control-flow-analysis");
    }
    
    @Override
    public List<Stmt> optimize(List<Stmt> statements, OptimizationContext context) {
        if (context.isDebugMode()) {
            System.out.println("=== Unreachable Code Elimination Pass ===");
        }
        
        blocksRemoved = 0;
        statementsRemoved = 0;
        
        // Get control flow analysis results
        ControlFlowAnalysisPass.ControlFlowGraph cfg = 
            context.getCachedAnalysis("control-flow-graph", ControlFlowAnalysisPass.ControlFlowGraph.class);
        ControlFlowAnalysisPass.ReachabilityInfo reachability = 
            context.getCachedAnalysis("reachability-info", ControlFlowAnalysisPass.ReachabilityInfo.class);
        
        if (cfg == null || reachability == null) {
            if (context.isDebugMode()) {
                System.out.println("  No control flow analysis available - skipping");
            }
            return statements;
        }
        
        // Remove unreachable code
        UnreachableCodeRemover remover = new UnreachableCodeRemover(reachability, context);
        List<Stmt> optimized = remover.removeUnreachableCode(statements);
        
        if (context.isDebugMode()) {
            System.out.println("  Blocks removed: " + blocksRemoved);
            System.out.println("  Statements removed: " + statementsRemoved);
        }
        
        return optimized;
    }
    
    /**
     * Removes unreachable code from statements.
     */
    private class UnreachableCodeRemover {
        private final ControlFlowAnalysisPass.ReachabilityInfo reachability;
        private final OptimizationContext context;
        
        public UnreachableCodeRemover(ControlFlowAnalysisPass.ReachabilityInfo reachability,
                                     OptimizationContext context) {
            this.reachability = reachability;
            this.context = context;
        }
        
        public List<Stmt> removeUnreachableCode(List<Stmt> statements) {
            List<Stmt> result = new ArrayList<>();
            
            for (Stmt stmt : statements) {
                Stmt processed = processStatement(stmt);
                if (processed != null) {
                    result.add(processed);
                }
            }
            
            return result;
        }
        
        private Stmt processStatement(Stmt stmt) {
            return stmt.accept(new Stmt.Visitor<Stmt>() {
                @Override
                public Stmt visitExpressionStmt(Stmt.Expression stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitVarStmt(Stmt.Var stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitBlockStmt(Stmt.Block stmt) {
                    List<Stmt> filteredStatements = removeUnreachableCode(stmt.statements);
                    return new Stmt.Block(filteredStatements);
                }
                
                @Override
                public Stmt visitIfStmt(Stmt.If stmt) {
                    // Check if we can eliminate branches based on constant conditions
                    if (stmt.condition instanceof Expr.Literal) {
                        Object value = ((Expr.Literal) stmt.condition).value;
                        if (isTruthy(value)) {
                            // Condition is always true - keep only then branch
                            statementsRemoved++;
                            return processStatement(stmt.thenBranch);
                        } else {
                            // Condition is always false - keep only else branch
                            statementsRemoved++;
                            return stmt.elseBranch != null ? 
                                processStatement(stmt.elseBranch) : null;
                        }
                    }
                    
                    // Process both branches
                    Stmt thenBranch = processStatement(stmt.thenBranch);
                    Stmt elseBranch = stmt.elseBranch != null ? 
                        processStatement(stmt.elseBranch) : null;
                    
                    return new Stmt.If(stmt.condition, thenBranch, elseBranch);
                }
                
                @Override
                public Stmt visitWhileStmt(Stmt.While stmt) {
                    // Check if condition is always false
                    if (stmt.condition instanceof Expr.Literal) {
                        Object value = ((Expr.Literal) stmt.condition).value;
                        if (!isTruthy(value)) {
                            // Loop never executes - remove it
                            statementsRemoved++;
                            return null;
                        }
                    }
                    
                    Stmt body = processStatement(stmt.body);
                    return new Stmt.While(stmt.condition, body);
                }
                
                @Override
                public Stmt visitForStmt(Stmt.For stmt) {
                    Stmt body = processStatement(stmt.body);
                    return new Stmt.For(stmt.variable, stmt.iterable, body);
                }
                
                @Override
                public Stmt visitReturnStmt(Stmt.Return stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitFunctionStmt(Stmt.Function stmt) {
                    List<Stmt> body = removeUnreachableCode(stmt.body);
                    return new Stmt.Function(stmt.name, stmt.params, stmt.returnType, body);
                }
                
                @Override
                public Stmt visitClassStmt(Stmt.Class stmt) {
                    List<Stmt.Function> methods = new ArrayList<>();
                    for (Stmt.Function method : stmt.methods) {
                        Stmt.Function processed = (Stmt.Function) processStatement(method);
                        if (processed != null) {
                            methods.add(processed);
                        }
                    }
                    return new Stmt.Class(stmt.name, methods);
                }
                
                @Override
                public Stmt visitImportStmt(Stmt.Import stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitExportStmt(Stmt.Export stmt) {
                    Stmt declaration = processStatement(stmt.declaration);
                    return declaration != null ? new Stmt.Export(declaration) : null;
                }
            });
        }
        
        private boolean isTruthy(Object value) {
            if (value == null) return false;
            if (value instanceof Boolean) return (Boolean) value;
            return true;
        }
        
        /**
         * Removes statements that come after a return statement.
         */
        private List<Stmt> removeDeadStatementsAfterReturn(List<Stmt> statements) {
            List<Stmt> result = new ArrayList<>();
            
            for (Stmt stmt : statements) {
                result.add(stmt);
                
                // If this is a return statement, ignore everything after it
                if (stmt instanceof Stmt.Return) {
                    if (statements.indexOf(stmt) < statements.size() - 1) {
                        int remainingStatements = statements.size() - statements.indexOf(stmt) - 1;
                        statementsRemoved += remainingStatements;
                        if (context.isDebugMode()) {
                            System.out.println("  Removed " + remainingStatements + 
                                             " statements after return");
                        }
                    }
                    break;
                }
                
                // Also check for blocks that end with returns
                if (stmt instanceof Stmt.Block) {
                    Stmt.Block block = (Stmt.Block) stmt;
                    if (endsWithReturn(block.statements)) {
                        if (statements.indexOf(stmt) < statements.size() - 1) {
                            int remainingStatements = statements.size() - statements.indexOf(stmt) - 1;
                            statementsRemoved += remainingStatements;
                            if (context.isDebugMode()) {
                                System.out.println("  Removed " + remainingStatements + 
                                                 " statements after block with return");
                            }
                        }
                        break;
                    }
                }
            }
            
            return result;
        }
        
        private boolean endsWithReturn(List<Stmt> statements) {
            if (statements.isEmpty()) return false;
            
            Stmt last = statements.get(statements.size() - 1);
            if (last instanceof Stmt.Return) {
                return true;
            }
            
            if (last instanceof Stmt.Block) {
                Stmt.Block block = (Stmt.Block) last;
                return endsWithReturn(block.statements);
            }
            
            return false;
        }
    }
}