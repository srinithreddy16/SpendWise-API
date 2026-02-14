package com.spendwise.repository;

import com.spendwise.domain.entity.Expense;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Specifications for dynamic Expense filtering.
 */
public final class ExpenseSpecification {

    private ExpenseSpecification() {
    }

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
}
