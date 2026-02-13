package com.spendwise.dto.error;

import com.spendwise.exception.ErrorCode;

import java.time.Instant;

/**
 * Standard error response DTO for API error responses.
 * Contains error code, user-friendly message, and timestamp.
 * Never exposes stack traces or internal implementation details.
 */
public record ErrorResponse(String errorCode, String message, String timestamp) {

    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, Instant.now().toString());
    }

    public static ErrorResponse of(ErrorCode code) {
        return of(code.name(), code.getClientMessage());
    }
}
