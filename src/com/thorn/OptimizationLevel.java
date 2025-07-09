package com.thorn;

/**
 * Optimization levels for the ThornLang compiler.
 * Higher levels enable more aggressive optimizations.
 */
public enum OptimizationLevel {
    /**
     * No optimization. Code runs as written.
     */
    O0(0),
    
    /**
     * Basic optimizations that are always safe and fast.
     * Includes: constant folding, dead code elimination, branch optimization.
     */
    O1(1),
    
    /**
     * Standard optimizations that provide good performance improvements.
     * Includes: O1 + CSE, function inlining, loop optimization.
     */
    O2(2),
    
    /**
     * Aggressive optimizations that may increase compilation time.
     * Includes: O2 + all available optimization passes.
     */
    O3(3);
    
    private final int level;
    
    OptimizationLevel(int level) {
        this.level = level;
    }
    
    /**
     * Gets the numeric level value.
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Checks if this level includes the optimizations of another level.
     */
    public boolean includes(OptimizationLevel other) {
        return this.level >= other.level;
    }
    
    /**
     * Parses an optimization level from a string.
     * Accepts: "0", "1", "2", "3", "O0", "O1", "O2", "O3"
     */
    public static OptimizationLevel fromString(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Optimization level cannot be null");
        }
        
        str = str.trim().toUpperCase();
        
        // Handle numeric format
        if (str.length() == 1 && Character.isDigit(str.charAt(0))) {
            int level = Character.getNumericValue(str.charAt(0));
            switch (level) {
                case 0: return O0;
                case 1: return O1;
                case 2: return O2;
                case 3: return O3;
                default:
                    throw new IllegalArgumentException("Invalid optimization level: " + str);
            }
        }
        
        // Handle O-prefix format
        if (str.startsWith("O") && str.length() == 2) {
            return fromString(str.substring(1));
        }
        
        throw new IllegalArgumentException("Invalid optimization level format: " + str);
    }
    
    @Override
    public String toString() {
        return "O" + level;
    }
}