package com.spendwise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateExpenseRequest(

        @NotNull(message = "Category is required")
        UUID categoryId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        String description,

        @NotNull(message = "Expense date is required")
        LocalDate expenseDate
) {
}

