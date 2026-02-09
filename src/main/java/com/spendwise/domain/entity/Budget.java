package com.spendwise.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Spending limit for a period.
 * Belongs to one User. A Budget can apply to multiple Categories (ManyToMany).
 * Empty set can mean no category filter or 'all categories' depending on business rules.
 */
@Entity
@Table(name = "budgets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Budget extends BaseEntity {

    /**
     * Owner of this budget. Unidirectional: User does not hold a collection of budgets.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    /**
     * Categories this budget applies to. Unidirectional: Category does not hold a collection of budgets.
     * Join table budget_categories links budget_id to category_id.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "budget_categories",
            joinColumns = @JoinColumn(name = "budget_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    private BigDecimal amount;

    /**
     * Year of the budget period (e.g. 2025).
     */
    private int year;

    /**
     * Month of the budget period (1-12), or null for yearly budget.
     */
    private Integer month;
}
