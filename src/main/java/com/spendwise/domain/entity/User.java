package com.spendwise.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Owner of expenses, categories, and budgets.
 * A User can have many Expenses and define multiple Budgets (expressed via Expense.user and Budget.user).
 * Referenced unidirectionally by child entities (Expense, Category, Budget).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    @Column(name = "password_hash")
    private String passwordHash;
}
