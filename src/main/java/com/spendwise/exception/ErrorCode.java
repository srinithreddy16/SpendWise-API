package com.spendwise.exception;

/**
 * Central enum for API error codes and their safe, client-facing messages.
 * Ensures raw exception messages are never returned to clients.
 */
public enum ErrorCode {
    INVALID_CREDENTIALS("Invalid email or password"),
    UNAUTHORIZED("Authentication required"),
    RESOURCE_NOT_FOUND("The requested resource was not found"),
    BUDGET_EXCEEDED("Expense exceeds remaining monthly budget"),
    VALIDATION_ERROR("One or more fields failed validation"),
    INTERNAL_ERROR("An unexpected error occurred"),
    INVALID_REFRESH_TOKEN("Invalid or expired refresh token"),
    ACCESS_DENIED("Access denied"),
    EMAIL_ALREADY_EXISTS("Email already in use"),
    DUPLICATE_BUDGET("A budget already exists for this user, year and month"),
    INVALID_TOKEN("Invalid or expired token");


    private final String clientMessage;

    ErrorCode(String clientMessage) {
        this.clientMessage = clientMessage;
    }

    public String getClientMessage() {
        return clientMessage;
    }
}
