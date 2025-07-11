package com.thorn.stdlib;

import com.thorn.StdlibException;
import com.thorn.ThornCallable;
import com.thorn.ExecutableFunction;
import com.thorn.ThreadSafeExecutor;
import com.thorn.Interpreter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Concurrent programming module for ThornLang
 * Provides threading, async execution, and scheduling capabilities.
 */
public class Concurrent {
    
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    
    /**
     * Start a new thread to execute the given function
     * @param function The function to execute in a new thread
     * @return A thread handle object
     */
    public static Object startThread(Object function) {
        if (!(function instanceof ThornCallable)) {
            throw new StdlibException("start_thread expects a function argument");
        }
        
        ExecutableFunction executableFunc = ThreadSafeExecutor.wrap((ThornCallable) function, getCurrentInterpreter());
        
        Future<?> future = threadPool.submit(() -> {
            try {
                executableFunc.execute(new ArrayList<>());
            } catch (Exception e) {
                java.lang.System.err.println("Thread execution failed: " + e.getMessage());
            } finally {
                ThreadSafeExecutor.cleanupThreadContext();
            }
        });
        
        return new ThreadHandle(future);
    }
    
    /**
     * Execute a function asynchronously and return a future
     * @param function The function to execute
     * @return A future object that can be used to get the result
     */
    public static Object runAsync(Object function) {
        if (!(function instanceof ThornCallable)) {
            throw new StdlibException("run_async expects a function argument");
        }
        
        ExecutableFunction executableFunc = ThreadSafeExecutor.wrap((ThornCallable) function, getCurrentInterpreter());
        
        Future<Object> future = threadPool.submit(() -> {
            try {
                return executableFunc.execute(new ArrayList<>());
            } finally {
                ThreadSafeExecutor.cleanupThreadContext();
            }
        });
        
        return new AsyncResult(future);
    }
    
    /**
     * Schedule a function to run after a delay
     * @param function The function to execute
     * @param delayMs Delay in milliseconds
     * @return A scheduled task handle
     */
    public static Object scheduleAfter(Object function, double delayMs) {
        if (!(function instanceof ThornCallable)) {
            throw new StdlibException("schedule_after expects a function argument");
        }
        
        ExecutableFunction executableFunc = ThreadSafeExecutor.wrap((ThornCallable) function, getCurrentInterpreter());
        
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                executableFunc.execute(new ArrayList<>());
            } catch (Exception e) {
                java.lang.System.err.println("Scheduled task failed: " + e.getMessage());
            } finally {
                ThreadSafeExecutor.cleanupThreadContext();
            }
        }, (long) delayMs, TimeUnit.MILLISECONDS);
        
        return new ScheduledTaskHandle(future);
    }
    
    /**
     * Schedule a function to run repeatedly with a fixed period
     * @param function The function to execute
     * @param delayMs Initial delay in milliseconds
     * @param periodMs Period between executions in milliseconds
     * @return A scheduled task handle
     */
    public static Object scheduleRepeat(Object function, double delayMs, double periodMs) {
        if (!(function instanceof ThornCallable)) {
            throw new StdlibException("schedule_repeat expects a function argument");
        }
        
        ExecutableFunction executableFunc = ThreadSafeExecutor.wrap((ThornCallable) function, getCurrentInterpreter());
        
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                executableFunc.execute(new ArrayList<>());
            } catch (Exception e) {
                java.lang.System.err.println("Repeated task failed: " + e.getMessage());
            } finally {
                ThreadSafeExecutor.cleanupThreadContext();
            }
        }, (long) delayMs, (long) periodMs, TimeUnit.MILLISECONDS);
        
        return new ScheduledTaskHandle(future);
    }
    
    /**
     * Sleep the current thread for the specified number of milliseconds
     * @param ms Number of milliseconds to sleep
     */
    public static void sleep(double ms) {
        try {
            Thread.sleep((long) ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StdlibException("Sleep was interrupted: " + e.getMessage());
        }
    }
    
    /**
     * Get the current thread ID
     * @return The current thread's ID as a number
     */
    public static double currentThreadId() {
        return Thread.currentThread().getId();
    }
    
    /**
     * Get the number of available processor cores
     * @return Number of available cores
     */
    public static double availableCores() {
        return Runtime.getRuntime().availableProcessors();
    }
    
    /**
     * Create a thread-safe counter
     * @param initialValue The initial value of the counter
     * @return A counter object
     */
    public static Object createCounter(double initialValue) {
        return new AtomicCounter((long) initialValue);
    }
    

    /**
     * Shutdown all thread pools (for cleanup)
     */
    public static void shutdown() {
        scheduler.shutdown();
        threadPool.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Helper method to get current interpreter
    private static Interpreter getCurrentInterpreter() {
        return ThreadSafeExecutor.getMainInterpreter();
    }
    
    /**
     * Thread handle for managing threads
     */
    public static class ThreadHandle {
        private final Future<?> future;
        
        public ThreadHandle(Future<?> future) {
            this.future = future;
        }
        
        public boolean isDone() {
            return future.isDone();
        }
        
        public void cancel() {
            future.cancel(true);
        }
        
        public void join() {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StdlibException("Thread join was interrupted");
            } catch (ExecutionException e) {
                throw new StdlibException("Thread execution failed: " + e.getCause().getMessage());
            }
        }
    }
    
    /**
     * Async result handle for getting results from async functions
     */
    public static class AsyncResult {
        private final Future<Object> future;
        
        public AsyncResult(Future<Object> future) {
            this.future = future;
        }
        
        public boolean isDone() {
            return future.isDone();
        }
        
        public Object get() {
            try {
                return future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StdlibException("Async result get was interrupted");
            } catch (ExecutionException e) {
                throw new StdlibException("Async execution failed: " + e.getCause().getMessage());
            }
        }
        
        public Object getWithTimeout(double timeoutMs) {
            try {
                return future.get((long) timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StdlibException("Async result get was interrupted");
            } catch (ExecutionException e) {
                throw new StdlibException("Async execution failed: " + e.getCause().getMessage());
            } catch (TimeoutException e) {
                throw new StdlibException("Async result timed out after " + timeoutMs + "ms");
            }
        }
        
        public void cancel() {
            future.cancel(true);
        }
    }
    
    /**
     * Scheduled task handle for managing scheduled tasks
     */
    public static class ScheduledTaskHandle {
        private final ScheduledFuture<?> future;
        
        public ScheduledTaskHandle(ScheduledFuture<?> future) {
            this.future = future;
        }
        
        public boolean isDone() {
            return future.isDone();
        }
        
        public void cancel() {
            future.cancel(true);
        }
        
        public double getDelay() {
            return future.getDelay(TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Thread-safe atomic counter
     */
    public static class AtomicCounter {
        private final java.util.concurrent.atomic.AtomicLong value;
        
        public AtomicCounter(long initialValue) {
            this.value = new java.util.concurrent.atomic.AtomicLong(initialValue);
        }
        
        public double increment() {
            return value.incrementAndGet();
        }
        
        public double decrement() {
            return value.decrementAndGet();
        }
        
        public double addAndGet(double delta) {
            return value.addAndGet((long) delta);
        }
        
        public double getValue() {
            return value.get();
        }
        
        public void setValue(double newValue) {
            value.set((long) newValue);
        }
        
        public double compareAndSwap(double expected, double newValue) {
            boolean success = value.compareAndSet((long) expected, (long) newValue);
            return success ? newValue : value.get();
        }
    }
}