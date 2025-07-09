package com.thorn;

import java.util.*;

/**
 * Base class for all optimization passes in the ThornLang compiler.
 * Each pass transforms the AST to improve performance or reduce code size.
 */
public abstract class OptimizationPass {
    
    /**
     * Returns the unique name of this optimization pass.
     */
    public abstract String getName();
    
    /**
     * Returns the type of this optimization pass.
     */
    public abstract PassType getType();
    
    /**
     * Returns the minimum optimization level required for this pass to run.
     * Default is O1.
     */
    public OptimizationLevel getMinimumLevel() {
        return OptimizationLevel.O1;
    }
    
    /**
     * Returns the list of pass names that this pass depends on.
     * The optimization pipeline will ensure dependencies run first.
     */
    public List<String> getDependencies() {
        return Collections.emptyList();
    }
    
    /**
     * Determines if this pass should run given the current context.
     */
    public boolean shouldRun(OptimizationContext context) {
        return context.getLevel().includes(getMinimumLevel()) && 
               !context.isPassDisabled(getName());
    }
    
    /**
     * Performs the optimization transformation on the AST.
     * This method should be implemented by concrete passes.
     */
    public abstract List<Stmt> optimize(List<Stmt> statements, OptimizationContext context);
    
    /**
     * Returns a human-readable description of what this pass does.
     */
    public String getDescription() {
        return "Optimization pass: " + getName();
    }
    
    /**
     * Validates that the transformation maintains program correctness.
     * This is called in debug mode to verify optimization correctness.
     * @param original The original AST before optimization
     * @param optimized The optimized AST after transformation
     * @param context The optimization context
     * @return true if the transformation is valid, false otherwise
     */
    public boolean validateTransformation(List<Stmt> original, List<Stmt> optimized, OptimizationContext context) {
        // Default implementation: assume transformation is valid
        // Subclasses can override with specific validation logic
        return true;
    }
}