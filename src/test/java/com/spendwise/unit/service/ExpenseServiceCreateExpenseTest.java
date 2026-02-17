package com.spendwise.unit.service;

import com.spendwise.domain.entity.Budget;
import com.spendwise.domain.entity.Category;
import com.spendwise.domain.entity.Expense;
import com.spendwise.domain.entity.User;
import com.spendwise.dto.request.CreateExpenseRequest;
import com.spendwise.dto.response.ExpenseResponse;
import com.spendwise.exception.BudgetExceededException;
import com.spendwise.exception.ResourceNotFoundException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpenseService createExpense")
class ExpenseServiceCreateExpenseTest {

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
    private CreateExpenseRequest validRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        LocalDate expenseDate = LocalDate.of(2025, 3, 15);

        user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setPassword("hashed");

        category = new Category();
        category.setId(categoryId);
        category.setName("Food");
        category.setUser(user);

        validRequest = new CreateExpenseRequest(
                categoryId,
                new BigDecimal("100.50"),
                "Lunch",
                expenseDate
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(categoryRepository.findByIdAndUser_Id(categoryId, userId)).thenReturn(Optional.of(category));
    }

    @Nested
    @DisplayName("valid expense")
    class ValidExpense {

        @Test
        @DisplayName("should save expense and return mapped response")
        void shouldSaveAndReturnResponse() {
            when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(budgetRepository.findByUserAndYearAndMonthAndCategory(
                    eq(userId), eq(2025), eq(3), eq(categoryId)))
                    .thenReturn(List.of());
            when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ExpenseResponse expectedResponse = new ExpenseResponse(
                    UUID.randomUUID(), categoryId, validRequest.amount(),
                    validRequest.description(), validRequest.expenseDate());
            when(expenseMapper.toExpenseResponse(any())).thenReturn(expectedResponse);

            ExpenseResponse result = expenseService.createExpense(userId, validRequest);

            assertThat(result).isEqualTo(expectedResponse);
            verify(expenseRepository).save(any());
            verify(expenseMapper).toExpenseResponse(any());
        }

        @Test
        @DisplayName("expense passed to save has correct user, category, amount, description, expenseDate")
        void shouldPassCorrectExpenseToSave() {
            when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(budgetRepository.findByUserAndYearAndMonthAndCategory(
                    eq(userId), eq(2025), eq(3), eq(categoryId)))
                    .thenReturn(List.of());
            when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(expenseMapper.toExpenseResponse(any())).thenReturn(
                    new ExpenseResponse(UUID.randomUUID(), categoryId, validRequest.amount(),
                            validRequest.description(), validRequest.expenseDate()));

            expenseService.createExpense(userId, validRequest);

            ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
            verify(expenseRepository).save(captor.capture());
            Expense saved = captor.getValue();

            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getCategory()).isEqualTo(category);
            assertThat(saved.getAmount()).isEqualByComparingTo(validRequest.amount());
            assertThat(saved.getDescription()).isEqualTo(validRequest.description());
            assertThat(saved.getExpenseDate()).isEqualTo(validRequest.expenseDate());
        }

        @Test
        @DisplayName("soft delete logic respected: expense has deleted=false and deletedAt=null")
        void softDeleteLogicRespected() {
            when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(budgetRepository.findByUserAndYearAndMonthAndCategory(
                    eq(userId), eq(2025), eq(3), eq(categoryId)))
                    .thenReturn(List.of());
            when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(expenseMapper.toExpenseResponse(any())).thenReturn(
                    new ExpenseResponse(UUID.randomUUID(), categoryId, validRequest.amount(),
                            validRequest.description(), validRequest.expenseDate()));

            expenseService.createExpense(userId, validRequest);

            ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
            verify(expenseRepository).save(captor.capture());
            Expense saved = captor.getValue();

            assertThat(saved.isDeleted()).isFalse();
            assertThat(saved.getDeletedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("exceeds budget")
    class ExceedsBudget {

        @Test
        @DisplayName("should throw BudgetExceededException and NOT call save")
        void shouldThrowAndNotSave() {
            when(expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    eq(userId), eq(categoryId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(new BigDecimal("800"));
            Budget budget = new Budget();
            budget.setAmount(new BigDecimal("1000"));
            budget.setYear(2025);
            budget.setMonth(3);
            when(budgetRepository.findByUserAndYearAndMonthAndCategory(
                    eq(userId), eq(2025), eq(3), eq(categoryId)))
                    .thenReturn(List.of(budget));

            CreateExpenseRequest request = new CreateExpenseRequest(
                    categoryId, new BigDecimal("300"), "Over limit", validRequest.expenseDate());

            assertThatThrownBy(() -> expenseService.createExpense(userId, request))
                    .isInstanceOf(BudgetExceededException.class)
                    .hasMessageContaining("Expense exceeds remaining monthly budget");

            verify(expenseRepository, never()).save(any());
            verify(budgetRepository).findByUserAndYearAndMonthAndCategory(
                    eq(userId), eq(2025), eq(3), eq(categoryId));
        }
    }

    @Nested
    @DisplayName("unauthorized user - user not found")
    class UserNotFound {

        @Test
        @DisplayName("should throw ResourceNotFoundException and NOT call save or categoryRepository")
        void shouldThrowAndNotCallSaveOrCategoryRepo() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.createExpense(userId, validRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            verify(expenseRepository, never()).save(any());
            verify(categoryRepository, never()).findByIdAndUser_Id(any(), any());
        }
    }

    @Nested
    @DisplayName("unauthorized user - category not owned")
    class CategoryNotOwned {

        @Test
        @DisplayName("should throw ResourceNotFoundException and NOT call save or expenseRepository sum")
        void shouldThrowAndNotCallSaveOrSum() {
            when(categoryRepository.findByIdAndUser_Id(categoryId, userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.createExpense(userId, validRequest))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Category not found");

            verify(expenseRepository, never()).save(any());
            verify(expenseRepository, never()).sumAmountByUserAndCategoryAndDateRange(
                    any(), any(), any(), any());
        }
    }
}
