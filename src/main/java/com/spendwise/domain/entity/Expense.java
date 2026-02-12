package com.spendwise.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Single expense record.
 * An Expense belongs to one Category. Belongs to one User.
 * Referenced unidirectionally by ExpenseAuditLog.
 */
@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Expense extends BaseEntity {

    /**
     * Owner of this expense. Unidirectional: User does not hold a collection of expenses.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    /**
     * Category of this expense. Unidirectional: Category does not hold a collection of expenses.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Category category;

    @Column(nullable = false)
    @NotNull
    @Positive
    private BigDecimal amount;

    private String description;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    /**
     * When non-null, the expense is soft-deleted and should be excluded from normal reads.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
