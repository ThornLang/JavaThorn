package com.thorn;

import java.util.List;

/**
 * Public interface for executing Thorn functions from stdlib modules.
 * This allows stdlib modules to execute Thorn functions in a thread-safe manner.
 */
public interface ExecutableFunction {
    /**
     * Get the arity (number of parameters) of this function.
     * @return The number of parameters this function expects
     */
    int arity();
    
    /**
     * Execute this function with the given arguments.
     * @param arguments The arguments to pass to the function
     * @return The result of executing the function
     * @throws RuntimeException if execution fails
     */
    Object execute(List<Object> arguments);
}