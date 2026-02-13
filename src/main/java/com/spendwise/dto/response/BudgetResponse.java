package com.spendwise.dto.response;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Response DTO for budget data with calculated metrics.
 * ID is included for update/delete operations. Metrics (totalSpent, remainingBudget) are calculated
 * based on expenses for the budget period and categories.
 */
public record BudgetResponse(
        UUID id,
        BigDecimal amount,
        int year,
        int month,
        Set<UUID> categoryIds,
        BigDecimal totalSpent,
        BigDecimal remainingBudget
) {}
