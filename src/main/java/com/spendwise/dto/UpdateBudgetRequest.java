package com.spendwise.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record UpdateBudgetRequest(

        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        Integer year,

        Integer month,

        Set<UUID> categoryIds
) {
}

