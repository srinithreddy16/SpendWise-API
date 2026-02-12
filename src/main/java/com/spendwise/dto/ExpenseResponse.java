package com.spendwise.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExpenseResponse(

        UUID id,

        UUID categoryId,

        BigDecimal amount,

        String description,

        LocalDate expenseDate
) {
}

