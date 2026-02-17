package com.spendwise.repository;

import com.spendwise.config.JpaAuditingConfig;
import com.spendwise.domain.entity.Category;
import com.spendwise.domain.entity.Expense;
import com.spendwise.domain.entity.User;
import com.spendwise.domain.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@DisplayName("ExpenseRepository")
class ExpenseRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User userA;
    private User userB;
    private Category categoryA;
    private Category categoryB;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setEmail("userA-" + UUID.randomUUID() + "@test.com");
        userA.setPassword("hash");
        userA.setRole(Role.USER);
        userA = userRepository.save(userA);

        userB = new User();
        userB.setEmail("userB-" + UUID.randomUUID() + "@test.com");
        userB.setPassword("hash");
        userB.setRole(Role.USER);
        userB = userRepository.save(userB);

        categoryA = new Category();
        categoryA.setName("Food");
        categoryA.setUser(userA);
        categoryA = categoryRepository.save(categoryA);

        categoryB = new Category();
        categoryB.setName("Transport");
        categoryB.setUser(userB);
        categoryB = categoryRepository.save(categoryB);
    }

    @Nested
    @DisplayName("sumAmountByUserAndCategoryAndDateRange")
    class SumAmountByUserAndCategoryAndDateRange {

        @Test
        @DisplayName("sums multiple expenses in date range")
        void monthlySumAggregation() {
            LocalDate start = LocalDate.of(2025, 3, 1);
            LocalDate end = LocalDate.of(2025, 3, 31);

            saveExpense(userA, categoryA, new BigDecimal("100"), LocalDate.of(2025, 3, 5), false);
            saveExpense(userA, categoryA, new BigDecimal("200"), LocalDate.of(2025, 3, 15), false);
            saveExpense(userA, categoryA, new BigDecimal("50"), LocalDate.of(2025, 3, 25), false);

            BigDecimal sum = expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    userA.getId(), categoryA.getId(), start, end);

            assertThat(sum).isEqualByComparingTo(new BigDecimal("350"));
        }

        @Test
        @DisplayName("excludes soft-deleted expenses")
        void excludesSoftDeleted() {
            LocalDate start = LocalDate.of(2025, 3, 1);
            LocalDate end = LocalDate.of(2025, 3, 31);

            saveExpense(userA, categoryA, new BigDecimal("100"), LocalDate.of(2025, 3, 5), false);
            saveExpense(userA, categoryA, new BigDecimal("200"), LocalDate.of(2025, 3, 15), true);

            BigDecimal sum = expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    userA.getId(), categoryA.getId(), start, end);

            assertThat(sum).isEqualByComparingTo(new BigDecimal("100"));
        }

        @Test
        @DisplayName("filters by ownership")
        void filtersByOwnership() {
            Category categoryForBoth = new Category();
            categoryForBoth.setName("SharedCategory");
            categoryForBoth.setUser(userA);
            categoryForBoth = categoryRepository.save(categoryForBoth);

            saveExpense(userA, categoryForBoth, new BigDecimal("100"), LocalDate.of(2025, 3, 10), false);
            saveExpense(userB, categoryB, new BigDecimal("500"), LocalDate.of(2025, 3, 10), false);

            LocalDate start = LocalDate.of(2025, 3, 1);
            LocalDate end = LocalDate.of(2025, 3, 31);

            BigDecimal sumUserA = expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    userA.getId(), categoryForBoth.getId(), start, end);
            BigDecimal sumUserB = expenseRepository.sumAmountByUserAndCategoryAndDateRange(
                    userB.getId(), categoryB.getId(), start, end);

            assertThat(sumUserA).isEqualByComparingTo(new BigDecimal("100"));
            assertThat(sumUserB).isEqualByComparingTo(new BigDecimal("500"));
        }
    }

    @Nested
    @DisplayName("findAllByUserExcludingDeleted")
    class FindAllByUserExcludingDeleted {

        @Test
        @DisplayName("returns only active expenses for user")
        void excludesDeletedAndFiltersByOwnership() {
            saveExpense(userA, categoryA, new BigDecimal("10"), LocalDate.of(2025, 1, 1), false);
            saveExpense(userA, categoryA, new BigDecimal("20"), LocalDate.of(2025, 1, 2), false);
            saveExpense(userA, categoryA, new BigDecimal("30"), LocalDate.of(2025, 1, 3), true);

            var result = expenseRepository.findAllByUserExcludingDeleted(userA.getId());

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findByIdAndUser_IdAndDeletedIsFalse")
    class FindByIdAndUserAndDeletedIsFalse {

        @Test
        @DisplayName("returns expense when owned and not deleted")
        void returnsWhenOwnedAndActive() {
            Expense expense = saveExpense(userA, categoryA, new BigDecimal("50"), LocalDate.of(2025, 1, 1), false);

            Optional<Expense> found = expenseRepository.findByIdAndUser_IdAndDeletedIsFalse(
                    expense.getId(), userA.getId());

            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("returns empty when expense is soft-deleted")
        void returnsEmptyWhenDeleted() {
            Expense expense = saveExpense(userA, categoryA, new BigDecimal("50"), LocalDate.of(2025, 1, 1), true);

            Optional<Expense> found = expenseRepository.findByIdAndUser_IdAndDeletedIsFalse(
                    expense.getId(), userA.getId());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("returns empty when wrong user")
        void returnsEmptyWhenWrongUser() {
            Expense expense = saveExpense(userA, categoryA, new BigDecimal("50"), LocalDate.of(2025, 1, 1), false);

            Optional<Expense> found = expenseRepository.findByIdAndUser_IdAndDeletedIsFalse(
                    expense.getId(), userB.getId());

            assertThat(found).isEmpty();
        }
    }

    private Expense saveExpense(User user, Category category, BigDecimal amount, LocalDate expenseDate, boolean deleted) {
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(category);
        expense.setAmount(amount);
        expense.setExpenseDate(expenseDate);
        expense.setDeleted(deleted);
        expense.setDeletedAt(deleted ? Instant.now() : null);
        return expenseRepository.save(expense);
    }
}
