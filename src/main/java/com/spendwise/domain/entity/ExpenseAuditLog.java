package com.spendwise.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immutable audit record for an expense change.
 * References one Expense. Unidirectional: Expense does not hold a collection of audit logs.
 */
//This class keeps a history of changes made to an Expense. Expense created → log it ,Expense updated → log it, Expense deleted → log it
@Entity
@Table(name = "expense_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseAuditLog extends BaseEntity {

    /**
     * The expense this log entry refers to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Expense expense;

    /**
     * Action that was performed on the expense.
     */
    @Enumerated(EnumType.STRING) // It tells JPA how to store an enum in the database.
    @Column(nullable = false)
    private AuditAction action;

    /**
     * Optional snapshot or diff details (e.g. JSON).
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    public enum AuditAction {
        CREATED,
        UPDATED,
        DELETED
    }
}
