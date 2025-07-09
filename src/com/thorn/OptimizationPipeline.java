package com.thorn;

import java.util.*;

/**
 * Manages the execution of optimization passes in the correct order.
 * Handles pass dependencies and ensures each pass runs only once.
 */
public class OptimizationPipeline {
    private final List<OptimizationPass> registeredPasses;
    private final Map<String, OptimizationPass> passByName;
    private final boolean debugMode;
    
    public OptimizationPipeline(boolean debugMode) {
        this.registeredPasses = new ArrayList<>();
        this.passByName = new HashMap<>();
        this.debugMode = debugMode;
    }
    
    /**
     * Registers an optimization pass with the pipeline.
     */
    public void registerPass(OptimizationPass pass) {
        if (passByName.containsKey(pass.getName())) {
            throw new IllegalArgumentException("Pass already registered: " + pass.getName());
        }
        registeredPasses.add(pass);
        passByName.put(pass.getName(), pass);
    }
    
    /**
     * Executes all applicable optimization passes on the given AST.
     */
    public List<Stmt> optimize(List<Stmt> statements, OptimizationContext context) {
        List<OptimizationPass> orderedPasses = orderPasses(context);
        
        if (debugMode) {
            System.out.println("=== Optimization Pipeline ===");
            System.out.println("Optimization level: " + context.getLevel());
            System.out.println("Passes to run: " + orderedPasses.size());
            for (OptimizationPass pass : orderedPasses) {
                System.out.println("  - " + pass.getName() + " (" + pass.getType() + ")");
            }
            System.out.println();
        }
        
        List<Stmt> current = statements;
        
        for (OptimizationPass pass : orderedPasses) {
            if (debugMode) {
                System.out.println("Running pass: " + pass.getName());
            }
            
            List<Stmt> original = current;
            current = pass.optimize(current, context);
            
            // Validate transformation in debug mode
            if (context.shouldValidateTransformations()) {
                if (!pass.validateTransformation(original, current, context)) {
                    throw new RuntimeException("Optimization pass " + pass.getName() + 
                                             " produced invalid transformation");
                }
            }
            
            if (debugMode && current != original) {
                System.out.println("  Pass modified AST");
            }
        }
        
        if (debugMode) {
            System.out.println("\n=== Optimization Complete ===");
        }
        
        return current;
    }
    
    /**
     * Orders passes based on their dependencies and types.
     */
    private List<OptimizationPass> orderPasses(OptimizationContext context) {
        List<OptimizationPass> applicablePasses = new ArrayList<>();
        
        // Filter passes that should run
        for (OptimizationPass pass : registeredPasses) {
            if (pass.shouldRun(context)) {
                applicablePasses.add(pass);
            }
        }
        
        // Topological sort based on dependencies
        return topologicalSort(applicablePasses);
    }
    
    /**
     * Performs topological sort on passes based on their dependencies.
     */
    private List<OptimizationPass> topologicalSort(List<OptimizationPass> passes) {
        Map<String, OptimizationPass> passMap = new HashMap<>();
        Map<String, Set<String>> dependencies = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        
        // Build dependency graph
        for (OptimizationPass pass : passes) {
            String name = pass.getName();
            passMap.put(name, pass);
            dependencies.put(name, new HashSet<>());
            inDegree.put(name, 0);
        }
        
        // Calculate in-degrees
        for (OptimizationPass pass : passes) {
            for (String dep : pass.getDependencies()) {
                if (passMap.containsKey(dep)) {
                    dependencies.get(dep).add(pass.getName());
                    inDegree.put(pass.getName(), inDegree.get(pass.getName()) + 1);
                }
            }
        }
        
        // Kahn's algorithm for topological sort
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        List<OptimizationPass> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sorted.add(passMap.get(current));
            
            for (String dependent : dependencies.get(current)) {
                int newDegree = inDegree.get(dependent) - 1;
                inDegree.put(dependent, newDegree);
                if (newDegree == 0) {
                    queue.offer(dependent);
                }
            }
        }
        
        // Check for cycles
        if (sorted.size() != passes.size()) {
            throw new RuntimeException("Circular dependency detected in optimization passes");
        }
        
        // Group by pass type for better ordering
        List<OptimizationPass> analysis = new ArrayList<>();
        List<OptimizationPass> transformation = new ArrayList<>();
        List<OptimizationPass> cleanup = new ArrayList<>();
        
        for (OptimizationPass pass : sorted) {
            switch (pass.getType()) {
                case ANALYSIS:
                    analysis.add(pass);
                    break;
                case TRANSFORMATION:
                    transformation.add(pass);
                    break;
                case CLEANUP:
                    cleanup.add(pass);
                    break;
            }
        }
        
        // Return passes in order: analysis, transformation, cleanup
        List<OptimizationPass> result = new ArrayList<>();
        result.addAll(analysis);
        result.addAll(transformation);
        result.addAll(cleanup);
        
        return result;
    }
}