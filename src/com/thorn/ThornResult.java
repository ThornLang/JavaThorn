package com.thorn;

import java.util.Objects;

/**
 * Represents the Result type in ThornLang for explicit error handling.
 * Result[T, E] = Ok(value: T) | Error(error: E)
 */
public abstract class ThornResult {
    
    /**
     * Returns true if this is an Ok result
     */
    public abstract boolean isOk();
    
    /**
     * Returns true if this is an Error result
     */
    public abstract boolean isError();
    
    /**
     * Returns the value if Ok, throws if Error
     */
    public abstract Object unwrap();
    
    /**
     * Returns the value if Ok, returns default if Error
     */
    public abstract Object unwrapOr(Object defaultValue);
    
    /**
     * Returns the error if Error, throws if Ok
     */
    public abstract Object unwrapError();
    
    /**
     * Returns the value if Ok, null if Error
     */
    public abstract Object getValue();
    
    /**
     * Returns the error if Error, null if Ok
     */
    public abstract Object getError();
    
    /**
     * Creates an Ok result with the given value
     */
    public static ThornResult ok(Object value) {
        return new Ok(value);
    }
    
    /**
     * Creates an Error result with the given error
     */
    public static ThornResult error(Object error) {
        return new Error(error);
    }
    
    /**
     * Ok variant of Result
     */
    public static class Ok extends ThornResult {
        private final Object value;
        
        public Ok(Object value) {
            this.value = value;
        }
        
        @Override
        public boolean isOk() {
            return true;
        }
        
        @Override
        public boolean isError() {
            return false;
        }
        
        @Override
        public Object unwrap() {
            return value;
        }
        
        @Override
        public Object unwrapOr(Object defaultValue) {
            return value;
        }
        
        @Override
        public Object unwrapError() {
            throw new RuntimeException("Called unwrapError on Ok result");
        }
        
        @Override
        public Object getValue() {
            return value;
        }
        
        @Override
        public Object getError() {
            return null;
        }
        
        @Override
        public String toString() {
            return "Ok(" + value + ")";
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Ok ok = (Ok) obj;
            return Objects.equals(value, ok.value);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
    
    /**
     * Error variant of Result
     */
    public static class Error extends ThornResult {
        private final Object error;
        
        public Error(Object error) {
            this.error = error;
        }
        
        @Override
        public boolean isOk() {
            return false;
        }
        
        @Override
        public boolean isError() {
            return true;
        }
        
        @Override
        public Object unwrap() {
            throw new RuntimeException("Called unwrap on Error result: " + error);
        }
        
        @Override
        public Object unwrapOr(Object defaultValue) {
            return defaultValue;
        }
        
        @Override
        public Object unwrapError() {
            return error;
        }
        
        @Override
        public Object getValue() {
            return null;
        }
        
        @Override
        public Object getError() {
            return error;
        }
        
        @Override
        public String toString() {
            return "Error(" + error + ")";
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Error error1 = (Error) obj;
            return Objects.equals(error, error1.error);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(error);
        }
    }
}