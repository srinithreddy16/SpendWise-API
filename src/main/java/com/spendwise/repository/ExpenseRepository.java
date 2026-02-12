package com.spendwise.repository;

import com.spendwise.domain.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Optional<Expense> findByIdAndDeletedAtIsNull(UUID id); //Find an expense by ID ,but only if it is not soft deleted

    List<Expense> findByUser_IdAndDeletedAtIsNullOrderByExpenseDateDesc(UUID userId);

    List<Expense> findByUser_IdAndCategory_IdAndDeletedAtIsNullOrderByExpenseDateDesc(UUID userId, UUID categoryId); //expense.user.id = ?AND expense.category.id = ?AND deleted_at IS NULLORDER BY expenseDate DESC

    List<Expense> findByUser_IdAndExpenseDateBetweenAndDeletedAtIsNullOrderByExpenseDateDesc(
            UUID userId, LocalDate start, LocalDate end); //expense.user.id = ?AND expenseDate BETWEEN start AND endAND deleted_at IS NULLORDER BY expenseDate DESC
}
