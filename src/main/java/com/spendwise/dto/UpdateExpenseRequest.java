package com.spendwise.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateExpenseRequest(

        UUID categoryId,

        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        String description,

        LocalDate expenseDate
) {
}

