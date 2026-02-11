package com.spendwise.dto;

import java.time.Instant;
import java.util.Map;

public record ValidationErrorResponse(String errorCode, String message, String timestamp, Map<String, String> errors) {

    public static ValidationErrorResponse of(String errorCode, String message, Map<String, String> errors) {
        return new ValidationErrorResponse(errorCode, message, Instant.now().toString(), errors);
    }
}
