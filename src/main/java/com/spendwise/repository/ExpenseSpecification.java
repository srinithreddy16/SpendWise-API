package com.spendwise.repository;

import com.spendwise.domain.entity.Expense;
import com.spendwise.dto.request.ExpenseListParams;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Specifications for dynamic Expense filtering.
 It is a helper class that builds dynamic database filters for Expense.
 You build filters dynamically and combine them.
 This is called Specification pattern in Spring Data JPA.
 */
public final class ExpenseSpecification {

    private ExpenseSpecification() {
    }

    /**
     * Ownership filter plus JOIN FETCH for category to prevent N+1 on paginated list.
     * <p>
     * <b>Why fetch category:</b> ExpenseResponse includes categoryId, so mapping via
     * ExpenseMapper.toExpenseResponse calls expense.getCategory().getId() for each row.
     * Without JOIN FETCH, category would lazy-load once per expense (N+1). Fetching here
     * loads categories in the same query as expenses.
     * <p>
     * <b>When we skip fetch:</b> Spring Data runs a separate count query for pagination;
     * its CriteriaQuery has result type long. Applying fetch to the count query would
     * add unnecessary joins and could affect the count. We skip fetch when
     * getResultType() is Long/long so only the main entity query fetches category.
     * <p>
     * <b>Tradeoffs:</b>
     * <ul>
     *   <li><b>Specification fetch vs EAGER:</b> Fetch is scoped to list queries only.
     *       EAGER on Expense.category would load category even when not needed (e.g. single-expense load).</li>
     *   <li><b>Specification fetch vs dedicated @Query:</b> Specification keeps dynamic
     *       filters (categoryId, date range, amount) in one place. A separate @Query with
     *       JOIN FETCH would duplicate filter logic or require many method variants.</li>
     *   <li><b>JOIN FETCH vs @BatchSize:</b> For a single ManyToOne, one JOIN FETCH suffices.
     *       @BatchSize is useful for OneToMany; not needed here.</li>
     * </ul>
     */
    public static Specification<Expense> forUser(UUID userId) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("category", JoinType.INNER);
            }
            return cb.equal(root.get("user").get("id"), userId);
        };
    }

    public static Specification<Expense> notDeleted() {
        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<Expense> withCategoryId(UUID categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Expense> fromDate(LocalDate fromDate) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), fromDate);
    }

    public static Specification<Expense> toDate(LocalDate toDate) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), toDate);
    }

    public static Specification<Expense> minAmount(BigDecimal minAmount) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), minAmount);
    }

    public static Specification<Expense> maxAmount(BigDecimal maxAmount) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), maxAmount);
    }

    /**
     * Builds a combined specification from filter params. Applies user ownership and
     * not-deleted, then adds optional filters when params are non-null.
       fromParams() builds one final dynamic database query by combining
       required filters (user + notDeleted) with optional filters from ExpenseListParams.
     */
    public static Specification<Expense> fromParams(UUID userId, ExpenseListParams params) {
        Specification<Expense> spec = forUser(userId).and(notDeleted());
        if (params.categoryId() != null) {
            spec = spec.and(withCategoryId(params.categoryId()));
        }
        if (params.fromDate() != null) {
            spec = spec.and(fromDate(params.fromDate()));
        }
        if (params.toDate() != null) {
            spec = spec.and(toDate(params.toDate()));
        }
        if (params.minAmount() != null) {
            spec = spec.and(minAmount(params.minAmount()));
        }
        if (params.maxAmount() != null) {
            spec = spec.and(maxAmount(params.maxAmount()));
        }
        return spec;
    }
}
