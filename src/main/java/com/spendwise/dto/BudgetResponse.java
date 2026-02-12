package com.spendwise.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record BudgetResponse(

        UUID id,

        BigDecimal amount,

        int year,

        int month,

        Set<UUID> categoryIds,

        BigDecimal totalSpent,

        BigDecimal remainingBudget
) {
}

