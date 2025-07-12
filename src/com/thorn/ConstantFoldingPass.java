package com.thorn;

import java.util.*;

/**
 * Evaluates constant expressions at compile time.
 * Simplifies arithmetic and boolean operations with literal operands.
 */
public class ConstantFoldingPass extends OptimizationPass {
    
    private int expressionsFolded = 0;
    
    @Override
    public String getName() {
        return "constant-folding";
    }
    
    @Override
    public PassType getType() {
        return PassType.TRANSFORMATION;
    }
    
    @Override
    public OptimizationLevel getMinimumLevel() {
        return OptimizationLevel.O1;
    }
    
    @Override
    public List<Stmt> optimize(List<Stmt> statements, OptimizationContext context) {
        if (context.isDebugMode()) {
            System.out.println("=== Constant Folding Pass ===");
        }
        
        expressionsFolded = 0;
        
        ConstantFolder folder = new ConstantFolder(context);
        List<Stmt> optimized = folder.foldConstants(statements);
        
        if (context.isDebugMode()) {
            System.out.println("  Expressions folded: " + expressionsFolded);
        }
        
        return optimized;
    }
    
    private class ConstantFolder {
        private final OptimizationContext context;
        
        public ConstantFolder(OptimizationContext context) {
            this.context = context;
        }
        
        public List<Stmt> foldConstants(List<Stmt> statements) {
            List<Stmt> result = new ArrayList<>();
            
            for (Stmt stmt : statements) {
                result.add(foldStatement(stmt));
            }
            
            return result;
        }
        
        private Stmt foldStatement(Stmt stmt) {
            return stmt.accept(new Stmt.Visitor<Stmt>() {
                @Override
                public Stmt visitExpressionStmt(Stmt.Expression stmt) {
                    return new Stmt.Expression(foldExpression(stmt.expression));
                }
                
                @Override
                public Stmt visitVarStmt(Stmt.Var stmt) {
                    Expr initializer = stmt.initializer != null ? 
                        foldExpression(stmt.initializer) : null;
                    return new Stmt.Var(stmt.name, stmt.type, initializer, stmt.isImmutable);
                }
                
                @Override
                public Stmt visitBlockStmt(Stmt.Block stmt) {
                    return new Stmt.Block(foldConstants(stmt.statements));
                }
                
                @Override
                public Stmt visitIfStmt(Stmt.If stmt) {
                    Expr condition = foldExpression(stmt.condition);
                    
                    // If condition is constant, optimize away the branch
                    if (condition instanceof Expr.Literal) {
                        Object value = ((Expr.Literal) condition).value;
                        if (isTruthy(value)) {
                            return foldStatement(stmt.thenBranch);
                        } else if (stmt.elseBranch != null) {
                            return foldStatement(stmt.elseBranch);
                        } else {
                            // No else branch and condition is false - remove entire if
                            return new Stmt.Block(new ArrayList<>());
                        }
                    }
                    
                    return new Stmt.If(
                        condition,
                        foldStatement(stmt.thenBranch),
                        stmt.elseBranch != null ? foldStatement(stmt.elseBranch) : null
                    );
                }
                
                @Override
                public Stmt visitWhileStmt(Stmt.While stmt) {
                    Expr condition = foldExpression(stmt.condition);
                    
                    // If condition is constant false, remove the loop
                    if (condition instanceof Expr.Literal && 
                        !isTruthy(((Expr.Literal) condition).value)) {
                        return new Stmt.Block(new ArrayList<>());
                    }
                    
                    return new Stmt.While(condition, foldStatement(stmt.body));
                }
                
                @Override
                public Stmt visitForStmt(Stmt.For stmt) {
                    return new Stmt.For(
                        stmt.variable,
                        foldExpression(stmt.iterable),
                        foldStatement(stmt.body)
                    );
                }
                
                @Override
                public Stmt visitReturnStmt(Stmt.Return stmt) {
                    return new Stmt.Return(
                        stmt.keyword,
                        stmt.value != null ? foldExpression(stmt.value) : null
                    );
                }
                
                @Override
                public Stmt visitThrowStmt(Stmt.Throw stmt) {
                    return new Stmt.Throw(
                        stmt.keyword,
                        stmt.value != null ? foldExpression(stmt.value) : null
                    );
                }
                
                @Override
                public Stmt visitFunctionStmt(Stmt.Function stmt) {
                    return new Stmt.Function(
                        stmt.name,
                        stmt.params,
                        stmt.returnType,
                        foldConstants(stmt.body)
                    );
                }
                
                @Override
                public Stmt visitClassStmt(Stmt.Class stmt) {
                    List<Stmt.Function> methods = new ArrayList<>();
                    for (Stmt.Function method : stmt.methods) {
                        methods.add((Stmt.Function) foldStatement(method));
                    }
                    return new Stmt.Class(stmt.name, methods);
                }
                
                @Override
                public Stmt visitImportStmt(Stmt.Import stmt) {
                    return stmt;
                }
                
                @Override
                public Stmt visitExportStmt(Stmt.Export stmt) {
                    return new Stmt.Export(foldStatement(stmt.declaration));
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
        
        private Expr foldExpression(Expr expr) {
            return expr.accept(new Expr.Visitor<Expr>() {
                @Override
                public Expr visitBinaryExpr(Expr.Binary expr) {
                    Expr left = foldExpression(expr.left);
                    Expr right = foldExpression(expr.right);
                    
                    // If both operands are literals, evaluate the operation
                    if (left instanceof Expr.Literal && right instanceof Expr.Literal) {
                        Object leftVal = ((Expr.Literal) left).value;
                        Object rightVal = ((Expr.Literal) right).value;
                        
                        Object result = evaluateBinary(expr.operator.type, leftVal, rightVal);
                        if (result != null) {
                            expressionsFolded++;
                            return new Expr.Literal(result);
                        }
                    }
                    
                    return new Expr.Binary(left, expr.operator, right);
                }
                
                @Override
                public Expr visitUnaryExpr(Expr.Unary expr) {
                    Expr operand = foldExpression(expr.right);
                    
                    if (operand instanceof Expr.Literal) {
                        Object value = ((Expr.Literal) operand).value;
                        Object result = evaluateUnary(expr.operator.type, value);
                        if (result != null) {
                            expressionsFolded++;
                            return new Expr.Literal(result);
                        }
                    }
                    
                    return new Expr.Unary(expr.operator, operand);
                }
                
                @Override
                public Expr visitGroupingExpr(Expr.Grouping expr) {
                    Expr inner = foldExpression(expr.expression);
                    if (inner instanceof Expr.Literal) {
                        return inner; // Remove unnecessary grouping
                    }
                    return new Expr.Grouping(inner);
                }
                
                @Override
                public Expr visitLiteralExpr(Expr.Literal expr) {
                    return expr;
                }
                
                @Override
                public Expr visitVariableExpr(Expr.Variable expr) {
                    return expr;
                }
                
                @Override
                public Expr visitAssignExpr(Expr.Assign expr) {
                    return new Expr.Assign(expr.name, foldExpression(expr.value));
                }
                
                @Override
                public Expr visitLogicalExpr(Expr.Logical expr) {
                    Expr left = foldExpression(expr.left);
                    
                    // Short-circuit evaluation
                    if (left instanceof Expr.Literal) {
                        Object leftVal = ((Expr.Literal) left).value;
                        
                        if (expr.operator.type == TokenType.OR_OR) {
                            if (isTruthy(leftVal)) {
                                return left; // true || anything = true
                            }
                        } else if (expr.operator.type == TokenType.AND_AND) {
                            if (!isTruthy(leftVal)) {
                                return left; // false && anything = false
                            }
                        }
                    }
                    
                    return new Expr.Logical(left, expr.operator, foldExpression(expr.right));
                }
                
                @Override
                public Expr visitCallExpr(Expr.Call expr) {
                    List<Expr> args = new ArrayList<>();
                    for (Expr arg : expr.arguments) {
                        args.add(foldExpression(arg));
                    }
                    return new Expr.Call(foldExpression(expr.callee), expr.paren, args);
                }
                
                // Other expression types - just recurse on children
                @Override
                public Expr visitGetExpr(Expr.Get expr) {
                    return new Expr.Get(foldExpression(expr.object), expr.name);
                }
                
                @Override
                public Expr visitSetExpr(Expr.Set expr) {
                    return new Expr.Set(foldExpression(expr.object), expr.name, 
                                      foldExpression(expr.value));
                }
                
                @Override
                public Expr visitThisExpr(Expr.This expr) {
                    return expr;
                }
                
                @Override
                public Expr visitListExpr(Expr.ListExpr expr) {
                    List<Expr> elements = new ArrayList<>();
                    for (Expr element : expr.elements) {
                        elements.add(foldExpression(element));
                    }
                    return new Expr.ListExpr(elements);
                }
                
                @Override
                public Expr visitDictExpr(Expr.Dict expr) {
                    List<Expr> keys = new ArrayList<>();
                    List<Expr> values = new ArrayList<>();
                    for (int i = 0; i < expr.keys.size(); i++) {
                        keys.add(foldExpression(expr.keys.get(i)));
                        values.add(foldExpression(expr.values.get(i)));
                    }
                    return new Expr.Dict(keys, values);
                }
                
                @Override
                public Expr visitIndexExpr(Expr.Index expr) {
                    return new Expr.Index(foldExpression(expr.object), expr.bracket,
                                        foldExpression(expr.index));
                }
                
                @Override
                public Expr visitIndexSetExpr(Expr.IndexSet expr) {
                    return new Expr.IndexSet(foldExpression(expr.object), expr.bracket,
                                           foldExpression(expr.index), 
                                           foldExpression(expr.value));
                }
                
                @Override
                public Expr visitSliceExpr(Expr.Slice expr) {
                    Expr foldedObject = foldExpression(expr.object);
                    Expr foldedStart = expr.start != null ? foldExpression(expr.start) : null;
                    Expr foldedEnd = expr.end != null ? foldExpression(expr.end) : null;
                    return new Expr.Slice(foldedObject, expr.bracket, foldedStart, foldedEnd);
                }
                
                @Override
                public Expr visitLambdaExpr(Expr.Lambda expr) {
                    return expr; // Don't fold inside lambdas
                }
                
                @Override
                public Expr visitMatchExpr(Expr.Match expr) {
                    Expr foldedExpr = foldExpression(expr.expr);
                    List<Expr.Match.Case> foldedCases = new ArrayList<>();
                    for (Expr.Match.Case c : expr.cases) {
                        Expr foldedPattern = foldExpression(c.pattern);
                        Expr foldedGuard = c.guard != null ? foldExpression(c.guard) : null;
                        Expr foldedValue = foldExpression(c.value);
                        foldedCases.add(new Expr.Match.Case(foldedPattern, foldedGuard, foldedValue));
                    }
                    return new Expr.Match(foldedExpr, foldedCases);
                }
                
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
        
        private Object evaluateBinary(TokenType op, Object left, Object right) {
            try {
                switch (op) {
                    case PLUS:
                        if (left instanceof Double && right instanceof Double) {
                            return (Double) left + (Double) right;
                        }
                        if (left instanceof String && right instanceof String) {
                            return (String) left + (String) right;
                        }
                        break;
                        
                    case MINUS:
                        if (left instanceof Double && right instanceof Double) {
                            return (Double) left - (Double) right;
                        }
                        break;
                        
                    case STAR:
                        if (left instanceof Double && right instanceof Double) {
                            return (Double) left * (Double) right;
                        }
                        break;
                        
                    case SLASH:
                        if (left instanceof Double && right instanceof Double) {
                            double divisor = (Double) right;
                            if (divisor != 0) {
                                return (Double) left / divisor;
                            }
                        }
                        break;
                        
                    case PERCENT:
                        if (left instanceof Double && right instanceof Double) {
                            return (Double) left % (Double) right;
                        }
                        break;
                        
                    case STAR_STAR:
                        if (left instanceof Double && right instanceof Double) {
                            return Math.pow((Double) left, (Double) right);
                        }
                        break;
                        
                    case GREATER:
                        if (left instanceof Double && right instanceof Double) {
                            return (Double) left > (Double) right;
                        }
                        break;
                        
                    case GREATER_EQUAL:
                        if (left instanceof Double && right instanceof Double) {
                            return (Double) left >= (Double) right;
                        }
                        break;
                        
                    case LESS:
                        if (left instanceof Double && right instanceof Double) {
                            return (Double) left < (Double) right;
                        }
                        break;
                        
                    case LESS_EQUAL:
                        if (left instanceof Double && right instanceof Double) {
                            return (Double) left <= (Double) right;
                        }
                        break;
                        
                    case EQUAL_EQUAL:
                        return isEqual(left, right);
                        
                    case BANG_EQUAL:
                        return !isEqual(left, right);
                }
            } catch (Exception e) {
                // If evaluation fails, don't fold
            }
            
            return null;
        }
        
        private Object evaluateUnary(TokenType op, Object operand) {
            switch (op) {
                case MINUS:
                    if (operand instanceof Double) {
                        return -(Double) operand;
                    }
                    break;
                    
                case BANG:
                    return !isTruthy(operand);
            }
            
            return null;
        }
        
        private boolean isTruthy(Object value) {
            if (value == null) return false;
            if (value instanceof Boolean) return (Boolean) value;
            return true;
        }
        
        private boolean isEqual(Object a, Object b) {
            if (a == null && b == null) return true;
            if (a == null) return false;
            return a.equals(b);
        }
    }
}