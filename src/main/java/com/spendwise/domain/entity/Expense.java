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
     * Retained for audit trail purposes to track when deletion occurred.
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Soft delete flag indicating whether this expense has been deleted.
     * <p>
     * <b>Why soft delete is critical in financial systems:</b>
     * <ul>
     *   <li><b>Audit Trail & Compliance:</b> Financial records must be preserved for regulatory
     *       compliance (SOX, GAAP, tax regulations). Physical deletion would violate data retention
     *       requirements and make audits impossible.</li>
     *   <li><b>Data Recovery:</b> Accidental deletions can be reversed without data loss, critical
     *       for financial accuracy and user trust.</li>
     *   <li><b>Referential Integrity:</b> Related records (audit logs, reports, budget calculations)
     *       remain valid and consistent. Physical deletion would break foreign key relationships or
     *       require cascading deletes that lose historical context.</li>
     *   <li><b>Historical Reporting:</b> Past financial reports remain accurate and reproducible.
     *       Deleted expenses don't disappear from historical monthly/yearly summaries, maintaining
     *       financial statement integrity.</li>
     *   <li><b>Forensic Analysis:</b> Deleted records may be needed for investigations, fraud detection,
     *       or dispute resolution. Soft delete preserves evidence while hiding from normal operations.</li>
     * </ul>
     * <p>
     * <b>Implementation details:</b>
     * <ul>
     *   <li>Default value is {@code false} (not deleted)</li>
     *   <li>All repository queries filter by {@code deleted = false} to exclude deleted records</li>
     *   <li>The {@code deletedAt} timestamp tracks when deletion occurred (for audit purposes)</li>
     *   <li>Physical deletion is never performed - records are preserved indefinitely</li>
     * </ul>
     */
    @Column(nullable = false)
    private boolean deleted = false;
}
