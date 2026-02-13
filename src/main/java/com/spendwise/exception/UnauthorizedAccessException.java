package com.spendwise.exception;

public class UnauthorizedAccessException extends ApiException {

    public UnauthorizedAccessException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedAccessException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED, message, cause);
    }
}
