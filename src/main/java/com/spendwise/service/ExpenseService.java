package com.spendwise.service;

import com.spendwise.domain.entity.Budget;
import com.spendwise.domain.entity.Category;
import com.spendwise.domain.entity.Expense;
import com.spendwise.domain.entity.User;
import com.spendwise.dto.CreateExpenseRequest;
import com.spendwise.dto.ExpenseResponse;
import com.spendwise.dto.UpdateExpenseRequest;
import com.spendwise.exception.BudgetExceededException;
import com.spendwise.exception.ResourceNotFoundException;
import com.spendwise.exception.UnauthorizedAccessException;
import com.spendwise.repository.BudgetRepository;
import com.spendwise.repository.CategoryRepository;
import com.spendwise.repository.ExpenseRepository;
import com.spendwise.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository,
                          BudgetRepository budgetRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.budgetRepository = budgetRepository;
    }

    //Creates a new expense Belongs to a specific user Returns an ExpenseResponse DTO and save to DB
    @Transactional
    public ExpenseResponse createExpense(UUID currentUserId, CreateExpenseRequest request) {
        User user = loadUser(currentUserId);
        Category category = loadCategoryForUser(request.categoryId(), currentUserId);

        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(category);
        expense.setAmount(request.amount());
        expense.setDescription(request.description());
        expense.setExpenseDate(request.expenseDate());
        expense.setDeletedAt(null);

        // Validate monthly budget for this user/category before saving.
        validateMonthlyBudget(user.getId(), category.getId(), expense.getAmount(), expense.getExpenseDate());

        Expense saved = expenseRepository.save(expense);
        return toExpenseResponse(saved);
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID currentUserId, UUID expenseId, UpdateExpenseRequest request) {
        Expense expense = loadOwnedExpense(expenseId, currentUserId);

        if (request.categoryId() != null && !request.categoryId().equals(expense.getCategory().getId())) {
            Category category = loadCategoryForUser(request.categoryId(), currentUserId);
            expense.setCategory(category);
        }
        if (request.amount() != null) {
            expense.setAmount(request.amount());
        }
        if (request.description() != null) {
            expense.setDescription(request.description());
        }
        if (request.expenseDate() != null) {
            expense.setExpenseDate(request.expenseDate());
        }

        // Re-validate budget with the potentially updated amount/date/category.
        validateMonthlyBudget(
                expense.getUser().getId(),
                expense.getCategory().getId(),
                expense.getAmount(),
                expense.getExpenseDate()
        );

        Expense saved = expenseRepository.save(expense);
        return toExpenseResponse(saved);
    }

    @Transactional
    public void deleteExpense(UUID currentUserId, UUID expenseId) {
        Expense expense = loadOwnedExpense(expenseId, currentUserId);
        // Soft delete via existing deletedAt semantics; row is not removed.
        expense.setDeletedAt(expense.getDeletedAt() == null ? java.time.Instant.now() : expense.getDeletedAt());
        expenseRepository.save(expense);
    }

    // N + 1 problem here
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesForUser(UUID currentUserId) {
        List<Expense> expenses = expenseRepository.findByUser_IdAndDeletedAtIsNullOrderByExpenseDateDesc(currentUserId);
        return expenses.stream()
                .map(this::toExpenseResponse)
                .toList();
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Category loadCategoryForUser(UUID categoryId, UUID currentUserId) {
        return categoryRepository.findByIdAndUser_Id(categoryId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or access denied"));
    }

    private Expense loadOwnedExpense(UUID expenseId, UUID currentUserId) {
        Expense expense = expenseRepository.findByIdAndDeletedAtIsNull(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        if (!expense.getUser().getId().equals(currentUserId)) {
            throw new UnauthorizedAccessException("You are not allowed to access this expense");
        }
        return expense;
    }

    private ExpenseResponse toExpenseResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getCategory().getId(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getExpenseDate()
        );
    }

    /**
     * Validates that adding an expense of the given amount for the given user/category
     * in the month of {@code expenseDate} does not exceed the configured monthly budget.
     * <p>
     * Behavior when no budget exists for that user/category/month: treated as \"no limit\".
     * For the month of the expense, calculate how much the user has already spent in that category.
     * If a budget exists and adding this expense would exceed it, throw an exception.”
     */
    private void validateMonthlyBudget(UUID userId, UUID categoryId, BigDecimal expenseAmount, LocalDate expenseDate) {
        if (expenseAmount == null || expenseDate == null || categoryId == null) {
            return;
        }

        YearMonth ym = YearMonth.from(expenseDate);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal alreadySpent = expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                userId, categoryId, start, end);
        if (alreadySpent == null) {
            alreadySpent = BigDecimal.ZERO;
        }

        List<Budget> budgets = budgetRepository.findByUserAndYearAndMonthAndCategory(
                userId, ym.getYear(), ym.getMonthValue(), categoryId);
        if (budgets.isEmpty()) {
            // No budget configured for this category in this month → treat as no limit.
            return;
        }

        BigDecimal budgetLimit = budgets.stream()
                .map(Budget::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal projected = alreadySpent.add(expenseAmount);
        if (projected.compareTo(budgetLimit) > 0) {
            throw new BudgetExceededException("Expense exceeds remaining monthly budget");
        }
    }
}

