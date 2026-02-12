package com.spendwise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record CreateBudgetRequest(

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @NotNull(message = "Year is required")
        Integer year,

        @NotNull(message = "Month is required")
        Integer month,

        @NotNull(message = "At least one category is required")
        Set<UUID> categoryIds
) {
}

