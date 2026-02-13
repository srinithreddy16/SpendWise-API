package com.spendwise.service;

import com.spendwise.domain.entity.Budget;
import com.spendwise.domain.entity.Category;
import com.spendwise.domain.entity.Expense;
import com.spendwise.domain.entity.User;
import com.spendwise.dto.BudgetResponse;
import com.spendwise.dto.CreateBudgetRequest;
import com.spendwise.dto.UpdateBudgetRequest;
import com.spendwise.exception.DuplicateBudgetException;
import com.spendwise.exception.ResourceNotFoundException;
import com.spendwise.repository.BudgetRepository;
import com.spendwise.repository.CategoryRepository;
import com.spendwise.repository.ExpenseRepository;
import com.spendwise.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private static final int MIN_MONTH = 1;
    private static final int MAX_MONTH = 12;

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final OwnershipValidationService ownershipValidationService;

    public BudgetService(BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         UserRepository userRepository,
                         ExpenseRepository expenseRepository,
                         OwnershipValidationService ownershipValidationService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.ownershipValidationService = ownershipValidationService;
    }

    // --- New DTO-based API ---

    /**
     * Creates a new budget for a user. Write operation that must be atomic:
     * uniqueness validation, category loading, budget creation, and save must succeed together.
     * Uses @Transactional to ensure consistency.
     */
    @Transactional
    public BudgetResponse createBudget(UUID currentUserId, CreateBudgetRequest request) {
        User user = loadUser(currentUserId);
        validateMonth(request.month());
        ensureMonthlyUnique(user.getId(), request.year(), request.month());

        Set<Category> categories = loadCategoriesForUser(request.categoryIds(), currentUserId);

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setAmount(request.amount());
        budget.setYear(request.year());
        budget.setMonth(request.month());
        budget.setCategories(categories);
        budget.setDeletedAt(null);

        Budget saved = budgetRepository.save(budget);
        BudgetMetrics metrics = calculateBudgetMetrics(user, saved);
        return toBudgetResponse(saved, metrics);
    }

    /**
     * Updates an existing budget. Write operation that must be atomic:
     * ownership validation, field updates, uniqueness checks, and save must succeed together.
     * Uses @Transactional to ensure consistency.
     */
    @Transactional
    public BudgetResponse updateBudget(UUID currentUserId, UUID budgetId, UpdateBudgetRequest request) {
        Budget budget = ownershipValidationService.validateUserOwnsBudget(currentUserId, budgetId);

        applyUpdate(budget, request, currentUserId);

        Budget saved = budgetRepository.save(budget);
        BudgetMetrics metrics = calculateBudgetMetrics(saved.getUser(), saved);
        return toBudgetResponse(saved, metrics);
    }

    /**
     * Retrieves budgets for a user filtered by year and month. Read-only operation:
     * queries database for budgets and calculates metrics, but performs no mutations.
     * Uses @Transactional(readOnly = true) to document intent and allow persistence provider optimizations.
     * Note: Metrics calculation involves in-memory aggregation but all database access is read-only.
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetsForUser(UUID currentUserId, int year, int month) {
        validateMonth(month);
        List<Budget> budgets = budgetRepository.findByUser_IdAndYearAndDeletedAtIsNullOrderByMonthAsc(currentUserId, year)
                .stream()
                .filter(b -> b.getMonth() != null && b.getMonth() == month)
                .collect(Collectors.toList());

        User user = loadUser(currentUserId);
        return budgets.stream()
                .map(b -> {
                    BudgetMetrics metrics = calculateBudgetMetrics(user, b);
                    return toBudgetResponse(b, metrics);
                })
                .collect(Collectors.toList());
    }

    // --- Helpers and existing behavior encapsulation ---

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validateMonth(Integer month) {
        if (month == null || month < MIN_MONTH || month > MAX_MONTH) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
    }

    private void ensureMonthlyUnique(UUID userId, int year, int month) {
        if (budgetRepository.existsByUser_IdAndYearAndMonthAndDeletedAtIsNull(userId, year, month)) {
            throw new DuplicateBudgetException("A budget already exists for this user, year and month");
        }
    }

    private Set<Category> loadCategoriesForUser(Set<UUID> categoryIds, UUID currentUserId) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new HashSet<>();
        }
        Set<Category> categories = new HashSet<>();
        for (UUID categoryId : categoryIds) {
            Category category = categoryRepository.findByIdAndUser_Id(categoryId, currentUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found or access denied: " + categoryId));
            categories.add(category);
        }
        return categories;
    }

    private void applyUpdate(Budget budget, UpdateBudgetRequest request, UUID currentUserId) {
        Integer newYear = request.year() != null ? request.year() : budget.getYear();
        Integer newMonth = request.month() != null ? request.month() : budget.getMonth();
        validateMonth(newMonth);

        // If year/month changed, enforce uniqueness for the new combination
        if (newYear != budget.getYear() || !newMonth.equals(budget.getMonth())) {
            if (budgetRepository.countByUserAndYearAndMonthExcludingId(
                    budget.getUser().getId(), newYear, newMonth, budget.getId()) > 0) {
                throw new DuplicateBudgetException("A budget already exists for this user, year and month");
            }
        }

        if (request.amount() != null) {
            budget.setAmount(request.amount());
        }
        budget.setYear(newYear);
        budget.setMonth(newMonth);

        if (request.categoryIds() != null) {
            Set<Category> categories = loadCategoriesForUser(request.categoryIds(), currentUserId);
            budget.setCategories(categories);
        }
    }

    // --- Metrics & mapping ---

    private BudgetMetrics calculateBudgetMetrics(User user, Budget budget) {
        YearMonth ym = YearMonth.of(budget.getYear(), budget.getMonth());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // Load expenses for the month with category JOIN FETCH to avoid N+1 when filtering by category
        // Uses deleted = false to exclude soft-deleted expenses
        List<Expense> monthlyExpenses = expenseRepository.findByUserAndDateRangeWithCategory(
                user.getId(), start, end);

        Set<UUID> categoryIds = budget.getCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        BigDecimal totalSpent = monthlyExpenses.stream()
                .filter(e -> categoryIds.contains(e.getCategory().getId()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = budget.getAmount().subtract(totalSpent);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        return new BudgetMetrics(totalSpent, remaining);
    }

    private BudgetResponse toBudgetResponse(Budget budget, BudgetMetrics metrics) {
        Set<UUID> categoryIds = budget.getCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        return new BudgetResponse(
                budget.getId(),
                budget.getAmount(),
                budget.getYear(),
                budget.getMonth() != null ? budget.getMonth() : 0,
                categoryIds,
                metrics.totalSpent(),
                metrics.remainingBudget()
        );
    }

    private record BudgetMetrics(BigDecimal totalSpent, BigDecimal remainingBudget) {
    }
}

