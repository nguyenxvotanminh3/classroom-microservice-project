package com.kienlongbank.nguyenminh.user.exception;

/**
 * Exception thrown when the createUser fallback method is executed.
 */
public class CreateUserFallbackException extends RuntimeException {

    public CreateUserFallbackException(String message, Throwable cause) {
        super(message, cause);
    }

    public CreateUserFallbackException(String message) {
        super(message);
    }
} 