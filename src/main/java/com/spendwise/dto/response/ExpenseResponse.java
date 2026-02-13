package com.spendwise.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for expense data.
 * Exposes only fields needed by API clients. ID is included for update/delete operations.
 */
public record ExpenseResponse(
        UUID id,
        UUID categoryId,
        BigDecimal amount,
        String description,
        LocalDate expenseDate
) {}
