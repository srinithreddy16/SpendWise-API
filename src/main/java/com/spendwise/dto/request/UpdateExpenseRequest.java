package com.spendwise.dto.request;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateExpenseRequest(
        UUID categoryId,

        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        String description,

        @PastOrPresent(message = "Expense date cannot be in the future")
        LocalDate expenseDate
) {}
