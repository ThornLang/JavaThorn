package com.thorn;

import java.util.*;

/**
 * Context information shared between optimization passes.
 * Allows passes to share analysis results and configuration.
 */
public class OptimizationContext {
    private final OptimizationLevel level;
    private final boolean debugMode;
    private final boolean validateTransformations;
    private final Map<String, Object> analysisCache;
    private final Set<String> disabledPasses;
    private final Map<String, Map<String, String>> passConfigurations;
    
    public OptimizationContext(OptimizationLevel level, boolean debugMode, boolean validateTransformations) {
        this.level = level;
        this.debugMode = debugMode;
        this.validateTransformations = validateTransformations;
        this.analysisCache = new HashMap<>();
        this.disabledPasses = new HashSet<>();
        this.passConfigurations = new HashMap<>();
    }
    
    /**
     * Gets the current optimization level.
     */
    public OptimizationLevel getLevel() {
        return level;
    }
    
    /**
     * Checks if debug mode is enabled.
     */
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Checks if transformation validation is enabled.
     */
    public boolean shouldValidateTransformations() {
        return validateTransformations;
    }
    
    /**
     * Stores analysis results for use by other passes.
     */
    public void cacheAnalysis(String key, Object value) {
        analysisCache.put(key, value);
    }
    
    /**
     * Retrieves cached analysis results.
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedAnalysis(String key, Class<T> type) {
        Object value = analysisCache.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Checks if an analysis result is cached.
     */
    public boolean hasAnalysis(String key) {
        return analysisCache.containsKey(key);
    }
    
    /**
     * Enables a specific optimization pass.
     */
    public void enablePass(String passName) {
        disabledPasses.remove(passName);
    }
    
    /**
     * Disables a specific optimization pass.
     */
    public void disablePass(String passName) {
        disabledPasses.add(passName);
    }
    
    /**
     * Checks if a pass is disabled.
     */
    public boolean isPassDisabled(String passName) {
        return disabledPasses.contains(passName);
    }
    
    /**
     * Sets configuration for a specific pass.
     */
    public void setPassConfiguration(String passName, String key, String value) {
        passConfigurations.computeIfAbsent(passName, k -> new HashMap<>()).put(key, value);
    }
    
    /**
     * Gets configuration for a specific pass.
     */
    public String getPassConfiguration(String passName, String key) {
        Map<String, String> config = passConfigurations.get(passName);
        return config != null ? config.get(key) : null;
    }
    
    /**
     * Gets configuration for a specific pass with a default value.
     */
    public String getPassConfiguration(String passName, String key, String defaultValue) {
        String value = getPassConfiguration(passName, key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Gets an integer configuration value.
     */
    public int getPassConfigurationInt(String passName, String key, int defaultValue) {
        String value = getPassConfiguration(passName, key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return defaultValue;
    }
}