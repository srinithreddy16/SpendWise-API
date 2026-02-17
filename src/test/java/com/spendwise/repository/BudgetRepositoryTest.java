package com.spendwise.repository;

import com.spendwise.config.JpaAuditingConfig;
import com.spendwise.domain.entity.Budget;
import com.spendwise.domain.entity.Category;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@DisplayName("BudgetRepository")
class BudgetRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("budget-user-" + UUID.randomUUID() + "@test.com");
        user.setPassword("hash");
        user.setRole(Role.USER);
        user = userRepository.save(user);

        category = new Category();
        category.setName("Food");
        category.setUser(user);
        category = categoryRepository.save(category);
    }

    @Nested
    @DisplayName("findByUserAndYearAndMonthAndCategory")
    class FindByUserAndYearAndMonthAndCategory {

        @Test
        @DisplayName("returns budget when user has budget with category for year/month")
        void returnsBudgetWithOwnershipAndCategoryJoin() {
            Budget budget = new Budget();
            budget.setUser(user);
            budget.setAmount(new BigDecimal("1000"));
            budget.setYear(2025);
            budget.setMonth(3);
            budget.setCategories(Set.of(category));
            budget.setDeletedAt(null);
            budgetRepository.save(budget);

            List<Budget> result = budgetRepository.findByUserAndYearAndMonthAndCategory(
                    user.getId(), 2025, 3, category.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1000"));
        }

        @Test
        @DisplayName("excludes soft-deleted budget")
        void excludesSoftDeleted() {
            Budget budget = new Budget();
            budget.setUser(user);
            budget.setAmount(new BigDecimal("1000"));
            budget.setYear(2025);
            budget.setMonth(3);
            budget.setCategories(Set.of(category));
            budget.setDeletedAt(Instant.now());
            budgetRepository.save(budget);

            List<Budget> result = budgetRepository.findByUserAndYearAndMonthAndCategory(
                    user.getId(), 2025, 3, category.getId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdAndUser_IdAndDeletedAtIsNull")
    class FindByIdAndUserAndDeletedAtIsNull {

        @Test
        @DisplayName("returns budget when owned and not deleted")
        void returnsWhenOwnedAndActive() {
            Budget budget = new Budget();
            budget.setUser(user);
            budget.setAmount(new BigDecimal("500"));
            budget.setYear(2025);
            budget.setMonth(1);
            budget.setCategories(new HashSet<>());
            budget.setDeletedAt(null);
            budget = budgetRepository.save(budget);

            Optional<Budget> found = budgetRepository.findByIdAndUser_IdAndDeletedAtIsNull(
                    budget.getId(), user.getId());

            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("returns empty when budget is soft-deleted")
        void returnsEmptyWhenDeleted() {
            Budget budget = new Budget();
            budget.setUser(user);
            budget.setAmount(new BigDecimal("500"));
            budget.setYear(2025);
            budget.setMonth(1);
            budget.setCategories(new HashSet<>());
            budget.setDeletedAt(Instant.now());
            budget = budgetRepository.save(budget);

            Optional<Budget> found = budgetRepository.findByIdAndUser_IdAndDeletedAtIsNull(
                    budget.getId(), user.getId());

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("returns empty when wrong user")
        void returnsEmptyWhenWrongUser() {
            User otherUser = new User();
            otherUser.setEmail("other-" + UUID.randomUUID() + "@test.com");
            otherUser.setPassword("hash");
            otherUser.setRole(Role.USER);
            otherUser = userRepository.save(otherUser);

            Budget budget = new Budget();
            budget.setUser(user);
            budget.setAmount(new BigDecimal("500"));
            budget.setYear(2025);
            budget.setMonth(1);
            budget.setCategories(new HashSet<>());
            budget.setDeletedAt(null);
            budget = budgetRepository.save(budget);

            Optional<Budget> found = budgetRepository.findByIdAndUser_IdAndDeletedAtIsNull(
                    budget.getId(), otherUser.getId());

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUser_IdAndDeletedAtIsNullOrderByYearDescMonthDesc")
    class FindByUserAndDeletedAtIsNull {

        @Test
        @DisplayName("returns only non-deleted budgets for user")
        void excludesDeletedAndFiltersByOwnership() {
            Budget active1 = saveBudget(user, 2025, 1, null);
            Budget active2 = saveBudget(user, 2025, 2, null);
            saveBudgetDeleted(user, 2025, 3);

            List<Budget> result = budgetRepository.findByUser_IdAndDeletedAtIsNullOrderByYearDescMonthDesc(user.getId());

            assertThat(result).hasSize(2);
        }
    }

    private Budget saveBudget(User u, int year, Integer month, Instant deletedAt) {
        Budget b = new Budget();
        b.setUser(u);
        b.setAmount(new BigDecimal("100"));
        b.setYear(year);
        b.setMonth(month);
        b.setCategories(new HashSet<>());
        b.setDeletedAt(deletedAt);
        return budgetRepository.save(b);
    }

    private Budget saveBudgetDeleted(User u, int year, int month) {
        Budget b = new Budget();
        b.setUser(u);
        b.setAmount(new BigDecimal("100"));
        b.setYear(year);
        b.setMonth(month);
        b.setCategories(new HashSet<>());
        b.setDeletedAt(Instant.now());
        return budgetRepository.save(b);
    }
}
