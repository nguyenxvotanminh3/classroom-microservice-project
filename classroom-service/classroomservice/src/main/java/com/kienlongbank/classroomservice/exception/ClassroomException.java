package com.kienlongbank.classroomservice.exception;

public class ClassroomException extends RuntimeException {
    
    public ClassroomException(String message) {
        super(message);
    }
    
    public ClassroomException(String message, Throwable cause) {
        super(message, cause);
    }
} 