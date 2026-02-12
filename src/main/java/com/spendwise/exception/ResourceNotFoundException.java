package com.spendwise.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {

        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) { // This constructor is used when another error caused this one.

        super(message, cause);
    }
}
