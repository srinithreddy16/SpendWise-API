package com.spendwise.exception;

/**
 * Central enum for API error codes and their safe, client-facing messages.
 * Ensures raw exception messages are never returned to clients.
 */
public enum ErrorCode {
    RESOURCE_NOT_FOUND("The requested resource was not found"),
    UNAUTHORIZED_ACCESS("You are not allowed to access this resource"),
    BUDGET_EXCEEDED("Expense exceeds remaining monthly budget"),
    VALIDATION_FAILED("Validation failed");

    private final String clientMessage;

    ErrorCode(String clientMessage) {

        this.clientMessage = clientMessage;
    }

    public String getClientMessage() {

        return clientMessage;
    }
}
