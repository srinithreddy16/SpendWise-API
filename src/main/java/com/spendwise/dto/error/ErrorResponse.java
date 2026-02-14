package com.spendwise.dto.error;

import com.spendwise.exception.ErrorCode;

import java.time.Instant;

/**
 * Standard error response DTO for API error responses.
 * Contains error code, user-friendly message, timestamp, and request path.
 * Never exposes stack traces or internal implementation details.
 */
public record ErrorResponse(String errorCode, String message, String timestamp, String path) {

    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, Instant.now().toString(), null);
    }

    public static ErrorResponse of(ErrorCode code) {
        return of(code.name(), code.getClientMessage(), null);
    }

    public static ErrorResponse of(ErrorCode code, String path) {
        return of(code.name(), code.getClientMessage(), path);
    }

    public static ErrorResponse of(String errorCode, String message, String path) {
        return new ErrorResponse(errorCode, message, Instant.now().toString(), path);
    }
}
