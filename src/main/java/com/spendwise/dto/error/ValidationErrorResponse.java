package com.spendwise.dto.error;

import java.time.Instant;
import java.util.Map;

/**
 * Validation error response DTO for field-level validation errors.
 * Contains error code, general message, timestamp, and a map of field-specific errors.
 */
public record ValidationErrorResponse(String errorCode, String message, String timestamp, Map<String, String> errors) {

    public static ValidationErrorResponse of(String errorCode, String message, Map<String, String> errors) {
        return new ValidationErrorResponse(errorCode, message, Instant.now().toString(), errors);
    }
}
