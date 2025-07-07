package com.thorn.vm;

import java.util.*;

/**
 * The Thorn Virtual Machine - executes bytecode with a register-based architecture.
 * Features fast instruction dispatch and efficient call frame management.
 */
public class ThornVM {
    private static final int MAX_CALL_STACK = 1000;
    
    private final CallFrame[] callStack;
    private final Map<String, Object> globals;
    private ConstantPool constantPool;
    private int frameCount;
    private boolean halted;
    
    public ThornVM() {
        this.callStack = new CallFrame[MAX_CALL_STACK];
        this.globals = new HashMap<>();
        this.frameCount = 0;
        this.halted = false;
        initializeBuiltins();
    }
    
    public Object execute(CompilationResult compilationResult) {
        this.constantPool = compilationResult.getConstantPool();
        this.halted = false;
        
        // Set up main function frame
        FunctionInfo mainFunction = compilationResult.getMainFunction();
        pushFrame(mainFunction, -1); // No return address for main
        
        return run();
    }
    
    private Object run() {
        while (!halted && frameCount > 0) {
            CallFrame currentFrame = getCurrentFrame();
            
            if (!currentFrame.hasMoreInstructions()) {
                // Function ended without explicit return - return null
                Object returnValue = null;
                popFrame();
                if (frameCount == 0) {
                    return returnValue; // Main function return
                }
                // Set return value in caller's register 0
                getCurrentFrame().setRegister(0, returnValue);
                continue;
            }
            
            int instruction = currentFrame.getCurrentInstruction();
            currentFrame.incrementPc();
            
            OpCode opcode = OpCode.getOpcode(instruction);
            int a = OpCode.getA(instruction);
            
            // Decode operands
            Object bValue = OpCode.isBConstant(instruction) ? 
                          constantPool.getConstant(OpCode.getBValue(instruction)) :
                          currentFrame.getRegister(OpCode.getBValue(instruction));
            
            Object cValue = OpCode.isCConstant(instruction) ?
                          constantPool.getConstant(OpCode.getCValue(instruction)) :
                          currentFrame.getRegister(OpCode.getCValue(instruction));
            
            // Dispatch instruction
            switch (opcode) {
                case LOAD_CONSTANT:
                    currentFrame.setRegister(a, constantPool.getConstant(OpCode.getBValue(instruction)));
                    break;
                    
                case LOAD_LOCAL:
                    currentFrame.setRegister(a, currentFrame.getRegister(OpCode.getBValue(instruction)));
                    break;
                    
                case STORE_LOCAL:
                    currentFrame.setRegister(OpCode.getBValue(instruction), currentFrame.getRegister(a));
                    break;
                    
                case LOAD_GLOBAL:
                    String globalName = (String) constantPool.getConstant(OpCode.getBValue(instruction));
                    currentFrame.setRegister(a, globals.get(globalName));
                    break;
                    
                case STORE_GLOBAL:
                    String storeName = (String) constantPool.getConstant(a);
                    globals.put(storeName, currentFrame.getRegister(OpCode.getBValue(instruction)));
                    break;
                    
                case MOVE:
                    currentFrame.setRegister(a, bValue);
                    break;
                    
                // Arithmetic operations
                case ADD:
                    currentFrame.setRegister(a, add(bValue, cValue));
                    break;
                    
                case SUB:
                    currentFrame.setRegister(a, subtract(bValue, cValue));
                    break;
                    
                case MUL:
                    currentFrame.setRegister(a, multiply(bValue, cValue));
                    break;
                    
                case DIV:
                    currentFrame.setRegister(a, divide(bValue, cValue));
                    break;
                    
                case MOD:
                    currentFrame.setRegister(a, modulo(bValue, cValue));
                    break;
                    
                case POW:
                    currentFrame.setRegister(a, power(bValue, cValue));
                    break;
                    
                case NEG:
                    currentFrame.setRegister(a, negate(bValue));
                    break;
                    
                // Fast arithmetic (numbers only)
                case ADD_FAST:
                    currentFrame.setRegister(a, ((Number) bValue).doubleValue() + ((Number) cValue).doubleValue());
                    break;
                    
                case SUB_FAST:
                    currentFrame.setRegister(a, ((Number) bValue).doubleValue() - ((Number) cValue).doubleValue());
                    break;
                    
                case MUL_FAST:
                    currentFrame.setRegister(a, ((Number) bValue).doubleValue() * ((Number) cValue).doubleValue());
                    break;
                    
                case DIV_FAST:
                    currentFrame.setRegister(a, ((Number) bValue).doubleValue() / ((Number) cValue).doubleValue());
                    break;
                    
                // Comparison operations
                case EQ:
                    currentFrame.setRegister(a, isEqual(bValue, cValue));
                    break;
                    
                case NE:
                    currentFrame.setRegister(a, !isEqual(bValue, cValue));
                    break;
                    
                case LT:
                    currentFrame.setRegister(a, isLess(bValue, cValue));
                    break;
                    
                case LE:
                    currentFrame.setRegister(a, isLessEqual(bValue, cValue));
                    break;
                    
                case GT:
                    currentFrame.setRegister(a, isGreater(bValue, cValue));
                    break;
                    
                case GE:
                    currentFrame.setRegister(a, isGreaterEqual(bValue, cValue));
                    break;
                    
                // Fast comparisons (numbers only)
                case EQ_FAST:
                    currentFrame.setRegister(a, ((Number) bValue).doubleValue() == ((Number) cValue).doubleValue());
                    break;
                    
                case LT_FAST:
                    currentFrame.setRegister(a, ((Number) bValue).doubleValue() < ((Number) cValue).doubleValue());
                    break;
                    
                // Logical operations
                case AND:
                    currentFrame.setRegister(a, isTruthy(bValue) && isTruthy(cValue));
                    break;
                    
                case OR:
                    currentFrame.setRegister(a, isTruthy(bValue) || isTruthy(cValue));
                    break;
                    
                case NOT:
                    currentFrame.setRegister(a, !isTruthy(bValue));
                    break;
                    
                case NULL_COALESCE:
                    currentFrame.setRegister(a, bValue != null ? bValue : cValue);
                    break;
                    
                // Control flow
                case JUMP:
                    int jumpOffset = Instruction.getJumpOffset(instruction);
                    currentFrame.setPc(currentFrame.getPc() + jumpOffset - 1);
                    break;
                    
                case JUMP_IF_FALSE:
                    if (!isTruthy(currentFrame.getRegister(a))) {
                        int offset = OpCode.getB(instruction);
                        // Sign extend from 8 bits to 32 bits
                        if ((offset & 0x80) != 0) {
                            offset = offset | 0xFFFFFF00;
                        }
                        currentFrame.setPc(currentFrame.getPc() + offset - 1);
                    }
                    break;
                    
                case JUMP_IF_TRUE:
                    if (isTruthy(currentFrame.getRegister(a))) {
                        int offset = OpCode.getB(instruction);
                        // Sign extend from 8 bits to 32 bits
                        if ((offset & 0x80) != 0) {
                            offset = offset | 0xFFFFFF00;
                        }
                        currentFrame.setPc(currentFrame.getPc() + offset - 1);
                    }
                    break;
                    
                case CALL:
                    // Function call implementation
                    Object function = currentFrame.getRegister(a);
                    int argCount = OpCode.getB(instruction);
                    Object result = callFunction(function, argCount, currentFrame);
                    // Put result in register 0 (standard calling convention)
                    currentFrame.setRegister(0, result);
                    break;
                    
                case RETURN:
                    Object returnValue = currentFrame.getRegister(a);
                    popFrame();
                    if (frameCount == 0) {
                        return returnValue; // Main function return
                    }
                    // Set return value in caller's register 0
                    getCurrentFrame().setRegister(0, returnValue);
                    break;
                    
                // Built-in operations
                case PRINT:
                    System.out.println(stringify(currentFrame.getRegister(a)));
                    break;
                    
                case CLOCK:
                    currentFrame.setRegister(a, (double) System.currentTimeMillis());
                    break;
                    
                case TYPE_OF:
                    currentFrame.setRegister(a, getTypeName(bValue));
                    break;
                    
                case MAKE_CLOSURE:
                    // Create a function closure from function pool
                    int funcIndex = OpCode.getB(instruction); // Function index directly, not from constants
                    FunctionInfo funcInfo = constantPool.getFunction(funcIndex);
                    currentFrame.setRegister(a, funcInfo);
                    break;
                    
                case NEW_OBJECT:
                    // Create a new object (simple HashMap for properties)
                    currentFrame.setRegister(a, new java.util.HashMap<String, Object>());
                    break;
                    
                case GET_PROPERTY:
                    // A = B.constants[C]
                    Object obj = bValue;
                    String propName = (String) constantPool.getConstant(OpCode.getCValue(instruction));
                    if (obj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
                        currentFrame.setRegister(a, map.get(propName));
                    } else {
                        throw new RuntimeException("Cannot access property on non-object");
                    }
                    break;
                    
                case SET_PROPERTY:
                    // B.constants[A] = C
                    String setPropName = (String) constantPool.getConstant(a);
                    Object setObj = currentFrame.getRegister(OpCode.getB(instruction));
                    Object setValue = cValue;
                    if (setObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> setMap = (java.util.Map<String, Object>) setObj;
                        setMap.put(setPropName, setValue);
                    } else {
                        throw new RuntimeException("Cannot set property on non-object");
                    }
                    break;
                    
                // Array operations
                case ARRAY_LENGTH:
                    // A = B.length
                    Object arrayObj = bValue;
                    if (arrayObj instanceof java.util.List) {
                        currentFrame.setRegister(a, (double) ((java.util.List<?>) arrayObj).size());
                    } else if (arrayObj instanceof String) {
                        currentFrame.setRegister(a, (double) ((String) arrayObj).length());
                    } else {
                        throw new RuntimeException("Cannot get length of non-array/string");
                    }
                    break;
                    
                case GET_INDEX:
                    // A = B[C]
                    Object indexable = bValue;
                    Object index = cValue;
                    if (indexable instanceof java.util.List && index instanceof Double) {
                        java.util.List<?> list = (java.util.List<?>) indexable;
                        int idx = ((Double) index).intValue();
                        if (idx >= 0 && idx < list.size()) {
                            currentFrame.setRegister(a, list.get(idx));
                        } else {
                            throw new RuntimeException("List index out of bounds: " + idx);
                        }
                    } else if (indexable instanceof String && index instanceof Double) {
                        String str = (String) indexable;
                        int idx = ((Double) index).intValue();
                        if (idx >= 0 && idx < str.length()) {
                            currentFrame.setRegister(a, String.valueOf(str.charAt(idx)));
                        } else {
                            throw new RuntimeException("String index out of bounds: " + idx);
                        }
                    } else {
                        throw new RuntimeException("Invalid index operation");
                    }
                    break;
                    
                case SET_INDEX:
                    // B[A] = C
                    Object setIndexable = bValue;
                    Object setIndex = currentFrame.getRegister(a);
                    Object setIndexValue = cValue;
                    if (setIndexable instanceof java.util.List && setIndex instanceof Double) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> list = (java.util.List<Object>) setIndexable;
                        int idx = ((Double) setIndex).intValue();
                        if (idx >= 0 && idx < list.size()) {
                            list.set(idx, setIndexValue);
                        } else {
                            throw new RuntimeException("List index out of bounds: " + idx);
                        }
                    } else {
                        throw new RuntimeException("Cannot set index on non-list");
                    }
                    break;
                    
                case ARRAY_PUSH:
                    // A.push(B)
                    Object arrayToPush = currentFrame.getRegister(a);
                    Object valueToPush = bValue;
                    if (arrayToPush instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> list = (java.util.List<Object>) arrayToPush;
                        list.add(valueToPush);
                    } else {
                        throw new RuntimeException("Cannot push to non-list");
                    }
                    break;
                    
                case NOP:
                    // No operation
                    break;
                    
                case HALT:
                    halted = true;
                    break;
                    
                default:
                    throw new RuntimeException("Unimplemented opcode: " + opcode);
            }
        }
        
        return null;
    }
    
    // Helper methods for operations
    
    private Object add(Object left, Object right) {
        if (left instanceof Double && right instanceof Double) {
            return (Double) left + (Double) right;
        }
        if (left instanceof String || right instanceof String) {
            return stringify(left) + stringify(right);
        }
        throw new RuntimeException("Invalid operands for +");
    }
    
    private Object subtract(Object left, Object right) {
        checkNumberOperands(left, right);
        return ((Number) left).doubleValue() - ((Number) right).doubleValue();
    }
    
    private Object multiply(Object left, Object right) {
        checkNumberOperands(left, right);
        return ((Number) left).doubleValue() * ((Number) right).doubleValue();
    }
    
    private Object divide(Object left, Object right) {
        checkNumberOperands(left, right);
        double rightVal = ((Number) right).doubleValue();
        if (rightVal == 0.0) {
            throw new RuntimeException("Division by zero");
        }
        return ((Number) left).doubleValue() / rightVal;
    }
    
    private Object modulo(Object left, Object right) {
        checkNumberOperands(left, right);
        return ((Number) left).doubleValue() % ((Number) right).doubleValue();
    }
    
    private Object power(Object left, Object right) {
        checkNumberOperands(left, right);
        return Math.pow(((Number) left).doubleValue(), ((Number) right).doubleValue());
    }
    
    private Object negate(Object operand) {
        checkNumberOperand(operand);
        return -((Number) operand).doubleValue();
    }
    
    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null) return false;
        return left.equals(right);
    }
    
    private boolean isLess(Object left, Object right) {
        checkNumberOperands(left, right);
        return ((Number) left).doubleValue() < ((Number) right).doubleValue();
    }
    
    private boolean isLessEqual(Object left, Object right) {
        checkNumberOperands(left, right);
        return ((Number) left).doubleValue() <= ((Number) right).doubleValue();
    }
    
    private boolean isGreater(Object left, Object right) {
        checkNumberOperands(left, right);
        return ((Number) left).doubleValue() > ((Number) right).doubleValue();
    }
    
    private boolean isGreaterEqual(Object left, Object right) {
        checkNumberOperands(left, right);
        return ((Number) left).doubleValue() >= ((Number) right).doubleValue();
    }
    
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (Boolean) object;
        return true;
    }
    
    private void checkNumberOperand(Object operand) {
        if (!(operand instanceof Number)) {
            throw new RuntimeException("Operand must be a number");
        }
    }
    
    private void checkNumberOperands(Object left, Object right) {
        if (!(left instanceof Number) || !(right instanceof Number)) {
            throw new RuntimeException("Operands must be numbers");
        }
    }
    
    private String stringify(Object object) {
        if (object == null) return "nil";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
    
    private String getTypeName(Object object) {
        if (object == null) return "nil";
        if (object instanceof Boolean) return "boolean";
        if (object instanceof Double) return "number";
        if (object instanceof String) return "string";
        return "object";
    }
    
    // Call frame management
    
    private CallFrame getCurrentFrame() {
        if (frameCount <= 0) {
            throw new RuntimeException("No active call frame");
        }
        return callStack[frameCount - 1];
    }
    
    private void pushFrame(FunctionInfo function, int returnAddress) {
        if (frameCount >= MAX_CALL_STACK) {
            throw new RuntimeException("Stack overflow");
        }
        
        callStack[frameCount] = new CallFrame(function, returnAddress, frameCount);
        frameCount++;
    }
    
    private void popFrame() {
        if (frameCount <= 0) {
            throw new RuntimeException("Stack underflow");
        }
        frameCount--;
    }
    
    private Object callFunction(Object function, int argCount, CallFrame callerFrame) {
        if (function instanceof FunctionInfo) {
            FunctionInfo funcInfo = (FunctionInfo) function;
            
            // Collect arguments from caller's registers (starting after function register)
            Object[] args = new Object[argCount];
            for (int i = 0; i < argCount; i++) {
                args[i] = callerFrame.getRegister(i + 1); // Skip function register (register 0)
            }
            
            // Push new frame for the function
            pushFrame(funcInfo, callerFrame.getPc());
            
            // Set up arguments in new frame
            getCurrentFrame().setArguments(args);
            
            // Return null for now - actual return will happen via RETURN instruction
            return null;
        }
        
        // Handle built-in functions
        if ("native_print".equals(function)) {
            if (argCount > 0) {
                System.out.println(stringify(callerFrame.getRegister(0)));
            }
            return null;
        }
        
        if ("native_clock".equals(function)) {
            return (double) System.currentTimeMillis();
        }
        
        throw new RuntimeException("Not a function: " + function);
    }
    
    private void initializeBuiltins() {
        // Add built-in functions and constants
        globals.put("clock", "native_clock");
        globals.put("print", "native_print");
    }
}