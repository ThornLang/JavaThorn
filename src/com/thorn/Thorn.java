package com.thorn;

import com.thorn.vm.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Thorn {
    private static final Interpreter interpreter = new Interpreter();
    private static final ThornVM vm = new ThornVM();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    static boolean printAst = false;
    static boolean useVM = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 3) {
            System.out.println("Usage: thorn [--ast] [--vm] [script]");
            System.out.println("       Use -Doptimize.thorn.ast=true to enable dead code elimination");
            System.exit(64);
        } 
        
        int fileArgIndex = 0;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--ast")) {
                printAst = true;
                fileArgIndex = i + 1;
            } else if (args[i].equals("--vm")) {
                useVM = true;
                fileArgIndex = i + 1;
            } else {
                break;
            }
        }
        
        if (args.length > fileArgIndex) {
            runFile(args[fileArgIndex]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        System.out.println("Thorn Programming Language v0.1" + (useVM ? " (VM Mode)" : " (Tree-walk Mode)"));
        System.out.println("Type 'exit' to quit");
        
        for (;;) {
            System.out.print("thorn> ");
            String line = reader.readLine();
            if (line == null || line.equals("exit")) break;
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error
        if (hadError) return;

        // Apply optimizations if enabled
        OptimizationLevel optLevel = getOptimizationLevel();
        boolean optimizeAst = optLevel != OptimizationLevel.O0;
        
        if (optimizeAst) {
            statements = applyOptimizations(statements, optLevel);
        }

        // Print AST if requested
        if (printAst) {
            AstPrinter printer = new AstPrinter();
            String astHeader = optimizeAst ? "=== Abstract Syntax Tree (Optimized) ===" : "=== Abstract Syntax Tree ===";
            System.out.println(astHeader);
            System.out.println(printer.print(statements));
            System.out.println("=== End AST ===\n");
        }

        if (useVM) {
            // Use VM mode - only basic expressions for now
            // System.err.println("DEBUG: Using VM mode, compiling " + statements.size() + " statements");
            try {
                SimpleCompiler compiler = new SimpleCompiler();
                CompilationResult result = compiler.compile(statements);
                
                if (printAst) {
                    System.out.println("=== Bytecode Disassembly ===");
                    result.disassemble();
                    System.out.println("=== End Disassembly ===\n");
                }
                
                vm.execute(result);
            } catch (Exception e) {
                System.err.println("VM execution failed: " + e.getMessage());
                if (printAst) {
                    e.printStackTrace();
                }
                hadRuntimeError = true;
            }
        } else {
            // Use tree-walking interpreter
            interpreter.interpret(statements);
        }
    }

    /**
     * Get the optimization level from system properties.
     * Supports both new and legacy configuration formats.
     */
    private static OptimizationLevel getOptimizationLevel() {
        // Check for new optimization level property
        String levelProp = System.getProperty("optimize.thorn.level");
        if (levelProp != null) {
            try {
                return OptimizationLevel.fromString(levelProp);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid optimization level: " + levelProp + ", using O0");
                return OptimizationLevel.O0;
            }
        }
        
        // Check for legacy property for backward compatibility
        boolean legacyOptimize = Boolean.getBoolean("optimize.thorn.ast");
        if (legacyOptimize) {
            return OptimizationLevel.O1; // Default to O1 for backward compatibility
        }
        
        return OptimizationLevel.O0;
    }
    
    /**
     * Apply optimizations to the AST using the optimization pipeline.
     */
    private static List<Stmt> applyOptimizations(List<Stmt> statements, OptimizationLevel level) {
        // Create and configure the optimization pipeline
        OptimizationPipeline pipeline = new OptimizationPipeline(printAst);
        
        // Register available optimization passes
        pipeline.registerPass(new DeadCodeEliminationPass());
        pipeline.registerPass(new ConstantFoldingPass());
        pipeline.registerPass(new BranchOptimizationPass());
        pipeline.registerPass(new CopyPropagationPass());
        pipeline.registerPass(new DeadStoreEliminationPass());
        pipeline.registerPass(new ControlFlowAnalysisPass());
        pipeline.registerPass(new UnreachableCodeEliminationPass());
        pipeline.registerPass(new CommonSubexpressionEliminationPass());
        pipeline.registerPass(new LoopOptimizationPass());
        pipeline.registerPass(new FunctionInliningPass());
        pipeline.registerPass(new TailCallOptimizationPass());
        
        // Create optimization context
        OptimizationContext context = new OptimizationContext(level, printAst, printAst);
        
        // Configure passes based on system properties
        configureOptimizationPasses(context);
        
        // Apply optimizations
        return pipeline.optimize(statements, context);
    }
    
    /**
     * Configure optimization passes based on system properties.
     */
    private static void configureOptimizationPasses(OptimizationContext context) {
        // Check for explicitly enabled/disabled passes
        String enabledPasses = System.getProperty("optimize.thorn.passes.enable");
        if (enabledPasses != null) {
            for (String passName : enabledPasses.split(",")) {
                context.enablePass(passName.trim());
            }
        }
        
        String disabledPasses = System.getProperty("optimize.thorn.passes.disable");
        if (disabledPasses != null) {
            for (String passName : disabledPasses.split(",")) {
                context.disablePass(passName.trim());
            }
        }
        
        // Configure individual pass parameters
        // Example: -Doptimize.thorn.inline.threshold=10
        String inlineThreshold = System.getProperty("optimize.thorn.inline.threshold");
        if (inlineThreshold != null) {
            context.setPassConfiguration("function-inlining", "threshold", inlineThreshold);
        }
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    private static void report(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    static class RuntimeError extends RuntimeException {
        final Token token;

        RuntimeError(Token token, String message) {
            super(message);
            this.token = token;
        }
    }
}