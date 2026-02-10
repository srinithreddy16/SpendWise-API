package com.spendwise.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Expense category (e.g. Food, Transport).
 * Belongs to one User. Referenced unidirectionally by Expense and Budget.
 */
@Entity
@Table(
        name = "categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseEntity {

    @NotBlank
    private String name;

    /**
     * Owner of this category. Unidirectional: User does not hold a collection of categories.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // means many categories belong to one user, the user is loaded only when accessed(needed), and a category cannot exist without a user.
    private User user;
}
