package com.spendwise.dto;

import java.time.Instant;

public record ErrorResponse(String errorCode, String message, String timestamp) {

    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, Instant.now().toString());
    }
}
