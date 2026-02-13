package com.spendwise.exception;

public class ValidationException extends ApiException {

    public ValidationException(String message) {

        super(ErrorCode.VALIDATION_FAILED, message);
    }

    public ValidationException(String message, Throwable cause) {

        super(ErrorCode.VALIDATION_FAILED, message, cause);
    }
}
