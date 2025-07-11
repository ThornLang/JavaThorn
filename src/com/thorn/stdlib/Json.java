package com.thorn.stdlib;

import com.thorn.StdlibException;
import java.util.*;
import java.io.*;
import java.nio.file.*;

/**
 * JSON parsing and manipulation module for ThornLang
 * Provides JSON parsing, stringification, validation, and manipulation utilities.
 */
public class Json {
    
    /**
     * Parse a JSON string into Thorn values
     * @param jsonString The JSON string to parse
     * @return Parsed value (Map, List, String, Double, Boolean, or null)
     * @throws RuntimeError if JSON is invalid
     */
    public static Object parse(String jsonString) {
        try {
            return new JsonParser(jsonString).parse();
        } catch (Exception e) {
            throw new StdlibException("JSON parse error: " + e.getMessage());
        }
    }
    
    /**
     * Convert a Thorn value to JSON string
     * @param value The value to convert
     * @return JSON string representation
     */
    public static String stringify(Object value) {
        return stringify(value, false, 0);
    }
    
    /**
     * Convert a Thorn value to pretty-printed JSON string
     * @param value The value to convert
     * @param indent Number of spaces for indentation (default 2)
     * @return Pretty-printed JSON string
     */
    public static String stringifyPretty(Object value, int indent) {
        return stringify(value, true, indent > 0 ? indent : 2);
    }
    
    /**
     * Parse JSON from a file
     * @param path Path to the JSON file
     * @return Parsed value
     * @throws RuntimeError if file cannot be read or JSON is invalid
     */
    public static Object parseFile(String path) {
        try {
            String content = Files.readString(Paths.get(path));
            return parse(content);
        } catch (IOException e) {
            throw new StdlibException("Cannot read file: " + path);
        }
    }
    
    /**
     * Write JSON to a file
     * @param path Path to write to
     * @param value Value to write
     * @param pretty Whether to pretty-print
     */
    public static void writeFile(String path, Object value, boolean pretty) {
        try {
            String json = pretty ? stringifyPretty(value, 2) : stringify(value);
            Files.writeString(Paths.get(path), json);
        } catch (IOException e) {
            throw new StdlibException("Cannot write file: " + path);
        }
    }
    
    /**
     * Check if a string is valid JSON
     * @param jsonString String to validate
     * @return true if valid JSON
     */
    public static boolean isValid(String jsonString) {
        try {
            parse(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get a value from a JSON object using dot notation path
     * @param obj The object (must be a Map)
     * @param path Dot-separated path (e.g., "user.profile.name")
     * @return The value at the path, or null if not found
     */
    public static Object get(Object obj, String path) {
        String[] parts = path.split("\\.");
        Object current = obj;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else if (current instanceof List && part.matches("\\d+")) {
                int index = Integer.parseInt(part);
                List<?> list = (List<?>) current;
                if (index >= 0 && index < list.size()) {
                    current = list.get(index);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    /**
     * Set a value in a JSON object using dot notation path
     * @param obj The object (must be a Map)
     * @param path Dot-separated path
     * @param value The value to set
     */
    @SuppressWarnings("unchecked")
    public static void set(Object obj, String path, Object value) {
        if (!(obj instanceof Map)) {
            throw new StdlibException("Cannot set property on non-object");
        }
        
        String[] parts = path.split("\\.");
        Map<String, Object> current = (Map<String, Object>) obj;
        
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);
            
            if (!(next instanceof Map)) {
                // Create intermediate objects as needed
                Map<String, Object> newMap = new HashMap<>();
                current.put(part, newMap);
                current = newMap;
            } else {
                current = (Map<String, Object>) next;
            }
        }
        
        current.put(parts[parts.length - 1], value);
    }
    
    /**
     * Check if a path exists in a JSON object
     * @param obj The object
     * @param path Dot-separated path
     * @return true if the path exists
     */
    public static boolean has(Object obj, String path) {
        return get(obj, path) != null;
    }
    
    /**
     * Deep merge multiple JSON objects
     * @param objects Objects to merge (later objects override earlier ones)
     * @return Merged object
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> merge(Object... objects) {
        Map<String, Object> result = new HashMap<>();
        
        for (Object obj : objects) {
            if (obj instanceof Map) {
                deepMerge(result, (Map<String, Object>) obj);
            }
        }
        
        return result;
    }
    
    // Helper method for deep merging
    @SuppressWarnings("unchecked")
    private static void deepMerge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map && target.get(key) instanceof Map) {
                // Recursively merge nested objects
                Map<String, Object> nested = new HashMap<>((Map<String, Object>) target.get(key));
                deepMerge(nested, (Map<String, Object>) value);
                target.put(key, nested);
            } else {
                // Simple replacement
                target.put(key, value);
            }
        }
    }
    
    // Internal JSON stringification
    private static String stringify(Object value, boolean pretty, int indent) {
        StringBuilder sb = new StringBuilder();
        stringifyValue(value, sb, pretty, indent, 0);
        return sb.toString();
    }
    
    private static void stringifyValue(Object value, StringBuilder sb, boolean pretty, int indentSize, int currentDepth) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Number) {
            sb.append(value.toString());
        } else if (value instanceof String) {
            sb.append('"');
            escapeString((String) value, sb);
            sb.append('"');
        } else if (value instanceof List) {
            stringifyArray((List<?>) value, sb, pretty, indentSize, currentDepth);
        } else if (value instanceof Map) {
            stringifyObject((Map<?, ?>) value, sb, pretty, indentSize, currentDepth);
        } else {
            // Convert unknown types to string
            sb.append('"');
            escapeString(value.toString(), sb);
            sb.append('"');
        }
    }
    
    private static void stringifyArray(List<?> list, StringBuilder sb, boolean pretty, int indentSize, int currentDepth) {
        sb.append('[');
        if (pretty && !list.isEmpty()) sb.append('\n');
        
        for (int i = 0; i < list.size(); i++) {
            if (pretty) indent(sb, indentSize, currentDepth + 1);
            stringifyValue(list.get(i), sb, pretty, indentSize, currentDepth + 1);
            
            if (i < list.size() - 1) {
                sb.append(',');
            }
            if (pretty) sb.append('\n');
        }
        
        if (pretty && !list.isEmpty()) indent(sb, indentSize, currentDepth);
        sb.append(']');
    }
    
    private static void stringifyObject(Map<?, ?> map, StringBuilder sb, boolean pretty, int indentSize, int currentDepth) {
        sb.append('{');
        if (pretty && !map.isEmpty()) sb.append('\n');
        
        int i = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (pretty) indent(sb, indentSize, currentDepth + 1);
            
            sb.append('"');
            escapeString(entry.getKey().toString(), sb);
            sb.append('"');
            sb.append(':');
            if (pretty) sb.append(' ');
            
            stringifyValue(entry.getValue(), sb, pretty, indentSize, currentDepth + 1);
            
            if (i < map.size() - 1) {
                sb.append(',');
            }
            if (pretty) sb.append('\n');
            i++;
        }
        
        if (pretty && !map.isEmpty()) indent(sb, indentSize, currentDepth);
        sb.append('}');
    }
    
    private static void escapeString(String str, StringBuilder sb) {
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20 || c > 0x7F) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
    }
    
    private static void indent(StringBuilder sb, int indentSize, int depth) {
        for (int i = 0; i < depth * indentSize; i++) {
            sb.append(' ');
        }
    }
    
    /**
     * Simple JSON parser
     */
    private static class JsonParser {
        private final String input;
        private int pos = 0;
        
        JsonParser(String input) {
            this.input = input.trim();
        }
        
        Object parse() {
            Object result = parseValue();
            skipWhitespace();
            if (pos < input.length()) {
                throw new RuntimeException("Unexpected character at position " + pos);
            }
            return result;
        }
        
        private Object parseValue() {
            skipWhitespace();
            if (pos >= input.length()) {
                throw new RuntimeException("Unexpected end of input");
            }
            
            char c = input.charAt(pos);
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't': return parseTrue();
                case 'f': return parseFalse();
                case 'n': return parseNull();
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) {
                        return parseNumber();
                    }
                    throw new RuntimeException("Unexpected character: " + c);
            }
        }
        
        private Map<String, Object> parseObject() {
            Map<String, Object> obj = new HashMap<>();
            pos++; // skip '{'
            skipWhitespace();
            
            if (pos < input.length() && input.charAt(pos) == '}') {
                pos++;
                return obj;
            }
            
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                
                if (pos >= input.length() || input.charAt(pos) != ':') {
                    throw new RuntimeException("Expected ':' after object key");
                }
                pos++; // skip ':'
                
                Object value = parseValue();
                obj.put(key, value);
                
                skipWhitespace();
                if (pos >= input.length()) {
                    throw new RuntimeException("Unexpected end in object");
                }
                
                char c = input.charAt(pos);
                if (c == '}') {
                    pos++;
                    return obj;
                } else if (c == ',') {
                    pos++;
                } else {
                    throw new RuntimeException("Expected ',' or '}' in object");
                }
            }
        }
        
        private List<Object> parseArray() {
            List<Object> arr = new ArrayList<>();
            pos++; // skip '['
            skipWhitespace();
            
            if (pos < input.length() && input.charAt(pos) == ']') {
                pos++;
                return arr;
            }
            
            while (true) {
                arr.add(parseValue());
                skipWhitespace();
                
                if (pos >= input.length()) {
                    throw new RuntimeException("Unexpected end in array");
                }
                
                char c = input.charAt(pos);
                if (c == ']') {
                    pos++;
                    return arr;
                } else if (c == ',') {
                    pos++;
                    skipWhitespace();
                } else {
                    throw new RuntimeException("Expected ',' or ']' in array");
                }
            }
        }
        
        private String parseString() {
            pos++; // skip opening '"'
            StringBuilder sb = new StringBuilder();
            
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '"') {
                    pos++;
                    return sb.toString();
                } else if (c == '\\') {
                    pos++;
                    if (pos >= input.length()) {
                        throw new RuntimeException("Unexpected end in string escape");
                    }
                    char escaped = input.charAt(pos);
                    switch (escaped) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 >= input.length()) {
                                throw new RuntimeException("Invalid unicode escape");
                            }
                            String hex = input.substring(pos + 1, pos + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                            break;
                        default:
                            throw new RuntimeException("Invalid escape sequence: \\" + escaped);
                    }
                } else {
                    sb.append(c);
                }
                pos++;
            }
            
            throw new RuntimeException("Unterminated string");
        }
        
        private Double parseNumber() {
            int start = pos;
            if (input.charAt(pos) == '-') pos++;
            
            // Parse integer part
            if (input.charAt(pos) == '0') {
                pos++;
            } else {
                while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                    pos++;
                }
            }
            
            // Parse decimal part
            if (pos < input.length() && input.charAt(pos) == '.') {
                pos++;
                while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                    pos++;
                }
            }
            
            // Parse exponent
            if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
                pos++;
                if (pos < input.length() && (input.charAt(pos) == '+' || input.charAt(pos) == '-')) {
                    pos++;
                }
                while (pos < input.length() && input.charAt(pos) >= '0' && input.charAt(pos) <= '9') {
                    pos++;
                }
            }
            
            return Double.parseDouble(input.substring(start, pos));
        }
        
        private Boolean parseTrue() {
            if (pos + 4 <= input.length() && input.substring(pos, pos + 4).equals("true")) {
                pos += 4;
                return true;
            }
            throw new RuntimeException("Invalid literal");
        }
        
        private Boolean parseFalse() {
            if (pos + 5 <= input.length() && input.substring(pos, pos + 5).equals("false")) {
                pos += 5;
                return false;
            }
            throw new RuntimeException("Invalid literal");
        }
        
        private Object parseNull() {
            if (pos + 4 <= input.length() && input.substring(pos, pos + 4).equals("null")) {
                pos += 4;
                return null;
            }
            throw new RuntimeException("Invalid literal");
        }
        
        private void skipWhitespace() {
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }
    }
}