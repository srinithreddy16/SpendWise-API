package com.spendwise.unit.service;

import com.spendwise.domain.entity.Budget;
import com.spendwise.domain.entity.Category;
import com.spendwise.domain.entity.User;
import com.spendwise.dto.request.CreateExpenseRequest;
import com.spendwise.dto.response.ExpenseResponse;
import com.spendwise.exception.BudgetExceededException;
import com.spendwise.mapper.ExpenseMapper;
import com.spendwise.repository.BudgetRepository;
import com.spendwise.repository.CategoryRepository;
import com.spendwise.repository.ExpenseRepository;
import com.spendwise.repository.UserRepository;
import com.spendwise.service.ExpenseService;
import com.spendwise.service.OwnershipValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService budget validation")
class ExpenseServiceBudgetValidationTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private OwnershipValidationService ownershipValidationService;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseService expenseService;

    private UUID userId;
    private UUID categoryId;
    private User user;
    private Category category;
    private LocalDate expenseDate;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        expenseDate = LocalDate.of(2025, 3, 15);

        user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setPassword("hashed");

        category = new Category();
        category.setId(categoryId);
        category.setName("Food");
        category.setUser(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findByIdAndUser_Id(categoryId, userId)).thenReturn(Optional.of(category));
    }

    @Nested
    @DisplayName("when no budget exists")
    class NoBudgetExists {

        @Test
        @DisplayName("createExpense should pass without exception")
        void createsExpenseWhenNoBudget() {
            CreateExpenseRequest request = new CreateExpenseRequest(
                    categoryId,
                    new BigDecimal("500"),
                    "Lunch",
                    expenseDate
            );

            when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("0"));
            when(budgetRepository.findByUserAndYearAndMonthAndCategory(
                    eq(userId), eq(2025), eq(3), eq(categoryId)))
                    .thenReturn(List.of());
            when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(expenseMapper.toExpenseResponse(any())).thenReturn(new ExpenseResponse(
                    UUID.randomUUID(), categoryId, request.amount(), request.description(), request.expenseDate()));

            expenseService.createExpense(userId, request);

            verify(expenseRepository).save(any());
        }
    }

    @Nested
    @DisplayName("when budget exists and expense is within limit")
    class BudgetExistsWithinLimit {

        @Test
        @DisplayName("createExpense should pass when alreadySpent + amount <= budget")
        void createsExpenseWhenWithinLimit() {
            BigDecimal budgetAmount = new BigDecimal("1000");
            BigDecimal alreadySpent = new BigDecimal("200");
            BigDecimal expenseAmount = new BigDecimal("500");

            CreateExpenseRequest request = new CreateExpenseRequest(
                    categoryId,
                    expenseAmount,
                    "Groceries",
                    expenseDate
            );

            when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(alreadySpent);

            Budget budget = new Budget();
            budget.setAmount(budgetAmount);
            budget.setYear(2025);
            budget.setMonth(3);
            when(budgetRepository.findByUserAndYearAndMonthAndCategory(
                    eq(userId), eq(2025), eq(3), eq(categoryId)))
                    .thenReturn(List.of(budget));

            when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(expenseMapper.toExpenseResponse(any())).thenReturn(new ExpenseResponse(
                    UUID.randomUUID(), categoryId, expenseAmount, request.description(), request.expenseDate()));

            expenseService.createExpense(userId, request);

            verify(expenseRepository).save(any());
        }

        @Test
        @DisplayName("createExpense should pass when projected equals budget exactly")
        void createsExpenseWhenProjectedEqualsBudgetExactly() {
            BigDecimal budgetAmount = new BigDecimal("1000");
            BigDecimal alreadySpent = new BigDecimal("700");
            BigDecimal expenseAmount = new BigDecimal("300");

            CreateExpenseRequest request = new CreateExpenseRequest(
                    categoryId,
                    expenseAmount,
                    "Dinner",
                    expenseDate
            );

            when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(alreadySpent);

            Budget budget = new Budget();
            budget.setAmount(budgetAmount);
            budget.setYear(2025);
            budget.setMonth(3);
            when(budgetRepository.findByUserAndYearAndMonthAndCategory(
                    eq(userId), eq(2025), eq(3), eq(categoryId)))
                    .thenReturn(List.of(budget));

            when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(expenseMapper.toExpenseResponse(any())).thenReturn(new ExpenseResponse(
                    UUID.randomUUID(), categoryId, expenseAmount, request.description(), request.expenseDate()));

            expenseService.createExpense(userId, request);

            verify(expenseRepository).save(any());
        }
    }

    @Nested
    @DisplayName("when budget exists and expense exceeds limit")
    class BudgetExistsExceedsLimit {

        @Test
        @DisplayName("createExpense should throw BudgetExceededException")
        void shouldThrowBudgetExceededException() {
            BigDecimal budgetAmount = new BigDecimal("1000");
            BigDecimal alreadySpent = new BigDecimal("200");
            BigDecimal expenseAmount = new BigDecimal("900");

            CreateExpenseRequest request = new CreateExpenseRequest(
                    categoryId,
                    expenseAmount,
                    "Shopping",
                    expenseDate
            );

            when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(alreadySpent);

            Budget budget = new Budget();
            budget.setAmount(budgetAmount);
            budget.setYear(2025);
            budget.setMonth(3);
            when(budgetRepository.findByUserAndYearAndMonthAndCategory(
                    eq(userId), eq(2025), eq(3), eq(categoryId)))
                    .thenReturn(List.of(budget));

            assertThatThrownBy(() -> expenseService.createExpense(userId, request))
                    .isInstanceOf(BudgetExceededException.class)
                    .hasMessageContaining("Expense exceeds remaining monthly budget");

            verify(expenseRepository).sumAmountByUserAndCategoryAndDateRange(
                    eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class));
            verify(budgetRepository).findByUserAndYearAndMonthAndCategory(
                    eq(userId), eq(2025), eq(3), eq(categoryId));
        }

        @Test
        @DisplayName("createExpense should throw BudgetExceededException when sum returns null")
        void shouldThrowWhenAlreadySpentIsNull() {
            when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(null);

            Budget budget = new Budget();
            budget.setAmount(new BigDecimal("100"));
            budget.setYear(2025);
            budget.setMonth(3);
            when(budgetRepository.findByUserAndYearAndMonthAndCategory(
                    eq(userId), eq(2025), eq(3), eq(categoryId)))
                    .thenReturn(List.of(budget));

            CreateExpenseRequest request = new CreateExpenseRequest(
                    categoryId,
                    new BigDecimal("150"),
                    "Test",
                    expenseDate
            );

            assertThatThrownBy(() -> expenseService.createExpense(userId, request))
                    .isInstanceOf(BudgetExceededException.class)
                    .hasMessageContaining("Expense exceeds remaining monthly budget");
        }
    }
}
