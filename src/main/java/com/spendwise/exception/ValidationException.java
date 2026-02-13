package com.spendwise.exception;

public class ValidationException extends ApiException {

    public ValidationException(String message) {

        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public ValidationException(String message, Throwable cause) {

        super(ErrorCode.VALIDATION_ERROR, message, cause);
    }
}
