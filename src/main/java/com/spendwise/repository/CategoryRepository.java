package com.spendwise.repository;

import com.spendwise.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {  //It only provides universal CRUD operations.

    Optional<Category> findByIdAndUser_Id(UUID id, UUID userId); //These are custom methods, because we knwo how the DataBase will be

    List<Category> findByUser_Id(UUID userId);
}
