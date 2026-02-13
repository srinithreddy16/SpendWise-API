package com.spendwise.service;

import com.spendwise.domain.entity.Budget;
import com.spendwise.domain.entity.Expense;
import com.spendwise.exception.UnauthorizedAccessException;
import com.spendwise.repository.BudgetRepository;
import com.spendwise.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Reusable ownership validation for user-owned resources.
 * <p>
 * Ownership checks must be performed in the service layer only, not in controllers.
 * Controllers obtain the current user id (e.g. from the security context) and pass it
 * together with resource ids into service methods; this component validates that the
 * resource exists and belongs to that user in a single efficient query.
 * <p>
 * Throws {@link UnauthorizedAccessException} when the resource does not exist or
 * does not belong to the user, so that we do not leak whether a resource exists
 * for another user.
 */
@Service
public class OwnershipValidationService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public OwnershipValidationService(ExpenseRepository expenseRepository,
                                      BudgetRepository budgetRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }


    /**
     * Validates that the given expense exists, is not soft deleted, and belongs to the user.
     * Uses a single query (id + user id) for efficiency.
     *
     * @param userId   the current user's id
     * @param expenseId the expense id
     * @return the expense if it exists and is owned by the user
     * @throws UnauthorizedAccessException if the expense is not found or not owned by the user
     */
    public Expense validateUserOwnsExpense(UUID userId, UUID expenseId) {
        return expenseRepository.findByIdAndUser_IdAndDeletedIsFalse(expenseId, userId)
                .orElseThrow(() -> new UnauthorizedAccessException("You are not allowed to access this expense"));
    }

    /**
     * Validates that
     * the given budget exists, is not soft deleted, and belongs to the user.
     * Uses a single query (id + user id) for efficiency.
     *
     * @param userId   the current user's id
     * @param budgetId the budget id
     * @return the budget if it exists and is owned by the user
     * @throws UnauthorizedAccessException if the budget is not found or not owned by the user
     */
    public Budget validateUserOwnsBudget(UUID userId, UUID budgetId) {
        return budgetRepository.findByIdAndUser_IdAndDeletedAtIsNull(budgetId, userId)
                .orElseThrow(() -> new UnauthorizedAccessException("You are not allowed to access this budget"));
    }
}
