package com.spendwise.repository;

import com.spendwise.domain.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Expense entities.
 * <p>
 * <b>N+1 problem and JOIN FETCH (interview gold)</b>
 * <p>
 * <b>What is N+1?</b> When we load a list of expenses (1 query) and then access
 * a lazy-loaded association (e.g. {@code expense.getCategory()}) inside a loop,
 * Hibernate issues one extra query per expense. Result: 1 + N queries instead of 1.
 * <p>
 * <b>When it occurs:</b> With {@link com.spendwise.domain.entity.Expense#category}
 * mapped as {@code FetchType.LAZY}, any code that iterates over expenses and
 * calls {@code getCategory()} triggers N+1 (e.g. mapping to DTOs that need category id).
 * <p>
 * <b>JOIN FETCH solution:</b> Use JPQL {@code JOIN FETCH e.category} in a dedicated
 * query. Hibernate loads expenses and their categories in a single SQL query (with a JOIN),
 * so no lazy load is needed. Query count goes from N+1 to 1.
 * <p>
 * <b>LAZY vs EAGER:</b> We keep the default as LAZY on the entity. We do NOT switch
 * to EAGER globally. Instead we use JOIN FETCH only in repository methods where the
 * caller is known to need category data (e.g. list endpoints that return category id).
 * This keeps other use cases efficient and avoids loading categories when not needed.
 */
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Optional<Expense> findByIdAndDeletedAtIsNull(UUID id); //Find an expense by ID ,but only if it is not soft deleted

    List<Expense> findByUser_IdAndDeletedAtIsNullOrderByExpenseDateDesc(UUID userId);

    List<Expense> findByUser_IdAndCategory_IdAndDeletedAtIsNullOrderByExpenseDateDesc(UUID userId, UUID categoryId); //expense.user.id = ?AND expense.category.id = ?AND deleted_at IS NULLORDER BY expenseDate DESC

    List<Expense> findByUser_IdAndExpenseDateBetweenAndDeletedAtIsNullOrderByExpenseDateDesc(
            UUID userId, LocalDate start, LocalDate end); //expense.user.id = ?AND expenseDate BETWEEN start AND endAND deleted_at IS NULLORDER BY expenseDate DESC

    /**
     * Fetches all expenses for a user, excluding soft-deleted ones.
     * <p>
     * Why explicit JPQL is preferred over derived method names:
     * - More readable than long method names like findByUser_IdAndDeletedAtIsNullOrderByExpenseDateDesc
     * - Easier to maintain and modify query logic
     * - Consistent with other custom queries in the repository
     * - Can be easily extended with JOIN FETCH for performance optimization
     * <p>
     * Why JPQL instead of native SQL:
     * - Database portability (works across PostgreSQL, MySQL, H2, etc.)
     * - Type safety at compile time
     * - Entity-aware (uses entity names and relationships)
     */
    @Query("""
            SELECT e FROM Expense e
            WHERE e.user.id = :userId
              AND e.deletedAt IS NULL
            ORDER BY e.expenseDate DESC
            """)
    List<Expense> findAllByUserExcludingDeleted(@Param("userId") UUID userId);

    /**
     * Fetches all expenses for a user with their categories eagerly loaded, excluding soft-deleted ones.
     * <p>
     * JOIN FETCH benefits:
     * - Prevents N+1 query problem when accessing expense.getCategory()
     * - Loads category in the same query, avoiding lazy loading exceptions
     * - More efficient than separate queries for each expense's category
     * - Single database round-trip instead of N+1 queries
     * <p>
     * Why JPQL JOIN FETCH instead of native SQL:
     * - Database portability and type safety
     * - Entity-aware relationship handling
     * - Hibernate manages the fetch strategy automatically
     * <p>
     * Use this query when you know you'll need category data for all expenses,
     * as it's more efficient than lazy loading each category separately.
     */
    @Query("""
            SELECT e FROM Expense e
            JOIN FETCH e.category
            WHERE e.user.id = :userId
              AND e.deletedAt IS NULL
            ORDER BY e.expenseDate DESC
            """)
    List<Expense> findAllByUserWithCategoryExcludingDeleted(@Param("userId") UUID userId);

    /**
     * Fetches expenses for a user within a date range, with category loaded via JOIN FETCH.
     * <p>
     * Use this when you need expenses in a range and will access {@code expense.getCategory()}
     * (e.g. filtering by category or building DTOs with category id). Without JOIN FETCH,
     * that would cause N+1: one query here plus one per expense when category is accessed.
     * With JOIN FETCH, a single query returns expenses and their categories.
     * <p>
     * Entity fetch type stays LAZY; only this query eagerly fetches category.
     */
    @Query("""
            SELECT e FROM Expense e
            JOIN FETCH e.category
            WHERE e.user.id = :userId
              AND e.expenseDate BETWEEN :start AND :end
              AND e.deletedAt IS NULL
            ORDER BY e.expenseDate DESC
            """)
    List<Expense> findByUserAndDateRangeWithCategory(
            @Param("userId") UUID userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    /**
     * Fetches expenses for a user and category within a specific month, with category eagerly loaded.
     * <p>
     * JOIN FETCH ensures category is loaded eagerly, preventing N+1 queries.
     * <p>
     * JPQL date functions (YEAR, MONTH) are database-agnostic:
     * - Hibernate translates them to appropriate SQL for the target database
     * - More portable than native SQL date extraction functions
     * - Type-safe and validated at compile time
     * <p>
     * Explicit query is clearer than derived method names and allows for JOIN FETCH optimization.
     */
    @Query("""
            SELECT e FROM Expense e
            JOIN FETCH e.category
            WHERE e.user.id = :userId
              AND e.category.id = :categoryId
              AND YEAR(e.expenseDate) = :year
              AND MONTH(e.expenseDate) = :month
              AND e.deletedAt IS NULL
            ORDER BY e.expenseDate DESC
            """)
    List<Expense> findByUserAndCategoryAndMonthWithCategory(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("year") int year,
            @Param("month") int month);

    /**
     * Sums expense amounts for a specific user, category, and date range.
     * <p>
     * Why JPQL aggregation is preferred:
     * - Database-level calculation avoids loading all expense rows into memory
     * - More efficient than fetching all expenses and summing in Java
     * - Reduces network traffic and memory usage
     * <p>
     * Why JPQL instead of native SQL:
     * - Database-agnostic: works across PostgreSQL, MySQL, H2, etc.
     * - Type-safe: uses entity names and relationships, not table/column names
     * - Hibernate/JPA can optimize queries automatically
     * - Compile-time validation of entity references
     * <p>
     * COALESCE ensures we return 0 when no expenses match (instead of NULL),
     * making the result safe for BigDecimal arithmetic.
     */
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.user.id = :userId
              AND e.category.id = :categoryId
              AND e.deletedAt IS NULL
              AND e.expenseDate BETWEEN :start AND :end
            """)
    BigDecimal sumAmountByUserAndCategoryAndDateRange(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}

