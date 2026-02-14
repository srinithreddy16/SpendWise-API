package com.spendwise.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Filter parameters for listing expenses.
 * It is a small DTO that groups all filtering parameters into one object.
 * All fields are optional.
 */
public record ExpenseListParams(
        UUID categoryId,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal minAmount,
        BigDecimal maxAmount
) {
    public static ExpenseListParams of(UUID categoryId, LocalDate fromDate, LocalDate toDate,
                                       BigDecimal minAmount, BigDecimal maxAmount) {
        return new ExpenseListParams(categoryId, fromDate, toDate, minAmount, maxAmount);
    }
}
