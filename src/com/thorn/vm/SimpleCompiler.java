package com.thorn.vm;

import java.util.*;
import com.thorn.Stmt;
import com.thorn.Expr;
import com.thorn.TokenType;

/**
 * Simple bytecode compiler for basic Thorn features.
 * Initial implementation focusing on core functionality.
 */
public class SimpleCompiler {
    private final ConstantPool constantPool;
    private final List<Integer> bytecode;
    private final Map<String, Integer> locals;      
    private final Stack<Integer> registerStack;
    private final Map<Integer, Boolean> numericRegisters; // Track which registers hold numbers
    private int loopDepth = 0; // Track nested loop depth
    
    private int nextRegister = 0;
    
    public SimpleCompiler() {
        this.constantPool = new ConstantPool();
        this.bytecode = new ArrayList<>();
        this.locals = new HashMap<>();
        this.registerStack = new Stack<>();
        this.numericRegisters = new HashMap<>();
        
        // Initialize register pool
        for (int i = 255; i >= 0; i--) {
            registerStack.push(i);
        }
    }
    
    /**
     * Compile a list of statements into bytecode.
     */
    public CompilationResult compile(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                compileStatement(stmt);
            }
            
            // Add HALT instruction at the end
            emit(Instruction.create(OpCode.HALT));
            
            // Create function info for the main script
            FunctionInfo mainFunction = new FunctionInfo.Builder("<script>")
                .arity(0)
                .localCount(locals.size())
                .upvalueCount(0)
                .bytecode(bytecode.stream().mapToInt(i -> i).toArray())
                .startPc(0)
                .build();
            
            constantPool.addFunction(mainFunction);
            
            return new CompilationResult(constantPool, 
                                       bytecode.stream().mapToInt(i -> i).toArray(),
                                       mainFunction);
        } catch (Exception e) {
            throw new RuntimeException("Compilation failed: " + e.getMessage(), e);
        }
    }
    
    // Emit an instruction
    private void emit(int instruction) {
        bytecode.add(instruction);
    }
    
    // Allocate a new register
    private int allocateRegister() {
        if (registerStack.isEmpty()) {
            throw new RuntimeException("Out of registers");
        }
        return registerStack.pop();
    }
    
    // Free a register
    private void freeRegister(int register) {
        registerStack.push(register);
    }
    
    // Get or allocate register for a variable
    private int getLocalRegister(String name) {
        Integer reg = locals.get(name);
        if (reg != null) {
            return reg;
        }
        
        reg = allocateRegister();
        locals.put(name, reg);
        return reg;
    }
    
    // Compile a statement
    private void compileStatement(Stmt stmt) {
        if (stmt instanceof Stmt.Expression) {
            Stmt.Expression exprStmt = (Stmt.Expression) stmt;
            Integer resultReg = compileExpression(exprStmt.expression);
            if (resultReg != null) {
                freeRegister(resultReg);
            }
        } else if (stmt instanceof Stmt.Var) {
            Stmt.Var varStmt = (Stmt.Var) stmt;
            int reg = getLocalRegister(varStmt.name.lexeme);
            
            if (varStmt.initializer != null) {
                Integer valueReg = compileExpression(varStmt.initializer);
                if (valueReg != reg) {
                    emit(Instruction.create(OpCode.MOVE, reg, valueReg));
                    freeRegister(valueReg);
                }
            } else {
                // Initialize to null
                int nullIndex = constantPool.addConstant(null);
                emit(Instruction.createWithConstantB(OpCode.LOAD_CONSTANT, reg, nullIndex, 0));
            }
        } else if (stmt instanceof Stmt.Block) {
            Stmt.Block blockStmt = (Stmt.Block) stmt;
            // Save current local scope
            Map<String, Integer> savedLocals = new HashMap<>(locals);
            
            for (Stmt statement : blockStmt.statements) {
                compileStatement(statement);
            }
            
            // Restore local scope (simple scope management)
            for (Map.Entry<String, Integer> entry : locals.entrySet()) {
                if (!savedLocals.containsKey(entry.getKey())) {
                    freeRegister(entry.getValue());
                }
            }
            locals.clear();
            locals.putAll(savedLocals);
        } else if (stmt instanceof Stmt.If) {
            compileIfStatement((Stmt.If) stmt);
        } else if (stmt instanceof Stmt.While) {
            compileWhileStatement((Stmt.While) stmt);
        } else if (stmt instanceof Stmt.For) {
            // System.err.println("DEBUG: Found For statement, compiling...");
            compileForStatement((Stmt.For) stmt);
        } else if (stmt instanceof Stmt.Function) {
            compileFunctionStatement((Stmt.Function) stmt);
        } else if (stmt instanceof Stmt.Return) {
            compileReturnStatement((Stmt.Return) stmt);
        } else if (stmt instanceof Stmt.Class) {
            compileClassStatement((Stmt.Class) stmt);
        } else if (stmt instanceof Stmt.Export) {
            compileExportStatement((Stmt.Export) stmt);
        } else if (stmt instanceof Stmt.Import) {
            compileImportStatement((Stmt.Import) stmt);
        } else if (stmt instanceof Stmt.TypeAlias) {
            compileTypeAliasStatement((Stmt.TypeAlias) stmt);
        } else {
            System.err.println("Warning: Unsupported statement type: " + stmt.getClass().getSimpleName());
        }
    }
    
    // Compile an expression
    private Integer compileExpression(Expr expr) {
        if (expr instanceof Expr.Literal) {
            Expr.Literal literalExpr = (Expr.Literal) expr;
            int reg = allocateRegister();
            int constantIndex = constantPool.addConstant(literalExpr.value);
            emit(Instruction.createWithConstantB(OpCode.LOAD_CONSTANT, reg, constantIndex, 0));
            
            // Track if this register holds a number
            if (literalExpr.value instanceof Double) {
                numericRegisters.put(reg, true);
            }
            
            return reg;
        } else if (expr instanceof Expr.Variable) {
            Expr.Variable varExpr = (Expr.Variable) expr;
            String name = varExpr.name.lexeme;
            Integer localReg = locals.get(name);
            
            if (localReg != null) {
                // Local variable - just return the register
                // Numeric type info should already be tracked for this register
                return localReg;
            }
            
            // Global variable
            int reg = allocateRegister();
            int nameIndex = constantPool.addString(name);
            emit(Instruction.createWithConstantB(OpCode.LOAD_GLOBAL, reg, nameIndex, 0));
            return reg;
        } else if (expr instanceof Expr.Assign) {
            Expr.Assign assignExpr = (Expr.Assign) expr;
            String name = assignExpr.name.lexeme;
            Integer localReg = locals.get(name);
            
            // Check for increment pattern: i = i + 1
            if (localReg != null && assignExpr.value instanceof Expr.Binary) {
                Expr.Binary binaryValue = (Expr.Binary) assignExpr.value;
                if (binaryValue.operator.type == TokenType.PLUS) {
                    // Check for i = i + 1 pattern
                    if (binaryValue.left instanceof Expr.Variable && 
                        binaryValue.right instanceof Expr.Literal) {
                        Expr.Variable var = (Expr.Variable) binaryValue.left;
                        Expr.Literal lit = (Expr.Literal) binaryValue.right;
                        
                        if (var.name.lexeme.equals(name) && 
                            lit.value instanceof Double && 
                            (Double)lit.value == 1.0) {
                            // Use INCREMENT_LOCAL superinstruction
                            emit(Instruction.create(OpCode.INCREMENT_LOCAL, localReg));
                            return localReg;
                        }
                    }
                }
            }
            
            // Standard assignment
            Integer valueReg = compileExpression(assignExpr.value);
            
            if (localReg != null) {
                // Local variable assignment
                if (valueReg != localReg) {
                    emit(Instruction.create(OpCode.MOVE, localReg, valueReg));
                    freeRegister(valueReg);
                }
                return localReg;
            } else {
                // Global variable assignment
                int nameIndex = constantPool.addString(name);
                emit(Instruction.create(OpCode.STORE_GLOBAL, nameIndex, valueReg, 0));
                return valueReg;
            }
        } else if (expr instanceof Expr.Binary) {
            Expr.Binary binaryExpr = (Expr.Binary) expr;
            
            // Check for superinstruction patterns
            if (binaryExpr.operator.type == TokenType.PLUS) {
                // Pattern: constant + local
                if (binaryExpr.left instanceof Expr.Literal && binaryExpr.right instanceof Expr.Variable) {
                    Expr.Literal literal = (Expr.Literal) binaryExpr.left;
                    Expr.Variable var = (Expr.Variable) binaryExpr.right;
                    Integer localReg = locals.get(var.name.lexeme);
                    
                    if (localReg != null && literal.value instanceof Double) {
                        int resultReg = allocateRegister();
                        int constIndex = constantPool.addConstant(literal.value);
                        emit(Instruction.createWithConstantB(OpCode.ADD_CONST_TO_LOCAL, resultReg, constIndex, localReg));
                        numericRegisters.put(resultReg, true);
                        return resultReg;
                    }
                }
                
                // Pattern: local + local
                if (binaryExpr.left instanceof Expr.Variable && binaryExpr.right instanceof Expr.Variable) {
                    Expr.Variable leftVar = (Expr.Variable) binaryExpr.left;
                    Expr.Variable rightVar = (Expr.Variable) binaryExpr.right;
                    Integer leftLocal = locals.get(leftVar.name.lexeme);
                    Integer rightLocal = locals.get(rightVar.name.lexeme);
                    
                    if (leftLocal != null && rightLocal != null) {
                        int resultReg = allocateRegister();
                        emit(Instruction.create(OpCode.ADD_LOCALS, resultReg, leftLocal, rightLocal));
                        // Assume numeric if both locals exist
                        numericRegisters.put(resultReg, true);
                        return resultReg;
                    }
                }
            }
            
            // Standard binary expression compilation
            Integer leftReg = compileExpression(binaryExpr.left);
            Integer rightReg = compileExpression(binaryExpr.right);
            int resultReg = allocateRegister();
            
            // Check if we can use fast arithmetic opcodes
            // Disable fast opcodes for now due to unreliable numeric tracking
            // TODO: Improve numeric register tracking to enable fast opcodes safely
            boolean canUseFast = false;
            
            OpCode opcode = canUseFast ? 
                getFastArithmeticOpCode(binaryExpr.operator.type) : 
                getArithmeticOpCode(binaryExpr.operator.type);
            
            // Debug: print when using fast opcodes
            if (System.getProperty("thorn.debug.fastops") != null) {
                System.err.println("Arithmetic operation: " + binaryExpr.operator.type + 
                                 ", leftNumeric=" + numericRegisters.getOrDefault(leftReg, false) +
                                 ", rightNumeric=" + numericRegisters.getOrDefault(rightReg, false) +
                                 ", canUseFast=" + canUseFast +
                                 ", opcode=" + opcode);
            }
            
            emit(Instruction.create(opcode, resultReg, leftReg, rightReg));
            
            // Mark result as numeric if we're doing arithmetic
            if (isArithmeticOperator(binaryExpr.operator.type)) {
                numericRegisters.put(resultReg, true);
            }
            
            freeRegister(leftReg);
            freeRegister(rightReg);
            return resultReg;
        } else if (expr instanceof Expr.Unary) {
            Expr.Unary unaryExpr = (Expr.Unary) expr;
            Integer operandReg = compileExpression(unaryExpr.right);
            int resultReg = allocateRegister();
            
            switch (unaryExpr.operator.type) {
                case MINUS:
                    emit(Instruction.create(OpCode.NEG, resultReg, operandReg));
                    break;
                case BANG:
                    emit(Instruction.create(OpCode.NOT, resultReg, operandReg));
                    break;
                default:
                    throw new RuntimeException("Unsupported unary operator: " + unaryExpr.operator.type);
            }
            
            freeRegister(operandReg);
            return resultReg;
        } else if (expr instanceof Expr.Call) {
            return compileCall((Expr.Call) expr);
        } else if (expr instanceof Expr.Get) {
            return compileGetExpression((Expr.Get) expr);
        } else if (expr instanceof Expr.Set) {
            return compileSetExpression((Expr.Set) expr);
        } else if (expr instanceof Expr.IndexSet) {
            return compileIndexSetExpression((Expr.IndexSet) expr);
        } else if (expr instanceof Expr.Index) {
            return compileIndexExpression((Expr.Index) expr);
        } else if (expr instanceof Expr.Slice) {
            return compileSliceExpression((Expr.Slice) expr);
        } else if (expr instanceof Expr.Grouping) {
            Expr.Grouping groupingExpr = (Expr.Grouping) expr;
            return compileExpression(groupingExpr.expression);
        } else if (expr instanceof Expr.Lambda) {
            return compileLambdaExpression((Expr.Lambda) expr);
        } else if (expr instanceof Expr.ListExpr) {
            return compileListExpression((Expr.ListExpr) expr);
        } else if (expr instanceof Expr.Dict) {
            return compileDictExpression((Expr.Dict) expr);
        } else if (expr instanceof Expr.Logical) {
            return compileLogicalExpression((Expr.Logical) expr);
        } else if (expr instanceof Expr.This) {
            return compileThisExpression((Expr.This) expr);
        } else {
            System.err.println("Warning: Unsupported expression type: " + expr.getClass().getSimpleName());
            // Return a null literal register
            int reg = allocateRegister();
            int nullIndex = constantPool.addConstant(null);
            emit(Instruction.createWithConstantB(OpCode.LOAD_CONSTANT, reg, nullIndex, 0));
            return reg;
        }
    }
    
    private OpCode getArithmeticOpCode(TokenType operator) {
        switch (operator) {
            case PLUS: return OpCode.ADD;
            case MINUS: return OpCode.SUB;
            case STAR: return OpCode.MUL;
            case SLASH: return OpCode.DIV;
            case PERCENT: return OpCode.MOD;
            case STAR_STAR: return OpCode.POW;
            case EQUAL_EQUAL: return OpCode.EQ;
            case BANG_EQUAL: return OpCode.NE;
            case LESS: return OpCode.LT;
            case LESS_EQUAL: return OpCode.LE;
            case GREATER: return OpCode.GT;
            case GREATER_EQUAL: return OpCode.GE;
            case QUESTION_QUESTION: return OpCode.NULL_COALESCE;
            default:
                throw new RuntimeException("Unsupported operator: " + operator);
        }
    }
    
    private Integer compileCall(Expr.Call callExpr) {
        // Handle special built-in functions first
        if (callExpr.callee instanceof Expr.Variable) {
            Expr.Variable funcVar = (Expr.Variable) callExpr.callee;
            String funcName = funcVar.name.lexeme;
            
            // Handle print() function
            if ("print".equals(funcName)) {
                if (callExpr.arguments.size() != 1) {
                    throw new RuntimeException("print() expects exactly 1 argument");
                }
                
                Integer argReg = compileExpression(callExpr.arguments.get(0));
                emit(Instruction.create(OpCode.PRINT, argReg, 0));
                freeRegister(argReg);
                
                // Return a null literal for print (which returns void)
                int resultReg = allocateRegister();
                int nullIndex = constantPool.addConstant(null);
                emit(Instruction.createWithConstantB(OpCode.LOAD_CONSTANT, resultReg, nullIndex, 0));
                return resultReg;
            }
        }
        
        // General function call handling - compile callee
        Integer calleeReg = compileExpression(callExpr.callee);
        
        // Compile arguments into consecutive registers starting from a base
        List<Integer> argRegs = new ArrayList<>();
        for (Expr arg : callExpr.arguments) {
            Integer argReg = compileExpression(arg);
            argRegs.add(argReg);
        }
        
        // Emit CALL instruction: function register, arg count
        emit(Instruction.create(OpCode.CALL, calleeReg, callExpr.arguments.size(), 0));
        
        // The result will be placed in register 0 by the VM
        int resultReg = 0;
        
        // Free argument registers
        for (Integer argReg : argRegs) {
            freeRegister(argReg);
        }
        freeRegister(calleeReg);
        
        return resultReg;
    }
    
    private void compileIfStatement(Stmt.If ifStmt) {
        // Compile condition
        Integer conditionReg = compileExpression(ifStmt.condition);
        
        // Use CMP_JUMP_IF_FALSE superinstruction for direct jumps
        int jumpToElse = bytecode.size();
        emit(Instruction.createConditionalJump(OpCode.CMP_JUMP_IF_FALSE, conditionReg, 0)); // Patch later
        freeRegister(conditionReg);
        
        // Compile then branch
        compileStatement(ifStmt.thenBranch);
        
        // If there's an else branch, emit unconditional jump to skip it
        int jumpToEnd = -1;
        if (ifStmt.elseBranch != null) {
            jumpToEnd = bytecode.size();
            emit(Instruction.createJump(OpCode.JUMP, 0)); // Patch later
        }
        
        // Patch the conditional jump to point here (else branch or end)
        int elseLabel = bytecode.size();
        patchJump(jumpToElse, elseLabel);
        
        // Compile else branch if present
        if (ifStmt.elseBranch != null) {
            compileStatement(ifStmt.elseBranch);
            
            // Patch the unconditional jump to point here (end)
            int endLabel = bytecode.size();
            patchJump(jumpToEnd, endLabel);
        }
    }
    
    private void compileWhileStatement(Stmt.While whileStmt) {
        int loopStart = bytecode.size();
        
        // Compile condition fresh on each iteration to avoid register conflicts
        Integer conditionReg = compileExpression(whileStmt.condition);
        
        // Emit conditional jump - if false, jump to end
        int jumpToEnd = bytecode.size();
        emit(Instruction.createConditionalJump(OpCode.JUMP_IF_FALSE, conditionReg, 0)); // Patch later
        
        // Free condition register to prevent conflicts
        freeRegister(conditionReg);
        
        // Compile body
        compileStatement(whileStmt.body);
        
        // Jump back to loop start (this will re-evaluate the condition with fresh registers)
        int jumpOffset = loopStart - (bytecode.size() + 1);
        emit(Instruction.createJump(OpCode.JUMP, jumpOffset));
        
        // Patch the conditional jump to point here (end)
        int endLabel = bytecode.size();
        patchJump(jumpToEnd, endLabel);
    }
    
    private void compileForStatement(Stmt.For forStmt) {
        // Compile the iterable expression
        Integer iterableReg = compileExpression(forStmt.iterable);
        
        // We need to implement list iteration
        // For now, we'll use a simple index-based approach
        
        // Use high-numbered registers for loop control to avoid conflicts
        // Allocate from the top of the register space, working downward
        int baseReg = 250 - (loopDepth * 3);      // Base register for this loop depth
        int indexReg = baseReg;                   // Index register 
        int lengthReg = baseReg + 1;              // Length register
        int tempReg = baseReg + 2;                // Temp register
        int elementReg = getLocalRegister(forStmt.variable.lexeme);
        
        loopDepth++; // Increment for nested loops
        
        // Initialize index to 0
        int zeroIndex = constantPool.addConstant(0.0);
        emit(Instruction.createWithConstantB(OpCode.LOAD_CONSTANT, indexReg, zeroIndex, 0));
        
        // Get list length using ARRAY_LENGTH opcode
        emit(Instruction.create(OpCode.ARRAY_LENGTH, lengthReg, iterableReg));
        
        // Loop start - re-evaluate everything fresh to avoid register conflicts
        int loopStart = bytecode.size();
        
        // Reload array length (in case it was corrupted)
        emit(Instruction.create(OpCode.ARRAY_LENGTH, lengthReg, iterableReg));
        
        // Check if index < length - use temp register for condition to avoid conflicts
        emit(Instruction.create(OpCode.LT, tempReg, indexReg, lengthReg));
        
        // Jump to end if condition is false
        int jumpToEnd = bytecode.size();
        emit(Instruction.createConditionalJump(OpCode.JUMP_IF_FALSE, tempReg, 0)); // Patch later
        
        // Get element at current index
        emit(Instruction.create(OpCode.GET_INDEX, elementReg, iterableReg, indexReg));
        
        // Execute loop body
        compileStatement(forStmt.body);
        
        // Increment index using our temp register
        int oneIndex = constantPool.addConstant(1.0);
        emit(Instruction.createWithConstantB(OpCode.LOAD_CONSTANT, tempReg, oneIndex, 0));
        emit(Instruction.create(OpCode.ADD, indexReg, indexReg, tempReg));
        
        // Jump back to loop start
        int jumpOffset = loopStart - (bytecode.size() + 1);
        emit(Instruction.createJump(OpCode.JUMP, jumpOffset));
        
        // Patch the conditional jump to point here (end)
        int endLabel = bytecode.size();
        if (System.getProperty("thorn.debug.jumps") != null) {
            System.err.println("FOR: Patching jump at " + jumpToEnd + " to point to " + endLabel);
        }
        patchJump(jumpToEnd, endLabel);
        
        // Decrement loop depth
        loopDepth--;
        
        // Free the iterable register (reserved registers don't need freeing)
        freeRegister(iterableReg);
    }
    
    private void patchJump(int jumpIndex, int targetLabel) {
        int jumpOffset = targetLabel - (jumpIndex + 1);
        
        // Get the original instruction and extract opcode and register info
        int originalInstruction = bytecode.get(jumpIndex);
        OpCode opcode = OpCode.getOpcode(originalInstruction);
        int conditionReg = OpCode.getA(originalInstruction);
        
        // Create new instruction with patched offset
        int patchedInstruction;
        if (opcode == OpCode.JUMP) {
            patchedInstruction = Instruction.createJump(OpCode.JUMP, jumpOffset);
        } else {
            patchedInstruction = Instruction.createConditionalJump(opcode, conditionReg, jumpOffset);
        }
        
        bytecode.set(jumpIndex, patchedInstruction);
    }
    
    private void compileFunctionStatement(Stmt.Function funcStmt) {
        String funcName = funcStmt.name.lexeme;
        int arity = funcStmt.params.size();
        
        // Create a new compiler for the function body
        SimpleCompiler funcCompiler = new SimpleCompiler();
        
        // Add parameters as local variables
        for (int i = 0; i < funcStmt.params.size(); i++) {
            String paramName = funcStmt.params.get(i).name.lexeme;
            funcCompiler.locals.put(paramName, i); // Use parameter index as register
        }
        
        // Compile function body
        for (Stmt stmt : funcStmt.body) {
            funcCompiler.compileStatement(stmt);
        }
        
        // Add implicit return null if no explicit return
        funcCompiler.emit(Instruction.create(OpCode.LOAD_CONSTANT, 0, funcCompiler.constantPool.addConstant(null)));
        funcCompiler.emit(Instruction.create(OpCode.RETURN, 0));
        funcCompiler.emit(Instruction.create(OpCode.HALT));
        
        // Convert bytecode list to array
        int[] bytecodeArray = new int[funcCompiler.bytecode.size()];
        for (int i = 0; i < funcCompiler.bytecode.size(); i++) {
            bytecodeArray[i] = funcCompiler.bytecode.get(i);
        }
        
        // Create function info and add to constant pool
        FunctionInfo functionInfo = new FunctionInfo.Builder(funcName)
            .arity(arity)
            .localCount(funcCompiler.locals.size())
            .upvalueCount(0) // TODO: Handle upvalues later
            .bytecode(bytecodeArray)
            .build();
        int functionIndex = constantPool.addFunction(functionInfo);
        
        // Store function in global variable
        int reg = allocateRegister();
        emit(Instruction.create(OpCode.MAKE_CLOSURE, reg, functionIndex, 0));
        
        int nameIndex = constantPool.addString(funcName);
        emit(Instruction.create(OpCode.STORE_GLOBAL, nameIndex, reg, 0));
        freeRegister(reg);
    }
    
    private void compileReturnStatement(Stmt.Return returnStmt) {
        if (returnStmt.value != null) {
            Integer valueReg = compileExpression(returnStmt.value);
            emit(Instruction.create(OpCode.RETURN, valueReg));
            freeRegister(valueReg);
        } else {
            // Return null
            int reg = allocateRegister();
            int nullIndex = constantPool.addConstant(null);
            emit(Instruction.createWithConstantB(OpCode.LOAD_CONSTANT, reg, nullIndex, 0));
            emit(Instruction.create(OpCode.RETURN, reg));
            freeRegister(reg);
        }
    }
    
    private void compileClassStatement(Stmt.Class classStmt) {
        String className = classStmt.name.lexeme;
        
        // Create a new object to represent the class
        int classReg = allocateRegister();
        emit(Instruction.create(OpCode.NEW_OBJECT, classReg));
        
        // Compile each method and add to the class object
        for (Stmt.Function method : classStmt.methods) {
            String methodName = method.name.lexeme;
            int arity = method.params.size();
            
            // Create a new compiler for the method body
            SimpleCompiler methodCompiler = new SimpleCompiler();
            
            // Add parameters as local variables
            for (int i = 0; i < method.params.size(); i++) {
                String paramName = method.params.get(i).name.lexeme;
                methodCompiler.locals.put(paramName, i);
            }
            
            // Compile method body
            for (Stmt stmt : method.body) {
                methodCompiler.compileStatement(stmt);
            }
            
            // Add implicit return null if no explicit return
            methodCompiler.emit(Instruction.create(OpCode.LOAD_CONSTANT, 0, methodCompiler.constantPool.addConstant(null)));
            methodCompiler.emit(Instruction.create(OpCode.RETURN, 0));
            methodCompiler.emit(Instruction.create(OpCode.HALT));
            
            // Convert bytecode to array
            int[] bytecodeArray = new int[methodCompiler.bytecode.size()];
            for (int i = 0; i < methodCompiler.bytecode.size(); i++) {
                bytecodeArray[i] = methodCompiler.bytecode.get(i);
            }
            
            // Create function info
            FunctionInfo methodInfo = new FunctionInfo.Builder(methodName)
                .arity(arity)
                .localCount(methodCompiler.locals.size())
                .upvalueCount(0)
                .bytecode(bytecodeArray)
                .build();
            
            int methodIndex = constantPool.addFunction(methodInfo);
            
            // Create closure for the method
            int methodReg = allocateRegister();
            emit(Instruction.create(OpCode.MAKE_CLOSURE, methodReg, methodIndex, 0));
            
            // Add method to class object  
            int methodNameIndex = constantPool.addString(methodName);
            emit(Instruction.create(OpCode.SET_PROPERTY, methodNameIndex, classReg, methodReg));
            
            freeRegister(methodReg);
        }
        
        // Store class in global variable
        int nameIndex = constantPool.addString(className);
        emit(Instruction.create(OpCode.STORE_GLOBAL, nameIndex, classReg, 0));
        freeRegister(classReg);
    }
    
    private Integer compileGetExpression(Expr.Get getExpr) {
        // Compile object expression
        Integer objectReg = compileExpression(getExpr.object);
        
        // Get property name
        String propName = getExpr.name.lexeme;
        int propNameIndex = constantPool.addString(propName);
        
        // Emit GET_PROPERTY instruction
        int resultReg = allocateRegister();
        emit(Instruction.createWithConstantC(OpCode.GET_PROPERTY, resultReg, objectReg, propNameIndex));
        
        freeRegister(objectReg);
        return resultReg;
    }
    
    private Integer compileSetExpression(Expr.Set setExpr) {
        // Compile object expression
        Integer objectReg = compileExpression(setExpr.object);
        
        // Compile value expression
        Integer valueReg = compileExpression(setExpr.value);
        
        // Get property name
        String propName = setExpr.name.lexeme;
        int propNameIndex = constantPool.addString(propName);
        
        // Emit SET_PROPERTY instruction  
        emit(Instruction.create(OpCode.SET_PROPERTY, propNameIndex, objectReg, valueReg));
        
        freeRegister(objectReg);
        return valueReg; // Return the assigned value
    }
    
    private Integer compileLambdaExpression(Expr.Lambda lambdaExpr) {
        // Create a new compiler for the lambda body
        SimpleCompiler lambdaCompiler = new SimpleCompiler();
        
        // Add parameters as local variables
        for (int i = 0; i < lambdaExpr.params.size(); i++) {
            String paramName = lambdaExpr.params.get(i).lexeme;
            lambdaCompiler.locals.put(paramName, i); // Use parameter index as register
        }
        
        // Compile lambda body
        for (Stmt stmt : lambdaExpr.body) {
            lambdaCompiler.compileStatement(stmt);
        }
        
        // Add implicit return null if no explicit return
        lambdaCompiler.emit(Instruction.create(OpCode.LOAD_CONSTANT, 0, lambdaCompiler.constantPool.addConstant(null)));
        lambdaCompiler.emit(Instruction.create(OpCode.RETURN, 0));
        lambdaCompiler.emit(Instruction.create(OpCode.HALT));
        
        // Convert bytecode list to array
        int[] bytecodeArray = new int[lambdaCompiler.bytecode.size()];
        for (int i = 0; i < lambdaCompiler.bytecode.size(); i++) {
            bytecodeArray[i] = lambdaCompiler.bytecode.get(i);
        }
        
        // Create function info for the lambda
        FunctionInfo lambdaInfo = new FunctionInfo.Builder("<lambda>")
            .arity(lambdaExpr.params.size())
            .localCount(lambdaCompiler.locals.size())
            .upvalueCount(0) // TODO: Handle upvalues for closures
            .bytecode(bytecodeArray)
            .build();
        
        int functionIndex = constantPool.addFunction(lambdaInfo);
        
        // Create closure and return it
        int reg = allocateRegister();
        emit(Instruction.create(OpCode.MAKE_CLOSURE, reg, functionIndex, 0));
        
        return reg;
    }
    
    private boolean isArithmeticOperator(TokenType type) {
        return type == TokenType.PLUS || type == TokenType.MINUS ||
               type == TokenType.STAR || type == TokenType.SLASH ||
               type == TokenType.PERCENT || type == TokenType.STAR_STAR ||
               type == TokenType.EQUAL_EQUAL || type == TokenType.BANG_EQUAL ||
               type == TokenType.LESS || type == TokenType.LESS_EQUAL ||
               type == TokenType.GREATER || type == TokenType.GREATER_EQUAL ||
               type == TokenType.QUESTION_QUESTION;
    }
    
    private boolean isNumericExpression(Expr expr) {
        if (expr instanceof Expr.Literal) {
            Object value = ((Expr.Literal) expr).value;
            return value instanceof Double;
        } else if (expr instanceof Expr.Binary) {
            Expr.Binary binary = (Expr.Binary) expr;
            // Arithmetic operations on numbers produce numbers
            return isArithmeticOperator(binary.operator.type) &&
                   isNumericExpression(binary.left) &&
                   isNumericExpression(binary.right);
        } else if (expr instanceof Expr.Unary) {
            Expr.Unary unary = (Expr.Unary) expr;
            // Unary minus on number produces number
            return unary.operator.type == TokenType.MINUS &&
                   isNumericExpression(unary.right);
        }
        // TODO: Add more cases for variables known to be numbers
        return false;
    }
    
    private OpCode getFastArithmeticOpCode(TokenType operator) {
        switch (operator) {
            case PLUS: return OpCode.ADD_FAST;
            case MINUS: return OpCode.SUB_FAST;
            case STAR: return OpCode.MUL_FAST;
            case SLASH: return OpCode.DIV_FAST;
            default:
                throw new RuntimeException("No fast opcode for operator: " + operator);
        }
    }
    
    private Integer compileListExpression(Expr.ListExpr listExpr) {
        // For now, create a list by loading it as a constant
        // In a production VM, you'd use NEW_ARRAY opcode
        java.util.List<Object> list = new java.util.ArrayList<>();
        
        // Evaluate all elements at compile time for constants
        // For non-constant expressions, we'd need NEW_ARRAY opcode
        for (Expr element : listExpr.elements) {
            if (element instanceof Expr.Literal) {
                list.add(((Expr.Literal) element).value);
            } else {
                // For now, compile complex lists at runtime
                int listReg = allocateRegister();
                int listIndex = constantPool.addConstant(new java.util.ArrayList<>());
                emit(Instruction.createWithConstantB(OpCode.LOAD_CONSTANT, listReg, listIndex, 0));
                
                // Compile each element and add to list
                for (Expr elem : listExpr.elements) {
                    Integer elemReg = compileExpression(elem);
                    emit(Instruction.create(OpCode.ARRAY_PUSH, listReg, elemReg));
                    freeRegister(elemReg);
                }
                
                return listReg;
            }
        }
        
        // All elements were literals, create constant list
        int reg = allocateRegister();
        int listIndex = constantPool.addConstant(list);
        emit(Instruction.createWithConstantB(OpCode.LOAD_CONSTANT, reg, listIndex, 0));
        return reg;
    }
    
    private Integer compileIndexSetExpression(Expr.IndexSet indexSetExpr) {
        // Compile the object being indexed (array or dictionary)
        Integer objectReg = compileExpression(indexSetExpr.object);
        
        // Compile the index expression
        Integer indexReg = compileExpression(indexSetExpr.index);
        
        // Compile the value to be assigned
        Integer valueReg = compileExpression(indexSetExpr.value);
        
        // Emit SET_INDEX instruction: object[index] = value (format: B[A] = C)
        emit(Instruction.create(OpCode.SET_INDEX, indexReg, objectReg, valueReg));
        
        // Free the object and index registers (value register is returned)
        freeRegister(objectReg);
        freeRegister(indexReg);
        
        // Return the assigned value (assignment expressions return the assigned value)
        return valueReg;
    }
    
    private Integer compileIndexExpression(Expr.Index indexExpr) {
        // Compile the object being indexed (array or dictionary)
        Integer objectReg = compileExpression(indexExpr.object);
        
        // Compile the index expression
        Integer indexReg = compileExpression(indexExpr.index);
        
        // Allocate result register
        int resultReg = allocateRegister();
        
        // Emit GET_INDEX instruction: result = object[index]
        emit(Instruction.create(OpCode.GET_INDEX, resultReg, objectReg, indexReg));
        
        // Free temporary registers
        freeRegister(objectReg);
        freeRegister(indexReg);
        
        // Return the result register
        return resultReg;
    }
    
    private Integer compileSliceExpression(Expr.Slice sliceExpr) {
        // Convert slice notation to method call: array.slice(start, end)
        // This follows the same pattern as compileCall but needs special handling
        // for optional arguments
        
        // Compile the array expression
        Integer objectReg = compileExpression(sliceExpr.object);
        
        // Get the slice method
        int sliceMethodReg = allocateRegister();
        int sliceNameIndex = constantPool.addString("slice");
        emit(Instruction.createWithConstantC(OpCode.GET_PROPERTY, sliceMethodReg, objectReg, sliceNameIndex));
        freeRegister(objectReg);
        
        // Now compile it like a regular method call
        // The VM expects the function in register A and arguments starting from register 1
        // So we compile arguments normally and let the VM handle register placement
        
        List<Integer> argRegs = new ArrayList<>();
        
        if (sliceExpr.start == null && sliceExpr.end == null) {
            // arr[:] -> arr.slice()
            // No arguments
        } else if (sliceExpr.start != null && sliceExpr.end == null) {
            // arr[start:] -> arr.slice(start)
            Integer startReg = compileExpression(sliceExpr.start);
            argRegs.add(startReg);
        } else if (sliceExpr.start == null && sliceExpr.end != null) {
            // arr[:end] -> arr.slice(0, end)
            int zeroReg = allocateRegister();
            int zeroIndex = constantPool.addConstant(0.0);
            emit(Instruction.createWithConstantB(OpCode.LOAD_CONSTANT, zeroReg, zeroIndex, 0));
            argRegs.add(zeroReg);
            
            Integer endReg = compileExpression(sliceExpr.end);
            argRegs.add(endReg);
        } else {
            // arr[start:end] -> arr.slice(start, end)
            Integer startReg = compileExpression(sliceExpr.start);
            argRegs.add(startReg);
            
            Integer endReg = compileExpression(sliceExpr.end);
            argRegs.add(endReg);
        }
        
        // Emit CALL instruction
        emit(Instruction.create(OpCode.CALL, sliceMethodReg, argRegs.size(), 0));
        
        // Free argument registers
        for (Integer argReg : argRegs) {
            freeRegister(argReg);
        }
        freeRegister(sliceMethodReg);
        
        // The result will be placed in register 0 by the VM
        return 0;
    }
    
    private Integer compileDictExpression(Expr.Dict dictExpr) {
        // For now, compile as a runtime dictionary creation
        // Each key-value pair needs to be compiled separately
        
        // Compile all key-value pairs
        List<Integer> keyRegs = new ArrayList<>();
        List<Integer> valueRegs = new ArrayList<>();
        
        for (int i = 0; i < dictExpr.keys.size(); i++) {
            Integer keyReg = compileExpression(dictExpr.keys.get(i));
            Integer valueReg = compileExpression(dictExpr.values.get(i));
            keyRegs.add(keyReg);
            valueRegs.add(valueReg);
        }
        
        // Allocate result register
        int resultReg = allocateRegister();
        
        // Create empty dictionary
        emit(Instruction.create(OpCode.NEW_DICT, resultReg, 0, 0));
        
        // Add each key-value pair
        for (int i = 0; i < keyRegs.size(); i++) {
            emit(Instruction.create(OpCode.SET_INDEX, keyRegs.get(i), resultReg, valueRegs.get(i)));
            freeRegister(keyRegs.get(i));
            freeRegister(valueRegs.get(i));
        }
        
        return resultReg;
    }
    
    private Integer compileLogicalExpression(Expr.Logical logicalExpr) {
        // Compile both operands
        Integer leftReg = compileExpression(logicalExpr.left);
        Integer rightReg = compileExpression(logicalExpr.right);
        
        // Allocate result register
        int resultReg = allocateRegister();
        
        // Emit appropriate logical operation
        OpCode opcode = logicalExpr.operator.type == TokenType.AND_AND ? OpCode.AND : OpCode.OR;
        emit(Instruction.create(opcode, resultReg, leftReg, rightReg));
        
        // Free temporary registers
        freeRegister(leftReg);
        freeRegister(rightReg);
        
        return resultReg;
    }
    
    private void compileExportStatement(Stmt.Export exportStmt) {
        // For now, just compile the underlying declaration
        // TODO: Implement proper module export tracking
        compileStatement(exportStmt.declaration);
    }
    
    private void compileImportStatement(Stmt.Import importStmt) {
        // For now, import statements are no-ops in the VM
        // TODO: Implement proper module import system
        // The VM would need to load and execute the module file
        System.err.println("Warning: Import statements not yet fully implemented in VM");
    }
    
    private void compileTypeAliasStatement(Stmt.TypeAlias typeAliasStmt) {
        // Type aliases are compile-time only - they don't generate runtime code
        // The type system would handle the aliasing at the type-checking phase
        // For now, this is a no-op since we don't have static type checking
        // In the future, this would update a type alias table in the compiler
    }
    
    private Integer compileThisExpression(Expr.This thisExpr) {
        // For now, return a null literal for 'this'
        // TODO: Implement proper 'this' context tracking in VM
        int reg = allocateRegister();
        int nullIndex = constantPool.addConstant(null);
        emit(Instruction.createWithConstantB(OpCode.LOAD_CONSTANT, reg, nullIndex, 0));
        System.err.println("Warning: 'this' keyword not yet fully implemented in VM");
        return reg;
    }
}