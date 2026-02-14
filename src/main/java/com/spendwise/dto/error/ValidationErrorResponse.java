package com.spendwise.dto.error;

import java.time.Instant;
import java.util.Map;

/**
 * Validation error response DTO for field-level validation errors.
 * Contains error code, general message, timestamp, path, and a map of field-specific errors.
 */
public record ValidationErrorResponse(String errorCode, String message, String timestamp, String path, Map<String, String> errors) {

    public static ValidationErrorResponse of(String errorCode, String message, String path, Map<String, String> errors) {
        return new ValidationErrorResponse(errorCode, message, Instant.now().toString(), path, errors);
    }
}
