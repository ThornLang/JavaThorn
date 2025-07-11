package com.thorn.stdlib;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.lang.management.*;

/**
 * System information and operations module for ThornLang
 * Provides access to system properties, environment variables, and process management.
 */
public class System {
    
    /**
     * Get platform name
     * @return Platform name ("windows", "linux", "macos", etc.)
     */
    public static String platform() {
        String os = java.lang.System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "macos";
        if (os.contains("linux")) return "linux";
        if (os.contains("sunos")) return "solaris";
        return os;
    }
    
    /**
     * Get CPU architecture
     * @return Architecture string (e.g., "x86_64", "aarch64")
     */
    public static String architecture() {
        return java.lang.System.getProperty("os.arch");
    }
    
    /**
     * Get system hostname
     * @return Hostname string
     */
    public static String hostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }
    
    /**
     * Get current username
     * @return Username string
     */
    public static String username() {
        return java.lang.System.getProperty("user.name");
    }
    
    /**
     * Get user home directory
     * @return Home directory path
     */
    public static String homeDir() {
        return java.lang.System.getProperty("user.home");
    }
    
    /**
     * Get system temp directory
     * @return Temp directory path
     */
    public static String tempDir() {
        return java.lang.System.getProperty("java.io.tmpdir");
    }
    
    /**
     * Get number of logical CPUs
     * @return CPU count
     */
    public static double cpuCount() {
        return Runtime.getRuntime().availableProcessors();
    }
    
    /**
     * Get total system memory in bytes
     * @return Total memory
     */
    public static double totalMemory() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        return memoryBean.getHeapMemoryUsage().getMax();
    }
    
    /**
     * Get available system memory in bytes
     * @return Available memory
     */
    public static double availableMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory() + (runtime.maxMemory() - runtime.totalMemory());
    }
    
    /**
     * Get environment variable
     * @param name Variable name
     * @return Variable value or null if not found
     */
    public static String getenv(String name) {
        return java.lang.System.getenv(name);
    }
    
    /**
     * Get all environment variables
     * @return Map of environment variables
     */
    public static Map<String, Object> environ() {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, String> entry : java.lang.System.getenv().entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    
    /**
     * Get current process ID
     * @return Process ID
     */
    public static double pid() {
        return ProcessHandle.current().pid();
    }
    
    /**
     * Get parent process ID
     * @return Parent process ID or -1 if not available
     */
    public static double ppid() {
        return ProcessHandle.current().parent()
            .map(p -> (double) p.pid())
            .orElse(-1.0);
    }
    
    /**
     * Exit process with code
     * @param code Exit code
     */
    public static void exit(double code) {
        java.lang.System.exit((int) code);
    }
    
    /**
     * Get current working directory
     * @return Current directory path
     */
    public static String cwd() {
        return java.lang.System.getProperty("user.dir");
    }
    
    /**
     * Change working directory
     * @param path New directory path
     * @throws StdlibException if directory change fails
     */
    public static void chdir(String path) {
        Path newPath = Paths.get(path).toAbsolutePath();
        if (!Files.isDirectory(newPath)) {
            throw new StdlibException("Not a directory: " + path);
        }
        java.lang.System.setProperty("user.dir", newPath.toString());
    }
    
    /**
     * Get canonical absolute path
     * @param path Path to resolve
     * @return Absolute path
     * @throws StdlibException if path cannot be resolved
     */
    public static String realpath(String path) {
        try {
            return Paths.get(path).toRealPath().toString();
        } catch (IOException e) {
            throw new StdlibException("Cannot resolve path: " + path, e);
        }
    }
    
    /**
     * Join path components
     * @param parts Path parts to join
     * @return Joined path
     */
    public static String joinPath(List<Object> parts) {
        if (parts.isEmpty()) return "";
        
        Path result = Paths.get(parts.get(0).toString());
        for (int i = 1; i < parts.size(); i++) {
            result = result.resolve(parts.get(i).toString());
        }
        return result.toString();
    }
    
    /**
     * Get current time in seconds since epoch
     * @return Time in seconds
     */
    public static double time() {
        return java.lang.System.currentTimeMillis() / 1000.0;
    }
    
    /**
     * Get current time in milliseconds
     * @return Time in milliseconds
     */
    public static double timeMillis() {
        return java.lang.System.currentTimeMillis();
    }
    
    /**
     * Get current time in nanoseconds
     * @return Time in nanoseconds
     */
    public static double timeNanos() {
        return java.lang.System.nanoTime();
    }
    
    /**
     * Sleep for specified duration
     * @param seconds Duration in seconds
     * @throws StdlibException if sleep is interrupted
     */
    public static void sleep(double seconds) {
        try {
            Thread.sleep((long)(seconds * 1000));
        } catch (InterruptedException e) {
            throw new StdlibException("Sleep interrupted", e);
        }
    }
    
    /**
     * Execute a command and return a Process handle
     * @param command Command to execute
     * @param args Command arguments
     * @return Process object
     * @throws StdlibException if execution fails
     */
    public static Process exec(String command, List<Object> args) {
        List<String> cmdList = new ArrayList<>();
        cmdList.add(command);
        for (Object arg : args) {
            cmdList.add(arg.toString());
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(cmdList);
            java.lang.Process proc = pb.start();
            return new Process(proc);
        } catch (IOException e) {
            throw new StdlibException("Failed to execute command: " + command, e);
        }
    }
    
    /**
     * Process wrapper class
     */
    public static class Process {
        private final java.lang.Process process;
        private final long pid;
        
        public Process(java.lang.Process process) {
            this.process = process;
            this.pid = process.pid();
        }
        
        public double getPid() {
            return pid;
        }
        
        public double waitFor() {
            try {
                return process.waitFor();
            } catch (InterruptedException e) {
                throw new StdlibException("Process wait interrupted", e);
            }
        }
        
        public Double tryWait() {
            try {
                if (process.waitFor(0, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    return (double) process.exitValue();
                }
                return null;
            } catch (InterruptedException e) {
                return null;
            }
        }
        
        public boolean isAlive() {
            return process.isAlive();
        }
        
        public void kill() {
            process.destroy();
        }
        
        public void killForcibly() {
            process.destroyForcibly();
        }
        
        public InputStream getStdout() {
            return process.getInputStream();
        }
        
        public InputStream getStderr() {
            return process.getErrorStream();
        }
        
        public OutputStream getStdin() {
            return process.getOutputStream();
        }
    }
    
    /**
     * Expand environment variables in a string
     * @param text Text with potential $VAR or ${VAR} references
     * @return Expanded text
     */
    public static String expandEnv(String text) {
        String result = text;
        
        // Replace ${VAR} style
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(result);
        
        while (matcher.find()) {
            String var = matcher.group(1);
            String value = java.lang.System.getenv(var);
            if (value != null) {
                result = result.replace(matcher.group(0), value);
            }
        }
        
        // Replace $VAR style
        pattern = java.util.regex.Pattern.compile("\\$([A-Za-z_][A-Za-z0-9_]*)");
        matcher = pattern.matcher(result);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String var = matcher.group(1);
            String value = java.lang.System.getenv(var);
            matcher.appendReplacement(sb, value != null ? value : matcher.group(0));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
}