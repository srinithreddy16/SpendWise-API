package com.spendwise.repository;

import com.spendwise.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByIdAndUser_Id(UUID id, UUID userId);

    List<Category> findByUser_Id(UUID userId);

    boolean existsByUser_IdAndName(UUID userId, String name);

    boolean existsByUser_IdAndNameAndIdNot(UUID userId, String name, UUID excludeId);
}
