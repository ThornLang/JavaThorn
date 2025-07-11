package com.thorn.stdlib;

import com.thorn.Token;

/**
 * Exception thrown by standard library functions
 */
public class StdlibException extends RuntimeException {
    public final Token token;
    
    public StdlibException(String message) {
        super(message);
        this.token = null;
    }
    
    public StdlibException(Token token, String message) {
        super(message);
        this.token = token;
    }
    
    public StdlibException(String message, Throwable cause) {
        super(message, cause);
        this.token = null;
    }
}