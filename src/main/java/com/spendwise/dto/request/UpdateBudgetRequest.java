package com.spendwise.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record UpdateBudgetRequest(
        @Positive(message = "Amount must be positive")
        BigDecimal amount,

        @Min(value = 1900, message = "Year must be at least 1900")
        @Max(value = 2100, message = "Year must be at most 2100")
        Integer year,

        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        Integer month,

        Set<UUID> categoryIds
) {}
