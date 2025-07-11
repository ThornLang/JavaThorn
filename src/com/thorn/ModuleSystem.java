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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ModuleSystem {
    private final Map<String, Module> loadedModules = new HashMap<>();
    private final Set<String> loadingModules = new HashSet<>(); // For circular dependency detection
    private final List<String> searchPaths = new ArrayList<>();
    private final Interpreter interpreter;
    private final Map<String, Class<?>> javaStdlibModules = new HashMap<>();
    
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
        
        // Register Java stdlib modules
        registerJavaStdlibModules();
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
            // First check if it's a Java stdlib module
            String moduleName = extractModuleName(modulePath);
            if (javaStdlibModules.containsKey(moduleName)) {
                return loadJavaStdlibModule(moduleName, javaStdlibModules.get(moduleName));
            }
            
            // Find the module file
            Path filePath = resolveModulePath(modulePath);
            if (filePath == null) {
                throw new Thorn.RuntimeError(null, "Cannot find module: " + modulePath);
            }
            
            // Check for conflict with stdlib module
            if (javaStdlibModules.containsKey(moduleName)) {
                throw new Thorn.RuntimeError(null, 
                    "ImportError: There is a file named after a stdlib module, unable to find correct import.");
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
            ModuleEnvironment moduleEnv = new ModuleEnvironment(module, interpreter.globals);
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
        
        public ModuleEnvironment(Module module, Environment globals) {
            super(globals);
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
    
    private void registerJavaStdlibModules() {
        // Register all Java stdlib modules
        javaStdlibModules.put("json", com.thorn.stdlib.Json.class);
        javaStdlibModules.put("system", com.thorn.stdlib.System.class);
        javaStdlibModules.put("random", com.thorn.stdlib.Random.class);
        javaStdlibModules.put("crypto", com.thorn.stdlib.Crypto.class);
        javaStdlibModules.put("net", com.thorn.stdlib.Net.class);
        javaStdlibModules.put("compression", com.thorn.stdlib.Compression.class);
        javaStdlibModules.put("concurrent", com.thorn.stdlib.Concurrent.class);
        // Future modules can be added here:
        // javaStdlibModules.put("io", com.thorn.stdlib.Io.class);
    }
    
    private String extractModuleName(String modulePath) {
        // Remove quotes if present
        if (modulePath.startsWith("\"") && modulePath.endsWith("\"")) {
            modulePath = modulePath.substring(1, modulePath.length() - 1);
        }
        
        // Extract base name without path and extension
        String name = modulePath;
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        if (name.endsWith(".thorn")) {
            name = name.substring(0, name.length() - 6);
        }
        return name;
    }
    
    private Module loadJavaStdlibModule(String moduleName, Class<?> moduleClass) {
        Module module = new Module(moduleName, "stdlib:" + moduleName);
        loadedModules.put(moduleName, module);
        
        // Set the main interpreter for concurrent module support
        ThreadSafeExecutor.setMainInterpreter(interpreter);
        
        try {
            // Load all public static methods as module functions
            for (Method method : moduleClass.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
                    String methodName = convertMethodName(method.getName());
                    JavaFunction function = createJavaFunction(methodName, method);
                    module.addExport(methodName, function);
                }
            }
            
            // Load all public static nested classes
            for (Class<?> nestedClass : moduleClass.getDeclaredClasses()) {
                if (Modifier.isPublic(nestedClass.getModifiers()) && Modifier.isStatic(nestedClass.getModifiers())) {
                    String className = nestedClass.getSimpleName();
                    JavaClass javaClass = createJavaClass(className, nestedClass);
                    module.addExport(className, javaClass);
                }
            }
            
            return module;
        } finally {
            loadingModules.remove(moduleName);
        }
    }
    
    private String convertMethodName(String javaName) {
        // Convert Java camelCase to Thorn snake_case
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < javaName.length(); i++) {
            char c = javaName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }
    
    private JavaFunction createJavaFunction(String name, Method method) {
        // Check if method has varargs
        boolean isVarArgs = method.isVarArgs();
        int arity = isVarArgs ? -1 : method.getParameterCount();
        
        return new JavaFunction(name, arity, (interpreter, arguments) -> {
            try {
                Class<?>[] paramTypes = method.getParameterTypes();
                Object[] args;
                
                if (isVarArgs) {
                    // Handle varargs - last parameter is an array
                    int regularParams = paramTypes.length - 1;
                    args = new Object[paramTypes.length];
                    
                    // Convert regular parameters
                    for (int i = 0; i < regularParams; i++) {
                        if (i < arguments.size()) {
                            args[i] = convertArgument(arguments.get(i), paramTypes[i]);
                        }
                    }
                    
                    // Convert varargs into array
                    Class<?> componentType = paramTypes[regularParams].getComponentType();
                    int varArgCount = Math.max(0, arguments.size() - regularParams);
                    Object varArgs = java.lang.reflect.Array.newInstance(componentType, varArgCount);
                    
                    for (int i = 0; i < varArgCount; i++) {
                        Object converted = convertArgument(arguments.get(regularParams + i), componentType);
                        java.lang.reflect.Array.set(varArgs, i, converted);
                    }
                    args[regularParams] = varArgs;
                } else {
                    // Regular method - convert arguments normally
                    args = new Object[arguments.size()];
                    for (int i = 0; i < arguments.size(); i++) {
                        args[i] = convertArgument(arguments.get(i), paramTypes[i]);
                    }
                }
                
                Object result = method.invoke(null, args);
                // Wrap Java objects that have methods
                if (result != null && shouldWrap(result)) {
                    return new JavaInstance(result);
                }
                return result;
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (cause instanceof com.thorn.StdlibException) {
                    throw new Thorn.RuntimeError(null, cause.getMessage());
                }
                throw new Thorn.RuntimeError(null, "Error calling " + name + ": " + cause.getMessage());
            }
        });
    }
    
    private JavaClass createJavaClass(String name, Class<?> clazz) {
        return new JavaClass(name, (interpreter, arguments) -> {
            try {
                // For now, we only support classes with public constructors
                // This is mainly for things like RandomGenerator, Hash, Process etc.
                if (arguments.isEmpty()) {
                    return clazz.getDeclaredConstructor().newInstance();
                } else {
                    // Try to find a constructor that matches
                    for (var constructor : clazz.getConstructors()) {
                        if (constructor.getParameterCount() == arguments.size()) {
                            Object[] args = new Object[arguments.size()];
                            Class<?>[] paramTypes = constructor.getParameterTypes();
                            for (int i = 0; i < arguments.size(); i++) {
                                args[i] = convertArgument(arguments.get(i), paramTypes[i]);
                            }
                            return constructor.newInstance(args);
                        }
                    }
                }
                throw new Thorn.RuntimeError(null, "No matching constructor for " + name);
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (cause instanceof com.thorn.StdlibException) {
                    throw new Thorn.RuntimeError(null, cause.getMessage());
                }
                throw new Thorn.RuntimeError(null, "Error creating " + name + ": " + cause.getMessage());
            }
        });
    }
    
    private Object convertArgument(Object thornValue, Class<?> targetType) {
        if (thornValue == null) {
            return null;
        }
        
        // Handle primitive types and their wrappers
        if (targetType == String.class) {
            return thornValue.toString();
        } else if (targetType == double.class || targetType == Double.class) {
            if (thornValue instanceof Double) {
                return thornValue;
            } else if (thornValue instanceof Number) {
                return ((Number) thornValue).doubleValue();
            }
        } else if (targetType == int.class || targetType == Integer.class) {
            if (thornValue instanceof Double) {
                return ((Double) thornValue).intValue();
            } else if (thornValue instanceof Number) {
                return ((Number) thornValue).intValue();
            }
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            if (thornValue instanceof Boolean) {
                return thornValue;
            }
        } else if (targetType == List.class) {
            if (thornValue instanceof List) {
                return thornValue;
            }
        } else if (targetType == Map.class) {
            if (thornValue instanceof Map) {
                return thornValue;
            }
        } else if (targetType == Object.class) {
            return thornValue;
        }
        
        // If no conversion found, return as-is and let Java handle it
        return thornValue;
    }
    
    private boolean shouldWrap(Object obj) {
        // Don't wrap primitives, strings, lists, maps, or Thorn objects
        if (obj instanceof String || 
            obj instanceof Number || 
            obj instanceof Boolean ||
            obj instanceof java.util.List ||
            obj instanceof java.util.Map ||
            obj instanceof ThornCallable ||
            obj instanceof ThornInstance ||
            obj instanceof JavaInstance) {
            return false;
        }
        
        // Check if the object has public methods that we should expose
        Class<?> clazz = obj.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                return true; // Has instance methods, should wrap
            }
        }
        
        return false;
    }
}