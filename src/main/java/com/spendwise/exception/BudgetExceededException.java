package com.spendwise.exception;

public class BudgetExceededException extends ApiException {

    public BudgetExceededException(String message) {
        super(ErrorCode.BUDGET_EXCEEDED, message);
    }

    public BudgetExceededException(String message, Throwable cause) {
        super(ErrorCode.BUDGET_EXCEEDED, message, cause);
    }
}
