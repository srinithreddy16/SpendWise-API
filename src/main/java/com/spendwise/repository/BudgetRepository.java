package com.spendwise.repository;

import com.spendwise.domain.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Optional<Budget> findByIdAndDeletedAtIsNull(UUID id);

    List<Budget> findByUser_IdAndDeletedAtIsNullOrderByYearDescMonthDesc(UUID userId);

    List<Budget> findByUser_IdAndYearAndDeletedAtIsNullOrderByMonthAsc(UUID userId, int year);

    boolean existsByUser_IdAndYearAndMonthAndDeletedAtIsNull(UUID userId, int year, Integer month);

    boolean existsByUser_IdAndYearAndMonthIsNullAndDeletedAtIsNull(UUID userId, int year);

    @Query("SELECT COUNT(b) FROM Budget b WHERE b.user.id = :userId AND b.year = :year AND b.month = :month AND b.deletedAt IS NULL AND b.id <> :excludeId")
    long countByUserAndYearAndMonthExcludingId(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("month") Integer month,
            @Param("excludeId") UUID excludeId);

    @Query("SELECT COUNT(b) FROM Budget b WHERE b.user.id = :userId AND b.year = :year AND b.month IS NULL AND b.deletedAt IS NULL AND b.id <> :excludeId")
    long countByUserAndYearAndMonthNullExcludingId(
            @Param("userId") UUID userId,
            @Param("year") int year,
            @Param("excludeId") UUID excludeId);
}
