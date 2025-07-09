package com.thorn;

import java.util.*;
import static com.thorn.TokenType.*;

/**
 * Optimization pass that eliminates common subexpressions.
 * This pass identifies repeated expressions and replaces them with temporary variables.
 */
public class CommonSubexpressionEliminationPass extends OptimizationPass {
    
    @Override
    public String getName() {
        return "common-subexpression-elimination";
    }
    
    @Override
    public PassType getType() {
        return PassType.TRANSFORMATION;
    }
    
    @Override
    public List<String> getDependencies() {
        return Arrays.asList("control-flow-analysis");
    }
    
    @Override
    public boolean shouldRun(OptimizationContext context) {
        // Run at O2 and above
        return context.getLevel().includes(OptimizationLevel.O2);
    }
    
    @Override
    public List<Stmt> transform(List<Stmt> statements, OptimizationContext context) {
        CSETransformer transformer = new CSETransformer(context);
        return transformer.transform(statements);
    }
    
    @Override
    public int getEstimatedCost() {
        return 6; // Moderate-high cost
    }
    
    @Override
    public String getDescription() {
        return "Eliminates common subexpressions by introducing temporary variables";
    }
    
    /**
     * Transformer that performs common subexpression elimination.
     */
    private static class CSETransformer {
        private final OptimizationContext context;
        private final Map<String, Expr> expressions;
        private final Map<String, Integer> expressionCounts;
        private final Map<String, String> tempVariables;
        private final List<Stmt> tempDeclarations;
        private int tempCounter;
        
        public CSETransformer(OptimizationContext context) {
            this.context = context;
            this.expressions = new HashMap<>();
            this.expressionCounts = new HashMap<>();
            this.tempVariables = new HashMap<>();
            this.tempDeclarations = new ArrayList<>();
            this.tempCounter = 0;
        }
        
        public List<Stmt> transform(List<Stmt> statements) {
            List<Stmt> result = new ArrayList<>();
            
            // Process each statement individually to avoid scoping issues
            for (Stmt stmt : statements) {
                // Reset for each statement
                expressions.clear();
                expressionCounts.clear();
                tempVariables.clear();
                tempCounter = 0;
                
                // Collect expressions within this statement
                collectExpressions(stmt);
                
                // Create temp variables for expressions that appear more than once within this statement
                Map<String, Expr> commonExpressions = new HashMap<>();
                for (Map.Entry<String, Expr> entry : expressions.entrySet()) {
                    if (expressionCounts.get(entry.getKey()) > 1) {
                        commonExpressions.put(entry.getKey(), entry.getValue());
                    }
                }
                
                // If we found common subexpressions in this statement
                if (!commonExpressions.isEmpty()) {
                    if (context.isDebugMode()) {
                        System.out.println("  Found " + commonExpressions.size() + " common expressions in statement");
                        for (Map.Entry<String, Expr> entry : commonExpressions.entrySet()) {
                            System.out.println("    " + entry.getKey() + " (count: " + expressionCounts.get(entry.getKey()) + ")");
                        }
                    }
                    
                    // Create temp variables  
                    for (Map.Entry<String, Expr> entry : commonExpressions.entrySet()) {
                        String tempVarName = "cse_temp_" + tempCounter++;
                        tempVariables.put(entry.getKey(), tempVarName);
                    }
                    
                    // Transform the statement
                    Stmt transformedStmt = transformStatement(stmt);
                    
                    // If this is a block statement, add temp declarations inside it
                    if (transformedStmt instanceof Stmt.Block) {
                        Stmt.Block block = (Stmt.Block) transformedStmt;
                        List<Stmt> blockStatements = new ArrayList<>();
                        
                        // Add temp variable declarations
                        for (Map.Entry<String, Expr> entry : commonExpressions.entrySet()) {
                            String tempVarName = tempVariables.get(entry.getKey());
                            Token tempName = new Token(TokenType.IDENTIFIER, tempVarName, null, 0);
                            Stmt.Var tempDecl = new Stmt.Var(tempName, null, entry.getValue(), false);
                            blockStatements.add(tempDecl);
                        }
                        
                        // Add transformed statements
                        blockStatements.addAll(block.statements);
                        result.add(new Stmt.Block(blockStatements));
                        
                        if (context.isDebugMode()) {
                            System.out.println("    Created block with " + blockStatements.size() + " statements (including temps)");
                        }
                    } else if (transformedStmt instanceof Stmt.If) {
                        // For if statements, we need to transform the branches  
                        Stmt.If ifStmt = (Stmt.If) transformedStmt;
                        if (ifStmt.thenBranch instanceof Stmt.Block) {
                            Stmt.Block block = (Stmt.Block) ifStmt.thenBranch;
                            List<Stmt> blockStatements = new ArrayList<>();
                            
                            // Add temp variable declarations
                            for (Map.Entry<String, Expr> entry : commonExpressions.entrySet()) {
                                String tempVarName = tempVariables.get(entry.getKey());
                                Token tempName = new Token(TokenType.IDENTIFIER, tempVarName, null, 0);
                                Stmt.Var tempDecl = new Stmt.Var(tempName, null, entry.getValue(), false);
                                blockStatements.add(tempDecl);
                            }
                            
                            // Add transformed statements
                            blockStatements.addAll(block.statements);
                            Stmt.Block newBlock = new Stmt.Block(blockStatements);
                            result.add(new Stmt.If(ifStmt.condition, newBlock, ifStmt.elseBranch));
                            
                            if (context.isDebugMode()) {
                                System.out.println("    Transformed if statement with " + blockStatements.size() + " statements in then branch");
                            }
                        } else {
                            result.add(transformedStmt);
                        }
                    } else {
                        // For non-block statements, we need to wrap them in a block with temp declarations
                        List<Stmt> blockStatements = new ArrayList<>();
                        
                        // Add temp variable declarations
                        for (Map.Entry<String, Expr> entry : commonExpressions.entrySet()) {
                            String tempVarName = tempVariables.get(entry.getKey());
                            Token tempName = new Token(TokenType.IDENTIFIER, tempVarName, null, 0);
                            Stmt.Var tempDecl = new Stmt.Var(tempName, null, entry.getValue(), false);
                            blockStatements.add(tempDecl);
                        }
                        
                        // Add the transformed statement
                        blockStatements.add(transformedStmt);
                        
                        // Wrap in a block
                        result.add(new Stmt.Block(blockStatements));
                        
                        if (context.isDebugMode()) {
                            System.out.println("    Wrapped non-block statement in block with " + tempVariables.size() + " temp declarations");
                        }
                    }
                } else {
                    // No common subexpressions, just add the original statement
                    result.add(stmt);
                }
            }
            
            if (context.isDebugMode()) {
                System.out.println("=== Common Subexpression Elimination ===");
                System.out.println("Processed " + statements.size() + " statements");
                if (result.size() > statements.size()) {
                    System.out.println("Created temp variables in some statements");
                }
            }
            
            return result;
        }
        
        /**
         * Collect expressions and count their occurrences.
         */
        private void collectExpressions(Stmt stmt) {
            stmt.accept(new Stmt.Visitor<Void>() {
                @Override
                public Void visitExpressionStmt(Stmt.Expression stmt) {
                    collectFromExpression(stmt.expression);
                    return null;
                }
                
                @Override
                public Void visitVarStmt(Stmt.Var stmt) {
                    if (stmt.initializer != null) {
                        collectFromExpression(stmt.initializer);
                    }
                    return null;
                }
                
                @Override
                public Void visitReturnStmt(Stmt.Return stmt) {
                    if (stmt.value != null) {
                        collectFromExpression(stmt.value);
                    }
                    return null;
                }
                
                @Override
                public Void visitIfStmt(Stmt.If stmt) {
                    collectFromExpression(stmt.condition);
                    collectExpressions(stmt.thenBranch);
                    if (stmt.elseBranch != null) {
                        collectExpressions(stmt.elseBranch);
                    }
                    return null;
                }
                
                @Override
                public Void visitWhileStmt(Stmt.While stmt) {
                    collectFromExpression(stmt.condition);
                    collectExpressions(stmt.body);
                    return null;
                }
                
                @Override
                public Void visitForStmt(Stmt.For stmt) {
                    collectFromExpression(stmt.iterable);
                    collectExpressions(stmt.body);
                    return null;
                }
                
                @Override
                public Void visitBlockStmt(Stmt.Block stmt) {
                    for (Stmt s : stmt.statements) {
                        collectExpressions(s);
                    }
                    return null;
                }
                
                @Override
                public Void visitFunctionStmt(Stmt.Function stmt) {
                    // Don't collect expressions from inside function bodies
                    // Functions should be processed independently
                    return null;
                }
                
                @Override
                public Void visitClassStmt(Stmt.Class stmt) {
                    for (Stmt.Function method : stmt.methods) {
                        collectExpressions(method);
                    }
                    return null;
                }
                
                @Override
                public Void visitImportStmt(Stmt.Import stmt) {
                    return null;
                }
                
                @Override
                public Void visitExportStmt(Stmt.Export stmt) {
                    collectExpressions(stmt.declaration);
                    return null;
                }
            });
        }
        
        /**
         * Collect expressions from an expression tree.
         */
        private void collectFromExpression(Expr expr) {
            if (isOptimizableExpression(expr)) {
                String exprStr = expressionToString(expr);
                expressions.put(exprStr, expr);
                expressionCounts.put(exprStr, expressionCounts.getOrDefault(exprStr, 0) + 1);
            }
            
            // Recursively collect from subexpressions
            expr.accept(new Expr.Visitor<Void>() {
                @Override
                public Void visitBinaryExpr(Expr.Binary expr) {
                    collectFromExpression(expr.left);
                    collectFromExpression(expr.right);
                    return null;
                }
                
                @Override
                public Void visitUnaryExpr(Expr.Unary expr) {
                    collectFromExpression(expr.right);
                    return null;
                }
                
                @Override
                public Void visitCallExpr(Expr.Call expr) {
                    collectFromExpression(expr.callee);
                    for (Expr arg : expr.arguments) {
                        collectFromExpression(arg);
                    }
                    return null;
                }
                
                @Override
                public Void visitGroupingExpr(Expr.Grouping expr) {
                    collectFromExpression(expr.expression);
                    return null;
                }
                
                @Override
                public Void visitLogicalExpr(Expr.Logical expr) {
                    collectFromExpression(expr.left);
                    collectFromExpression(expr.right);
                    return null;
                }
                
                @Override
                public Void visitListExpr(Expr.ListExpr expr) {
                    for (Expr element : expr.elements) {
                        collectFromExpression(element);
                    }
                    return null;
                }
                
                @Override
                public Void visitDictExpr(Expr.Dict expr) {
                    for (Expr key : expr.keys) {
                        collectFromExpression(key);
                    }
                    for (Expr value : expr.values) {
                        collectFromExpression(value);
                    }
                    return null;
                }
                
                @Override
                public Void visitIndexExpr(Expr.Index expr) {
                    collectFromExpression(expr.object);
                    collectFromExpression(expr.index);
                    return null;
                }
                
                @Override
                public Void visitGetExpr(Expr.Get expr) {
                    collectFromExpression(expr.object);
                    return null;
                }
                
                @Override
                public Void visitSetExpr(Expr.Set expr) {
                    collectFromExpression(expr.object);
                    collectFromExpression(expr.value);
                    return null;
                }
                
                @Override
                public Void visitAssignExpr(Expr.Assign expr) {
                    collectFromExpression(expr.value);
                    return null;
                }
                
                // Simple expressions - no subexpressions
                @Override
                public Void visitLiteralExpr(Expr.Literal expr) {
                    return null;
                }
                
                @Override
                public Void visitVariableExpr(Expr.Variable expr) {
                    return null;
                }
                
                @Override
                public Void visitThisExpr(Expr.This expr) {
                    return null;
                }
                
                @Override
                public Void visitLambdaExpr(Expr.Lambda expr) {
                    // Don't optimize across lambda boundaries
                    return null;
                }
                
                @Override
                public Void visitMatchExpr(Expr.Match expr) {
                    // Complex expression - skip for now
                    return null;
                }
                
                @Override
                public Void visitIndexSetExpr(Expr.IndexSet expr) {
                    collectFromExpression(expr.object);
                    collectFromExpression(expr.index);
                    collectFromExpression(expr.value);
                    return null;
                }
                
                // Type expressions
                @Override
                public Void visitTypeExpr(Expr.Type expr) {
                    return null;
                }
                
                @Override
                public Void visitGenericTypeExpr(Expr.GenericType expr) {
                    return null;
                }
                
                @Override
                public Void visitFunctionTypeExpr(Expr.FunctionType expr) {
                    return null;
                }
                
                @Override
                public Void visitArrayTypeExpr(Expr.ArrayType expr) {
                    return null;
                }
            });
        }
        
        /**
         * Check if an expression is worth optimizing.
         */
        private boolean isOptimizableExpression(Expr expr) {
            // Only optimize relatively expensive expressions
            return expr instanceof Expr.Binary ||
                   expr instanceof Expr.Call ||
                   expr instanceof Expr.Index ||
                   expr instanceof Expr.Get;
        }
        
        /**
         * Convert expression to string for comparison.
         */
        private String expressionToString(Expr expr) {
            return expr.accept(new Expr.Visitor<String>() {
                @Override
                public String visitBinaryExpr(Expr.Binary expr) {
                    return "(" + expressionToString(expr.left) + " " + 
                           expr.operator.lexeme + " " + expressionToString(expr.right) + ")";
                }
                
                @Override
                public String visitUnaryExpr(Expr.Unary expr) {
                    return "(" + expr.operator.lexeme + expressionToString(expr.right) + ")";
                }
                
                @Override
                public String visitCallExpr(Expr.Call expr) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(expressionToString(expr.callee)).append("(");
                    for (int i = 0; i < expr.arguments.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(expressionToString(expr.arguments.get(i)));
                    }
                    sb.append(")");
                    return sb.toString();
                }
                
                @Override
                public String visitLiteralExpr(Expr.Literal expr) {
                    return expr.value.toString();
                }
                
                @Override
                public String visitVariableExpr(Expr.Variable expr) {
                    return expr.name.lexeme;
                }
                
                @Override
                public String visitGroupingExpr(Expr.Grouping expr) {
                    return "(" + expressionToString(expr.expression) + ")";
                }
                
                @Override
                public String visitIndexExpr(Expr.Index expr) {
                    return expressionToString(expr.object) + "[" + expressionToString(expr.index) + "]";
                }
                
                @Override
                public String visitGetExpr(Expr.Get expr) {
                    return expressionToString(expr.object) + "." + expr.name.lexeme;
                }
                
                // Default for other expressions
                @Override
                public String visitAssignExpr(Expr.Assign expr) {
                    return "assign";
                }
                
                @Override
                public String visitLogicalExpr(Expr.Logical expr) {
                    return "logical";
                }
                
                @Override
                public String visitListExpr(Expr.ListExpr expr) {
                    return "list";
                }
                
                @Override
                public String visitDictExpr(Expr.Dict expr) {
                    return "dict";
                }
                
                @Override
                public String visitIndexSetExpr(Expr.IndexSet expr) {
                    return "indexset";
                }
                
                @Override
                public String visitMatchExpr(Expr.Match expr) {
                    return "match";
                }
                
                @Override
                public String visitSetExpr(Expr.Set expr) {
                    return "set";
                }
                
                @Override
                public String visitThisExpr(Expr.This expr) {
                    return "this";
                }
                
                @Override
                public String visitLambdaExpr(Expr.Lambda expr) {
                    return "lambda";
                }
                
                @Override
                public String visitTypeExpr(Expr.Type expr) {
                    return "type";
                }
                
                @Override
                public String visitGenericTypeExpr(Expr.GenericType expr) {
                    return "generictype";
                }
                
                @Override
                public String visitFunctionTypeExpr(Expr.FunctionType expr) {
                    return "functiontype";
                }
                
                @Override
                public String visitArrayTypeExpr(Expr.ArrayType expr) {
                    return "arraytype";
                }
            });
        }
        
        /**
         * Transform a statement by replacing common subexpressions.
         */
        private Stmt transformStatement(Stmt stmt) {
            return stmt.accept(new Stmt.Visitor<Stmt>() {
                @Override
                public Stmt visitExpressionStmt(Stmt.Expression stmt) {
                    Expr optimizedExpr = transformExpression(stmt.expression);
                    return new Stmt.Expression(optimizedExpr);
                }
                
                @Override
                public Stmt visitVarStmt(Stmt.Var stmt) {
                    Expr optimizedInitializer = stmt.initializer != null ? 
                        transformExpression(stmt.initializer) : null;
                    return new Stmt.Var(stmt.name, stmt.type, optimizedInitializer, stmt.isImmutable);
                }
                
                @Override
                public Stmt visitReturnStmt(Stmt.Return stmt) {
                    Expr optimizedValue = stmt.value != null ? 
                        transformExpression(stmt.value) : null;
                    return new Stmt.Return(stmt.keyword, optimizedValue);
                }
                
                @Override
                public Stmt visitIfStmt(Stmt.If stmt) {
                    Expr optimizedCondition = transformExpression(stmt.condition);
                    Stmt optimizedThen = transformStatement(stmt.thenBranch);
                    Stmt optimizedElse = stmt.elseBranch != null ? 
                        transformStatement(stmt.elseBranch) : null;
                    return new Stmt.If(optimizedCondition, optimizedThen, optimizedElse);
                }
                
                @Override
                public Stmt visitWhileStmt(Stmt.While stmt) {
                    Expr optimizedCondition = transformExpression(stmt.condition);
                    Stmt optimizedBody = transformStatement(stmt.body);
                    return new Stmt.While(optimizedCondition, optimizedBody);
                }
                
                @Override
                public Stmt visitForStmt(Stmt.For stmt) {
                    Expr optimizedIterable = transformExpression(stmt.iterable);
                    Stmt optimizedBody = transformStatement(stmt.body);
                    return new Stmt.For(stmt.variable, optimizedIterable, optimizedBody);
                }
                
                @Override
                public Stmt visitBlockStmt(Stmt.Block stmt) {
                    List<Stmt> optimizedStatements = new ArrayList<>();
                    
                    // Transform all statements in the block
                    for (Stmt s : stmt.statements) {
                        optimizedStatements.add(transformStatement(s));
                    }
                    
                    return new Stmt.Block(optimizedStatements);
                }
                
                @Override
                public Stmt visitFunctionStmt(Stmt.Function stmt) {
                    // Don't transform function bodies in the outer scope
                    // Each function should be processed independently
                    return stmt;
                }
                
                @Override
                public Stmt visitClassStmt(Stmt.Class stmt) {
                    // Don't transform class methods in the outer scope
                    // Each method should be processed independently
                    return stmt;
                }
                
                @Override
                public Stmt visitImportStmt(Stmt.Import stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitExportStmt(Stmt.Export stmt) {
                    Stmt optimizedDeclaration = transformStatement(stmt.declaration);
                    return new Stmt.Export(optimizedDeclaration);
                }
            });
        }
        
        /**
         * Transform an expression by replacing common subexpressions.
         */
        private Expr transformExpression(Expr expr) {
            String exprStr = expressionToString(expr);
            
            // If we have a temp variable for this expression, use it
            if (isOptimizableExpression(expr) && tempVariables.containsKey(exprStr)) {
                Token tempName = new Token(TokenType.IDENTIFIER, tempVariables.get(exprStr), null, 0);
                return new Expr.Variable(tempName);
            }
            
            // Recursively transform subexpressions
            return expr.accept(new Expr.Visitor<Expr>() {
                @Override
                public Expr visitBinaryExpr(Expr.Binary expr) {
                    Expr left = transformExpression(expr.left);
                    Expr right = transformExpression(expr.right);
                    return new Expr.Binary(left, expr.operator, right);
                }
                
                @Override
                public Expr visitUnaryExpr(Expr.Unary expr) {
                    Expr right = transformExpression(expr.right);
                    return new Expr.Unary(expr.operator, right);
                }
                
                @Override
                public Expr visitCallExpr(Expr.Call expr) {
                    Expr callee = transformExpression(expr.callee);
                    List<Expr> args = new ArrayList<>();
                    for (Expr arg : expr.arguments) {
                        args.add(transformExpression(arg));
                    }
                    return new Expr.Call(callee, expr.paren, args);
                }
                
                @Override
                public Expr visitGroupingExpr(Expr.Grouping expr) {
                    Expr expression = transformExpression(expr.expression);
                    return new Expr.Grouping(expression);
                }
                
                @Override
                public Expr visitLogicalExpr(Expr.Logical expr) {
                    Expr left = transformExpression(expr.left);
                    Expr right = transformExpression(expr.right);
                    return new Expr.Logical(left, expr.operator, right);
                }
                
                @Override
                public Expr visitListExpr(Expr.ListExpr expr) {
                    List<Expr> elements = new ArrayList<>();
                    for (Expr element : expr.elements) {
                        elements.add(transformExpression(element));
                    }
                    return new Expr.ListExpr(elements);
                }
                
                @Override
                public Expr visitDictExpr(Expr.Dict expr) {
                    List<Expr> keys = new ArrayList<>();
                    List<Expr> values = new ArrayList<>();
                    for (Expr key : expr.keys) {
                        keys.add(transformExpression(key));
                    }
                    for (Expr value : expr.values) {
                        values.add(transformExpression(value));
                    }
                    return new Expr.Dict(keys, values);
                }
                
                @Override
                public Expr visitIndexExpr(Expr.Index expr) {
                    Expr object = transformExpression(expr.object);
                    Expr index = transformExpression(expr.index);
                    return new Expr.Index(object, expr.bracket, index);
                }
                
                @Override
                public Expr visitGetExpr(Expr.Get expr) {
                    Expr object = transformExpression(expr.object);
                    return new Expr.Get(object, expr.name);
                }
                
                @Override
                public Expr visitSetExpr(Expr.Set expr) {
                    Expr object = transformExpression(expr.object);
                    Expr value = transformExpression(expr.value);
                    return new Expr.Set(object, expr.name, value);
                }
                
                @Override
                public Expr visitAssignExpr(Expr.Assign expr) {
                    Expr value = transformExpression(expr.value);
                    return new Expr.Assign(expr.name, value);
                }
                
                @Override
                public Expr visitIndexSetExpr(Expr.IndexSet expr) {
                    Expr object = transformExpression(expr.object);
                    Expr index = transformExpression(expr.index);
                    Expr value = transformExpression(expr.value);
                    return new Expr.IndexSet(object, expr.bracket, index, value);
                }
                
                // Simple expressions - return as-is
                @Override
                public Expr visitLiteralExpr(Expr.Literal expr) {
                    return expr;
                }
                
                @Override
                public Expr visitVariableExpr(Expr.Variable expr) {
                    return expr;
                }
                
                @Override
                public Expr visitThisExpr(Expr.This expr) {
                    return expr;
                }
                
                @Override
                public Expr visitLambdaExpr(Expr.Lambda expr) {
                    // Don't optimize across lambda boundaries
                    return expr;
                }
                
                @Override
                public Expr visitMatchExpr(Expr.Match expr) {
                    // Complex expression - skip for now
                    return expr;
                }
                
                // Type expressions - return as-is
                @Override
                public Expr visitTypeExpr(Expr.Type expr) {
                    return expr;
                }
                
                @Override
                public Expr visitGenericTypeExpr(Expr.GenericType expr) {
                    return expr;
                }
                
                @Override
                public Expr visitFunctionTypeExpr(Expr.FunctionType expr) {
                    return expr;
                }
                
                @Override
                public Expr visitArrayTypeExpr(Expr.ArrayType expr) {
                    return expr;
                }
            });
        }
    }
}