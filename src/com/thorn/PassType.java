package com.thorn;

/**
 * Categorizes optimization passes by their purpose and effect.
 */
public enum PassType {
    /**
     * Analysis passes gather information about the program without modifying it.
     * Examples: Control flow analysis, data flow analysis, type inference.
     */
    ANALYSIS,
    
    /**
     * Transformation passes modify the AST to improve performance or reduce size.
     * Examples: Constant folding, function inlining, loop optimization.
     */
    TRANSFORMATION,
    
    /**
     * Cleanup passes remove redundant or dead code after other optimizations.
     * Examples: Dead code elimination, unreachable code elimination.
     */
    CLEANUP
}