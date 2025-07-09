package com.thorn;

import java.util.*;
import static com.thorn.TokenType.*;

/**
 * Optimization pass that inlines small functions to eliminate call overhead.
 * This pass analyzes function definitions and replaces function calls with
 * the function body when beneficial for performance.
 */
public class FunctionInliningPass extends OptimizationPass {
    
    private static final int DEFAULT_INLINE_THRESHOLD = 5; // AST node count
    private static final int MAX_INLINE_DEPTH = 3; // Prevent excessive inlining
    private static final int MAX_CALL_SITES = 5; // Don't inline heavily used functions
    
    @Override
    public String getName() {
        return "function-inlining";
    }
    
    @Override
    public PassType getType() {
        return PassType.TRANSFORMATION;
    }
    
    @Override
    public List<String> getDependencies() {
        return Arrays.asList("dead-code-elimination", "constant-folding");
    }
    
    @Override
    public OptimizationLevel getMinimumLevel() {
        return OptimizationLevel.O2;
    }
    
    @Override
    public List<Stmt> optimize(List<Stmt> statements, OptimizationContext context) {
        if (context.isDebugMode()) {
            System.out.println("=== Function Inlining Pass ===");
            System.out.println("  Stub implementation - no transformations applied");
        }
        return statements; // TODO: Implement function inlining optimization
    }
    
    /**
     * Transformer that performs function inlining analysis and transformations.
     */
    private static class InliningTransformer {
        private final OptimizationContext context;
        private final Map<String, Stmt.Function> functionDefinitions;
        private final Map<String, Integer> functionSizes;
        private final Map<String, Integer> callCounts;
        private final Set<String> recursiveFunctions;
        private final Set<String> inlineableFunctions;
        private final Set<String> successfullyInlinedFunctions;
        private int inlineThreshold;
        private int inlineDepth;
        
        public InliningTransformer(OptimizationContext context) {
            this.context = context;
            this.functionDefinitions = new HashMap<>();
            this.functionSizes = new HashMap<>();
            this.callCounts = new HashMap<>();
            this.recursiveFunctions = new HashSet<>();
            this.inlineableFunctions = new HashSet<>();
            this.successfullyInlinedFunctions = new HashSet<>();
            this.inlineThreshold = context.getPassConfigurationInt(
                "function-inlining", "threshold", DEFAULT_INLINE_THRESHOLD
            );
            this.inlineDepth = 0;
        }
        
        public List<Stmt> transform(List<Stmt> statements) {
            // First pass: collect function definitions and analyze
            analyzeFunctions(statements);
            
            // Second pass: count function calls
            countFunctionCalls(statements);
            
            // Third pass: determine which functions to inline
            determineInlineableFunctions();
            
            // Fourth pass: perform inlining transformations
            List<Stmt> result = performInlining(statements);
            
            if (context.isDebugMode()) {
                System.out.println("=== Function Inlining ===");
                System.out.println("Functions analyzed: " + functionDefinitions.size());
                System.out.println("Inlineable functions: " + inlineableFunctions.size());
                System.out.println("Recursive functions: " + recursiveFunctions.size());
                for (String funcName : inlineableFunctions) {
                    System.out.println("  Will inline: " + funcName + 
                        " (size: " + functionSizes.get(funcName) + 
                        ", calls: " + callCounts.get(funcName) + ")");
                }
            }
            
            return result;
        }
        
        /**
         * Analyze function definitions to calculate size and detect recursion.
         */
        private void analyzeFunctions(List<Stmt> statements) {
            for (Stmt stmt : statements) {
                analyzeFunctionDefinitions(stmt);
            }
        }
        
        private void analyzeFunctionDefinitions(Stmt stmt) {
            stmt.accept(new Stmt.Visitor<Void>() {
                @Override
                public Void visitFunctionStmt(Stmt.Function stmt) {
                    String funcName = stmt.name.lexeme;
                    functionDefinitions.put(funcName, stmt);
                    
                    // Calculate function size (AST node count)
                    int size = calculateFunctionSize(stmt);
                    functionSizes.put(funcName, size);
                    
                    // Check for recursion
                    if (isRecursive(stmt)) {
                        recursiveFunctions.add(funcName);
                    }
                    
                    return null;
                }
                
                @Override
                public Void visitBlockStmt(Stmt.Block stmt) {
                    for (Stmt s : stmt.statements) {
                        analyzeFunctionDefinitions(s);
                    }
                    return null;
                }
                
                @Override
                public Void visitClassStmt(Stmt.Class stmt) {
                    for (Stmt.Function method : stmt.methods) {
                        analyzeFunctionDefinitions(method);
                    }
                    return null;
                }
                
                @Override
                public Void visitIfStmt(Stmt.If stmt) {
                    analyzeFunctionDefinitions(stmt.thenBranch);
                    if (stmt.elseBranch != null) {
                        analyzeFunctionDefinitions(stmt.elseBranch);
                    }
                    return null;
                }
                
                @Override
                public Void visitWhileStmt(Stmt.While stmt) {
                    analyzeFunctionDefinitions(stmt.body);
                    return null;
                }
                
                @Override
                public Void visitForStmt(Stmt.For stmt) {
                    analyzeFunctionDefinitions(stmt.body);
                    return null;
                }
                
                // Other statement types don't contain function definitions
                @Override public Void visitExpressionStmt(Stmt.Expression stmt) { return null; }
                @Override public Void visitVarStmt(Stmt.Var stmt) { return null; }
                @Override public Void visitReturnStmt(Stmt.Return stmt) { return null; }
                @Override public Void visitImportStmt(Stmt.Import stmt) { return null; }
                @Override public Void visitExportStmt(Stmt.Export stmt) { 
                    analyzeFunctionDefinitions(stmt.declaration);
                    return null; 
                }
                
                @Override public Void visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                    return null;
                }
            });
        }
        
        /**
         * Calculate the size of a function in AST nodes.
         */
        private int calculateFunctionSize(Stmt.Function function) {
            FunctionSizeCalculator calculator = new FunctionSizeCalculator();
            for (Stmt stmt : function.body) {
                calculator.calculateStatementSize(stmt);
            }
            return calculator.getSize();
        }
        
        /**
         * Check if a function is recursive by looking for calls to itself.
         */
        private boolean isRecursive(Stmt.Function function) {
            String funcName = function.name.lexeme;
            RecursionDetector detector = new RecursionDetector(funcName);
            for (Stmt stmt : function.body) {
                if (detector.checkStatement(stmt)) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Count function calls throughout the program.
         */
        private void countFunctionCalls(List<Stmt> statements) {
            CallCounter counter = new CallCounter();
            for (Stmt stmt : statements) {
                counter.countCalls(stmt);
            }
        }
        
        /**
         * Determine which functions should be inlined based on heuristics.
         */
        private void determineInlineableFunctions() {
            for (Map.Entry<String, Stmt.Function> entry : functionDefinitions.entrySet()) {
                String funcName = entry.getKey();
                int size = functionSizes.get(funcName);
                int calls = callCounts.getOrDefault(funcName, 0);
                
                // Inlining heuristics
                boolean shouldInline = true;
                
                // Don't inline if function is too large
                if (size > inlineThreshold) {
                    shouldInline = false;
                }
                
                // Don't inline recursive functions
                if (recursiveFunctions.contains(funcName)) {
                    shouldInline = false;
                }
                
                // Don't inline functions with too many call sites
                if (calls > MAX_CALL_SITES) {
                    shouldInline = false;
                }
                
                // Don't inline functions that are too complex for simple expression inlining
                // Use configured threshold, but be conservative for multi-statement functions
                if (size > inlineThreshold) {
                    shouldInline = false;
                }
                
                // Don't inline functions with no calls
                if (calls == 0) {
                    shouldInline = false;
                }
                
                if (shouldInline) {
                    inlineableFunctions.add(funcName);
                }
            }
        }
        
        /**
         * Perform the actual inlining transformations.
         */
        private List<Stmt> performInlining(List<Stmt> statements) {
            List<Stmt> result = new ArrayList<>();
            
            for (Stmt stmt : statements) {
                Stmt transformed = transformStatement(stmt);
                
                // Only keep function definitions that were not successfully inlined
                if (stmt instanceof Stmt.Function) {
                    Stmt.Function func = (Stmt.Function) stmt;
                    if (!successfullyInlinedFunctions.contains(func.name.lexeme)) {
                        result.add(transformed);
                    }
                    // Skip successfully inlined functions (they've been replaced at call sites)
                } else {
                    result.add(transformed);
                }
            }
            
            return result;
        }
        
        /**
         * Transform a statement, inlining function calls where appropriate.
         */
        private Stmt transformStatement(Stmt stmt) {
            return stmt.accept(new Stmt.Visitor<Stmt>() {
                @Override
                public Stmt visitExpressionStmt(Stmt.Expression stmt) {
                    Expr transformedExpr = transformExpression(stmt.expression);
                    return new Stmt.Expression(transformedExpr);
                }
                
                @Override
                public Stmt visitVarStmt(Stmt.Var stmt) {
                    Expr transformedInitializer = stmt.initializer != null ? 
                        transformExpression(stmt.initializer) : null;
                    return new Stmt.Var(stmt.name, stmt.type, transformedInitializer, stmt.isImmutable);
                }
                
                @Override
                public Stmt visitReturnStmt(Stmt.Return stmt) {
                    Expr transformedValue = stmt.value != null ? 
                        transformExpression(stmt.value) : null;
                    return new Stmt.Return(stmt.keyword, transformedValue);
                }
                
                @Override
                public Stmt visitIfStmt(Stmt.If stmt) {
                    Expr transformedCondition = transformExpression(stmt.condition);
                    Stmt transformedThen = transformStatement(stmt.thenBranch);
                    Stmt transformedElse = stmt.elseBranch != null ? 
                        transformStatement(stmt.elseBranch) : null;
                    return new Stmt.If(transformedCondition, transformedThen, transformedElse);
                }
                
                @Override
                public Stmt visitWhileStmt(Stmt.While stmt) {
                    Expr transformedCondition = transformExpression(stmt.condition);
                    Stmt transformedBody = transformStatement(stmt.body);
                    return new Stmt.While(transformedCondition, transformedBody);
                }
                
                @Override
                public Stmt visitForStmt(Stmt.For stmt) {
                    Expr transformedIterable = transformExpression(stmt.iterable);
                    Stmt transformedBody = transformStatement(stmt.body);
                    return new Stmt.For(stmt.variable, transformedIterable, transformedBody);
                }
                
                @Override
                public Stmt visitBlockStmt(Stmt.Block stmt) {
                    List<Stmt> transformedStatements = new ArrayList<>();
                    for (Stmt s : stmt.statements) {
                        Stmt transformed = transformStatement(s);
                        if (transformed instanceof Stmt.Block) {
                            // Flatten nested blocks from inlining
                            transformedStatements.addAll(((Stmt.Block) transformed).statements);
                        } else {
                            transformedStatements.add(transformed);
                        }
                    }
                    return new Stmt.Block(transformedStatements);
                }
                
                @Override
                public Stmt visitFunctionStmt(Stmt.Function stmt) {
                    // Transform function body
                    List<Stmt> transformedBody = new ArrayList<>();
                    for (Stmt s : stmt.body) {
                        transformedBody.add(transformStatement(s));
                    }
                    return new Stmt.Function(stmt.name, stmt.params, stmt.returnType, transformedBody);
                }
                
                @Override
                public Stmt visitClassStmt(Stmt.Class stmt) {
                    List<Stmt.Function> transformedMethods = new ArrayList<>();
                    for (Stmt.Function method : stmt.methods) {
                        transformedMethods.add((Stmt.Function) transformStatement(method));
                    }
                    return new Stmt.Class(stmt.name, transformedMethods);
                }
                
                @Override
                public Stmt visitImportStmt(Stmt.Import stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitExportStmt(Stmt.Export stmt) {
                    Stmt transformedDeclaration = transformStatement(stmt.declaration);
                    return new Stmt.Export(transformedDeclaration);
                }
                
                @Override
                public Stmt visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                    return stmt;
                }
            });
        }
        
        /**
         * Transform expressions, inlining function calls where appropriate.
         */
        private Expr transformExpression(Expr expr) {
            if (inlineDepth >= MAX_INLINE_DEPTH) {
                // Prevent excessive inlining depth
                return expr;
            }
            
            return expr.accept(new Expr.Visitor<Expr>() {
                @Override
                public Expr visitCallExpr(Expr.Call expr) {
                    // Check if this is a function call that can be inlined
                    if (expr.callee instanceof Expr.Variable) {
                        Expr.Variable var = (Expr.Variable) expr.callee;
                        String funcName = var.name.lexeme;
                        
                        if (inlineableFunctions.contains(funcName)) {
                            Expr inlined = inlineFunction(funcName, expr.arguments);
                            if (inlined != null) {
                                successfullyInlinedFunctions.add(funcName);
                                return inlined;
                            }
                            // Fall through to transform arguments if inlining failed
                        }
                    }
                    
                    // Transform arguments
                    List<Expr> transformedArgs = new ArrayList<>();
                    for (Expr arg : expr.arguments) {
                        transformedArgs.add(transformExpression(arg));
                    }
                    
                    Expr transformedCallee = transformExpression(expr.callee);
                    return new Expr.Call(transformedCallee, expr.paren, transformedArgs);
                }
                
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
                public Expr visitAssignExpr(Expr.Assign expr) {
                    Expr value = transformExpression(expr.value);
                    return new Expr.Assign(expr.name, value);
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
                    // Don't inline across lambda boundaries
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
        
        /**
         * Inline a function call by substituting the function body.
         */
        private Expr inlineFunction(String funcName, List<Expr> arguments) {
            Stmt.Function function = functionDefinitions.get(funcName);
            if (function == null) {
                return null; // Should not happen if analysis is correct
            }
            
            inlineDepth++;
            try {
                // Create parameter substitution map
                Map<String, Expr> parameterMap = new HashMap<>();
                for (int i = 0; i < function.params.size() && i < arguments.size(); i++) {
                    String paramName = function.params.get(i).name.lexeme;
                    Expr argValue = transformExpression(arguments.get(i));
                    parameterMap.put(paramName, argValue);
                }
                
                // Create inlined function body with parameter substitution
                FunctionInliner inliner = new FunctionInliner(parameterMap);
                List<Stmt> inlinedBody = new ArrayList<>();
                
                for (Stmt stmt : function.body) {
                    Stmt inlinedStmt = inliner.inlineStatement(stmt);
                    if (inlinedStmt != null) {
                        inlinedBody.add(inlinedStmt);
                    }
                }
                
                // For simple functions that just return an expression, return that expression
                if (inlinedBody.size() == 1 && inlinedBody.get(0) instanceof Stmt.Return) {
                    Stmt.Return returnStmt = (Stmt.Return) inlinedBody.get(0);
                    if (returnStmt.value != null) {
                        // Successfully inlined! Return the inlined expression
                        return returnStmt.value;
                    }
                }
                
                // For more complex functions, we can't inline them yet
                // Return null to indicate inlining failed
                return null;
                
            } finally {
                inlineDepth--;
            }
        }
        
        /**
         * Helper class to calculate function size in AST nodes.
         */
        private static class FunctionSizeCalculator {
            private int size = 0;
            
            public int getSize() {
                return size;
            }
            
            public void calculateStatementSize(Stmt stmt) {
                size++;
                stmt.accept(new Stmt.Visitor<Void>() {
                    @Override
                    public Void visitExpressionStmt(Stmt.Expression stmt) {
                        calculateExpressionSize(stmt.expression);
                        return null;
                    }
                    
                    @Override
                    public Void visitVarStmt(Stmt.Var stmt) {
                        if (stmt.initializer != null) {
                            calculateExpressionSize(stmt.initializer);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitReturnStmt(Stmt.Return stmt) {
                        if (stmt.value != null) {
                            calculateExpressionSize(stmt.value);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitIfStmt(Stmt.If stmt) {
                        calculateExpressionSize(stmt.condition);
                        calculateStatementSize(stmt.thenBranch);
                        if (stmt.elseBranch != null) {
                            calculateStatementSize(stmt.elseBranch);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitWhileStmt(Stmt.While stmt) {
                        calculateExpressionSize(stmt.condition);
                        calculateStatementSize(stmt.body);
                        return null;
                    }
                    
                    @Override
                    public Void visitForStmt(Stmt.For stmt) {
                        calculateExpressionSize(stmt.iterable);
                        calculateStatementSize(stmt.body);
                        return null;
                    }
                    
                    @Override
                    public Void visitBlockStmt(Stmt.Block stmt) {
                        for (Stmt s : stmt.statements) {
                            calculateStatementSize(s);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitFunctionStmt(Stmt.Function stmt) {
                        for (Stmt s : stmt.body) {
                            calculateStatementSize(s);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitClassStmt(Stmt.Class stmt) {
                        for (Stmt.Function method : stmt.methods) {
                            calculateStatementSize(method);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitImportStmt(Stmt.Import stmt) {
                        return null;
                    }
                    
                    @Override
                    public Void visitExportStmt(Stmt.Export stmt) {
                        calculateStatementSize(stmt.declaration);
                        return null;
                    }
                    
                    @Override
                    public Void visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                        return null;
                    }
                });
            }
            
            public void calculateExpressionSize(Expr expr) {
                size++;
                // Simplified - just count each expression as 1 node
                // A more sophisticated version would traverse the expression tree
            }
        }
        
        /**
         * Helper class to detect recursive function calls.
         */
        private static class RecursionDetector {
            private final String targetFunction;
            
            public RecursionDetector(String targetFunction) {
                this.targetFunction = targetFunction;
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
                        for (Stmt s : stmt.body) {
                            if (checkStatement(s)) return true;
                        }
                        return false;
                    }
                    
                    @Override
                    public Boolean visitClassStmt(Stmt.Class stmt) {
                        for (Stmt.Function method : stmt.methods) {
                            if (checkStatement(method)) return true;
                        }
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
                    
                    @Override
                    public Boolean visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                        return false;
                    }
                });
            }
            
            public boolean checkExpression(Expr expr) {
                return expr.accept(new Expr.Visitor<Boolean>() {
                    @Override
                    public Boolean visitCallExpr(Expr.Call expr) {
                        if (expr.callee instanceof Expr.Variable) {
                            Expr.Variable var = (Expr.Variable) expr.callee;
                            if (var.name.lexeme.equals(targetFunction)) {
                                return true;
                            }
                        }
                        
                        // Check arguments for recursive calls
                        for (Expr arg : expr.arguments) {
                            if (checkExpression(arg)) return true;
                        }
                        
                        return checkExpression(expr.callee);
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
                    public Boolean visitGroupingExpr(Expr.Grouping expr) {
                        return checkExpression(expr.expression);
                    }
                    
                    @Override
                    public Boolean visitLogicalExpr(Expr.Logical expr) {
                        return checkExpression(expr.left) || checkExpression(expr.right);
                    }
                    
                    @Override
                    public Boolean visitAssignExpr(Expr.Assign expr) {
                        return checkExpression(expr.value);
                    }
                    
                    // Other expressions types return false by default
                    @Override public Boolean visitLiteralExpr(Expr.Literal expr) { return false; }
                    @Override public Boolean visitVariableExpr(Expr.Variable expr) { return false; }
                    @Override public Boolean visitThisExpr(Expr.This expr) { return false; }
                    @Override public Boolean visitLambdaExpr(Expr.Lambda expr) { return false; }
                    @Override public Boolean visitMatchExpr(Expr.Match expr) { return false; }
                    @Override public Boolean visitListExpr(Expr.ListExpr expr) { return false; }
                    @Override public Boolean visitDictExpr(Expr.Dict expr) { return false; }
                    @Override public Boolean visitIndexExpr(Expr.Index expr) { return false; }
                    @Override public Boolean visitGetExpr(Expr.Get expr) { return false; }
                    @Override public Boolean visitSetExpr(Expr.Set expr) { return false; }
                    @Override public Boolean visitIndexSetExpr(Expr.IndexSet expr) { return false; }
                    @Override public Boolean visitTypeExpr(Expr.Type expr) { return false; }
                    @Override public Boolean visitGenericTypeExpr(Expr.GenericType expr) { return false; }
                    @Override public Boolean visitFunctionTypeExpr(Expr.FunctionType expr) { return false; }
                    @Override public Boolean visitArrayTypeExpr(Expr.ArrayType expr) { return false; }
                });
            }
        }
        
        /**
         * Helper class to count function calls.
         */
        private class CallCounter {
            public void countCalls(Stmt stmt) {
                stmt.accept(new Stmt.Visitor<Void>() {
                    @Override
                    public Void visitExpressionStmt(Stmt.Expression stmt) {
                        countCallsInExpression(stmt.expression);
                        return null;
                    }
                    
                    @Override
                    public Void visitVarStmt(Stmt.Var stmt) {
                        if (stmt.initializer != null) {
                            countCallsInExpression(stmt.initializer);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitReturnStmt(Stmt.Return stmt) {
                        if (stmt.value != null) {
                            countCallsInExpression(stmt.value);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitIfStmt(Stmt.If stmt) {
                        countCallsInExpression(stmt.condition);
                        countCalls(stmt.thenBranch);
                        if (stmt.elseBranch != null) {
                            countCalls(stmt.elseBranch);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitWhileStmt(Stmt.While stmt) {
                        countCallsInExpression(stmt.condition);
                        countCalls(stmt.body);
                        return null;
                    }
                    
                    @Override
                    public Void visitForStmt(Stmt.For stmt) {
                        countCallsInExpression(stmt.iterable);
                        countCalls(stmt.body);
                        return null;
                    }
                    
                    @Override
                    public Void visitBlockStmt(Stmt.Block stmt) {
                        for (Stmt s : stmt.statements) {
                            countCalls(s);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitFunctionStmt(Stmt.Function stmt) {
                        for (Stmt s : stmt.body) {
                            countCalls(s);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitClassStmt(Stmt.Class stmt) {
                        for (Stmt.Function method : stmt.methods) {
                            countCalls(method);
                        }
                        return null;
                    }
                    
                    @Override
                    public Void visitImportStmt(Stmt.Import stmt) {
                        return null;
                    }
                    
                    @Override
                    public Void visitExportStmt(Stmt.Export stmt) {
                        countCalls(stmt.declaration);
                        return null;
                    }
                    
                    @Override
                    public Void visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                        return null;
                    }
                });
            }
            
            private void countCallsInExpression(Expr expr) {
                expr.accept(new Expr.Visitor<Void>() {
                    @Override
                    public Void visitCallExpr(Expr.Call expr) {
                        if (expr.callee instanceof Expr.Variable) {
                            Expr.Variable var = (Expr.Variable) expr.callee;
                            String funcName = var.name.lexeme;
                            callCounts.put(funcName, callCounts.getOrDefault(funcName, 0) + 1);
                        }
                        
                        // Count calls in arguments
                        for (Expr arg : expr.arguments) {
                            countCallsInExpression(arg);
                        }
                        
                        countCallsInExpression(expr.callee);
                        return null;
                    }
                    
                    @Override
                    public Void visitBinaryExpr(Expr.Binary expr) {
                        countCallsInExpression(expr.left);
                        countCallsInExpression(expr.right);
                        return null;
                    }
                    
                    @Override
                    public Void visitUnaryExpr(Expr.Unary expr) {
                        countCallsInExpression(expr.right);
                        return null;
                    }
                    
                    @Override
                    public Void visitGroupingExpr(Expr.Grouping expr) {
                        countCallsInExpression(expr.expression);
                        return null;
                    }
                    
                    @Override
                    public Void visitLogicalExpr(Expr.Logical expr) {
                        countCallsInExpression(expr.left);
                        countCallsInExpression(expr.right);
                        return null;
                    }
                    
                    @Override
                    public Void visitAssignExpr(Expr.Assign expr) {
                        countCallsInExpression(expr.value);
                        return null;
                    }
                    
                    // Other expression types don't contain calls
                    @Override public Void visitLiteralExpr(Expr.Literal expr) { return null; }
                    @Override public Void visitVariableExpr(Expr.Variable expr) { return null; }
                    @Override public Void visitThisExpr(Expr.This expr) { return null; }
                    @Override public Void visitLambdaExpr(Expr.Lambda expr) { return null; }
                    @Override public Void visitMatchExpr(Expr.Match expr) { return null; }
                    @Override public Void visitListExpr(Expr.ListExpr expr) { return null; }
                    @Override public Void visitDictExpr(Expr.Dict expr) { return null; }
                    @Override public Void visitIndexExpr(Expr.Index expr) { return null; }
                    @Override public Void visitGetExpr(Expr.Get expr) { return null; }
                    @Override public Void visitSetExpr(Expr.Set expr) { return null; }
                    @Override public Void visitIndexSetExpr(Expr.IndexSet expr) { return null; }
                    @Override public Void visitTypeExpr(Expr.Type expr) { return null; }
                    @Override public Void visitGenericTypeExpr(Expr.GenericType expr) { return null; }
                    @Override public Void visitFunctionTypeExpr(Expr.FunctionType expr) { return null; }
                    @Override public Void visitArrayTypeExpr(Expr.ArrayType expr) { return null; }
                });
            }
        }
        
        /**
         * Helper class to perform parameter substitution when inlining.
         */
        private static class FunctionInliner {
            private final Map<String, Expr> parameterMap;
            
            public FunctionInliner(Map<String, Expr> parameterMap) {
                this.parameterMap = parameterMap;
            }
            
            public Stmt inlineStatement(Stmt stmt) {
                return stmt.accept(new Stmt.Visitor<Stmt>() {
                    @Override
                    public Stmt visitExpressionStmt(Stmt.Expression stmt) {
                        Expr inlinedExpr = inlineExpression(stmt.expression);
                        return new Stmt.Expression(inlinedExpr);
                    }
                    
                    @Override
                    public Stmt visitVarStmt(Stmt.Var stmt) {
                        Expr inlinedInitializer = stmt.initializer != null ? 
                            inlineExpression(stmt.initializer) : null;
                        return new Stmt.Var(stmt.name, stmt.type, inlinedInitializer, stmt.isImmutable);
                    }
                    
                    @Override
                    public Stmt visitReturnStmt(Stmt.Return stmt) {
                        Expr inlinedValue = stmt.value != null ? 
                            inlineExpression(stmt.value) : null;
                        return new Stmt.Return(stmt.keyword, inlinedValue);
                    }
                    
                    @Override
                    public Stmt visitIfStmt(Stmt.If stmt) {
                        Expr inlinedCondition = inlineExpression(stmt.condition);
                        Stmt inlinedThen = inlineStatement(stmt.thenBranch);
                        Stmt inlinedElse = stmt.elseBranch != null ? 
                            inlineStatement(stmt.elseBranch) : null;
                        return new Stmt.If(inlinedCondition, inlinedThen, inlinedElse);
                    }
                    
                    @Override
                    public Stmt visitWhileStmt(Stmt.While stmt) {
                        Expr inlinedCondition = inlineExpression(stmt.condition);
                        Stmt inlinedBody = inlineStatement(stmt.body);
                        return new Stmt.While(inlinedCondition, inlinedBody);
                    }
                    
                    @Override
                    public Stmt visitForStmt(Stmt.For stmt) {
                        Expr inlinedIterable = inlineExpression(stmt.iterable);
                        Stmt inlinedBody = inlineStatement(stmt.body);
                        return new Stmt.For(stmt.variable, inlinedIterable, inlinedBody);
                    }
                    
                    @Override
                    public Stmt visitBlockStmt(Stmt.Block stmt) {
                        List<Stmt> inlinedStatements = new ArrayList<>();
                        for (Stmt s : stmt.statements) {
                            inlinedStatements.add(inlineStatement(s));
                        }
                        return new Stmt.Block(inlinedStatements);
                    }
                    
                    @Override
                    public Stmt visitFunctionStmt(Stmt.Function stmt) {
                        // Don't inline nested functions for simplicity
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
                        Stmt inlinedDeclaration = inlineStatement(stmt.declaration);
                        return new Stmt.Export(inlinedDeclaration);
                    }
                    
                    @Override
                    public Stmt visitExportIdentifierStmt(Stmt.ExportIdentifier stmt) {
                        return stmt;
                    }
                });
            }
            
            public Expr inlineExpression(Expr expr) {
                return expr.accept(new Expr.Visitor<Expr>() {
                    @Override
                    public Expr visitVariableExpr(Expr.Variable expr) {
                        String varName = expr.name.lexeme;
                        if (parameterMap.containsKey(varName)) {
                            return parameterMap.get(varName);
                        }
                        return expr;
                    }
                    
                    @Override
                    public Expr visitBinaryExpr(Expr.Binary expr) {
                        Expr left = inlineExpression(expr.left);
                        Expr right = inlineExpression(expr.right);
                        return new Expr.Binary(left, expr.operator, right);
                    }
                    
                    @Override
                    public Expr visitUnaryExpr(Expr.Unary expr) {
                        Expr right = inlineExpression(expr.right);
                        return new Expr.Unary(expr.operator, right);
                    }
                    
                    @Override
                    public Expr visitCallExpr(Expr.Call expr) {
                        Expr callee = inlineExpression(expr.callee);
                        List<Expr> args = new ArrayList<>();
                        for (Expr arg : expr.arguments) {
                            args.add(inlineExpression(arg));
                        }
                        return new Expr.Call(callee, expr.paren, args);
                    }
                    
                    @Override
                    public Expr visitGroupingExpr(Expr.Grouping expr) {
                        Expr expression = inlineExpression(expr.expression);
                        return new Expr.Grouping(expression);
                    }
                    
                    @Override
                    public Expr visitLogicalExpr(Expr.Logical expr) {
                        Expr left = inlineExpression(expr.left);
                        Expr right = inlineExpression(expr.right);
                        return new Expr.Logical(left, expr.operator, right);
                    }
                    
                    @Override
                    public Expr visitAssignExpr(Expr.Assign expr) {
                        Expr value = inlineExpression(expr.value);
                        return new Expr.Assign(expr.name, value);
                    }
                    
                    // Simple expressions - return as-is
                    @Override
                    public Expr visitLiteralExpr(Expr.Literal expr) {
                        return expr;
                    }
                    
                    @Override
                    public Expr visitThisExpr(Expr.This expr) {
                        return expr;
                    }
                    
                    @Override
                    public Expr visitLambdaExpr(Expr.Lambda expr) {
                        return expr;
                    }
                    
                    @Override
                    public Expr visitMatchExpr(Expr.Match expr) {
                        return expr;
                    }
                    
                    @Override
                    public Expr visitListExpr(Expr.ListExpr expr) {
                        List<Expr> elements = new ArrayList<>();
                        for (Expr element : expr.elements) {
                            elements.add(inlineExpression(element));
                        }
                        return new Expr.ListExpr(elements);
                    }
                    
                    @Override
                    public Expr visitDictExpr(Expr.Dict expr) {
                        List<Expr> keys = new ArrayList<>();
                        List<Expr> values = new ArrayList<>();
                        for (Expr key : expr.keys) {
                            keys.add(inlineExpression(key));
                        }
                        for (Expr value : expr.values) {
                            values.add(inlineExpression(value));
                        }
                        return new Expr.Dict(keys, values);
                    }
                    
                    @Override
                    public Expr visitIndexExpr(Expr.Index expr) {
                        Expr object = inlineExpression(expr.object);
                        Expr index = inlineExpression(expr.index);
                        return new Expr.Index(object, expr.bracket, index);
                    }
                    
                    @Override
                    public Expr visitGetExpr(Expr.Get expr) {
                        Expr object = inlineExpression(expr.object);
                        return new Expr.Get(object, expr.name);
                    }
                    
                    @Override
                    public Expr visitSetExpr(Expr.Set expr) {
                        Expr object = inlineExpression(expr.object);
                        Expr value = inlineExpression(expr.value);
                        return new Expr.Set(object, expr.name, value);
                    }
                    
                    @Override
                    public Expr visitIndexSetExpr(Expr.IndexSet expr) {
                        Expr object = inlineExpression(expr.object);
                        Expr index = inlineExpression(expr.index);
                        Expr value = inlineExpression(expr.value);
                        return new Expr.IndexSet(object, expr.bracket, index, value);
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
}