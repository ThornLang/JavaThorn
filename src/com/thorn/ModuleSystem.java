package com.thorn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModuleSystem {
    private final Map<String, Module> loadedModules = new HashMap<>();
    private final Set<String> loadingModules = new HashSet<>(); // For circular dependency detection
    private final List<String> searchPaths = new ArrayList<>();
    private final Interpreter interpreter;
    
    public ModuleSystem(Interpreter interpreter) {
        this.interpreter = interpreter;
        // Add default search paths
        searchPaths.add("."); // Current directory
        searchPaths.add("./stdlib"); // Standard library
        
        // Add THORN_PATH environment variable paths if set
        String thornPath = System.getenv("THORN_PATH");
        if (thornPath != null) {
            for (String path : thornPath.split(":")) {
                searchPaths.add(path.trim());
            }
        }
    }
    
    public static class Module {
        private final String name;
        private final String path;
        private final Environment exports;
        private final Set<String> exportedNames;
        
        public Module(String name, String path) {
            this.name = name;
            this.path = path;
            this.exports = new Environment();
            this.exportedNames = new HashSet<>();
        }
        
        public void addExport(String name, Object value) {
            exports.define(name, value, false);
            exportedNames.add(name);
        }
        
        public Object getExport(String name) {
            return getExport(name, null);
        }
        
        public Object getExport(String name, Token token) {
            if (!exportedNames.contains(name)) {
                throw new Thorn.RuntimeError(token, "ImportError: Unable to find '" + name + 
                    "' in module '" + this.name + "'");
            }
            return exports.get(new Token(TokenType.IDENTIFIER, name, null, 0));
        }
        
        public Set<String> getExportedNames() {
            return new HashSet<>(exportedNames);
        }
        
        public Environment getExports() {
            return exports;
        }
    }
    
    public Module loadModule(String modulePath) {
        // Check if already loaded
        if (loadedModules.containsKey(modulePath)) {
            return loadedModules.get(modulePath);
        }
        
        // Check for circular dependencies
        if (loadingModules.contains(modulePath)) {
            throw new Thorn.RuntimeError(null, "ImportError: Circular dependency detected for module '" + modulePath + "'");
        }
        
        loadingModules.add(modulePath);
        
        try {
            // Find the module file
            Path filePath = resolveModulePath(modulePath);
            if (filePath == null) {
                throw new Thorn.RuntimeError(null, "Cannot find module: " + modulePath);
            }
            
            // Read and parse the module
            String source = Files.readString(filePath);
            Scanner scanner = new Scanner(source);
            List<Token> tokens = scanner.scanTokens();
            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parse();
            
            // Note: Parser throws exceptions on errors, so if we get here, parsing succeeded
            
            // Create a new module
            Module module = new Module(modulePath, filePath.toString());
            loadedModules.put(modulePath, module);
            
            // Execute the module in its own environment with export tracking
            ModuleEnvironment moduleEnv = new ModuleEnvironment(module);
            interpreter.executeModule(statements, moduleEnv);
            
            return module;
            
        } catch (IOException e) {
            throw new Thorn.RuntimeError(null, "Error reading module '" + modulePath + "': " + e.getMessage());
        } finally {
            loadingModules.remove(modulePath);
        }
    }
    
    private Path resolveModulePath(String modulePath) {
        // Remove quotes if present
        if (modulePath.startsWith("\"") && modulePath.endsWith("\"")) {
            modulePath = modulePath.substring(1, modulePath.length() - 1);
        }
        
        // Add .thorn extension if not present
        if (!modulePath.endsWith(".thorn")) {
            modulePath = modulePath + ".thorn";
        }
        
        // Try each search path
        for (String searchPath : searchPaths) {
            Path path = Paths.get(searchPath, modulePath);
            if (Files.exists(path) && Files.isRegularFile(path)) {
                return path;
            }
        }
        
        // Try as absolute path
        Path path = Paths.get(modulePath);
        if (Files.exists(path) && Files.isRegularFile(path)) {
            return path;
        }
        
        return null;
    }
    
    public void addSearchPath(String path) {
        searchPaths.add(path);
    }
    
    // Special environment that tracks exports
    public static class ModuleEnvironment extends Environment {
        private final Module module;
        
        public ModuleEnvironment(Module module) {
            this.module = module;
        }
        
        public void export(String name, Object value) {
            define(name, value, false);
            module.addExport(name, value);
        }
        
        public Module getModule() {
            return module;
        }
    }
}