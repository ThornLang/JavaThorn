package com.thorn;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe executor for running Thorn functions in separate threads.
 * This class provides the public API for stdlib modules to execute Thorn functions
 * across threads while maintaining proper isolation and error handling.
 */
public class ThreadSafeExecutor {
    
    private static final ConcurrentHashMap<Long, ThreadLocalContext> threadContexts = new ConcurrentHashMap<>();
    private static final ReentrantReadWriteLock globalLock = new ReentrantReadWriteLock();
    
    // Registry to hold the main interpreter instance for stdlib modules
    private static volatile Interpreter mainInterpreter;
    
    /**
     * Set the main interpreter instance (called by ModuleSystem)
     * @param interpreter The main interpreter instance
     */
    public static void setMainInterpreter(Interpreter interpreter) {
        mainInterpreter = interpreter;
    }
    
    /**
     * Get the main interpreter instance for stdlib modules
     * @return The main interpreter instance
     */
    public static Interpreter getMainInterpreter() {
        if (mainInterpreter == null) {
            throw new RuntimeException("Main interpreter not set - this is a bug in the module system");
        }
        return mainInterpreter;
    }
    
    /**
     * Wraps a ThornCallable to make it executable from stdlib modules.
     * @param callable The internal ThornCallable to wrap
     * @param sourceInterpreter The interpreter that created this callable
     * @return An ExecutableFunction that can be called from stdlib modules
     */
    public static ExecutableFunction wrap(ThornCallable callable, Interpreter sourceInterpreter) {
        return new WrappedFunction(callable, sourceInterpreter);
    }
    
    /**
     * Creates a new thread-safe execution context for the current thread.
     * @param parentInterpreter The parent interpreter to copy global state from
     * @return A thread-local interpreter instance
     */
    public static Interpreter createThreadContext(Interpreter parentInterpreter) {
        long threadId = Thread.currentThread().getId();
        
        if (threadContexts.containsKey(threadId)) {
            return threadContexts.get(threadId).interpreter;
        }
        
        // Create a new interpreter instance with copied global state
        Interpreter threadInterpreter = new Interpreter();
        
        // Copy global environment from parent (thread-safe)
        globalLock.readLock().lock();
        try {
            copyGlobalEnvironment(parentInterpreter, threadInterpreter);
        } finally {
            globalLock.readLock().unlock();
        }
        
        ThreadLocalContext context = new ThreadLocalContext(threadInterpreter);
        threadContexts.put(threadId, context);
        
        return threadInterpreter;
    }
    
    /**
     * Cleanup thread-local context when thread is done.
     */
    public static void cleanupThreadContext() {
        long threadId = Thread.currentThread().getId();
        threadContexts.remove(threadId);
    }
    
    /**
     * Copy global environment from parent to child interpreter in a thread-safe way.
     */
    private static void copyGlobalEnvironment(Interpreter source, Interpreter target) {
        // Access the global environment (we'll need to make this accessible)
        Environment sourceGlobals = source.globals;
        Environment targetGlobals = target.globals;
        
        // Copy built-in functions and global variables
        // Note: This requires accessing package-private members, which we'll address
        sourceGlobals.copyTo(targetGlobals);
    }
    
    /**
     * Internal wrapper class that implements ExecutableFunction
     */
    private static class WrappedFunction implements ExecutableFunction {
        private final ThornCallable callable;
        private final Interpreter sourceInterpreter;
        
        public WrappedFunction(ThornCallable callable, Interpreter sourceInterpreter) {
            this.callable = callable;
            this.sourceInterpreter = sourceInterpreter;
        }
        
        @Override
        public int arity() {
            return callable.arity();
        }
        
        @Override
        public Object execute(List<Object> arguments) {
            // Create or get thread-local interpreter
            Interpreter threadInterpreter = createThreadContext(sourceInterpreter);
            
            try {
                // Execute the function in the thread-local context
                return callable.call(threadInterpreter, arguments);
            } catch (Exception e) {
                // Wrap any internal exceptions as RuntimeExceptions for stdlib
                throw new RuntimeException("Function execution failed: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Holds thread-local execution context
     */
    private static class ThreadLocalContext {
        final Interpreter interpreter;
        
        ThreadLocalContext(Interpreter interpreter) {
            this.interpreter = interpreter;
        }
    }
}