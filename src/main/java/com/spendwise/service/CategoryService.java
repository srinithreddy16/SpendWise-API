package com.spendwise.service;

import com.spendwise.domain.entity.Category;
import com.spendwise.dto.request.CreateCategoryRequest;
import com.spendwise.dto.request.UpdateCategoryRequest;
import com.spendwise.dto.response.CategoryResponse;
import com.spendwise.exception.ResourceNotFoundException;
import com.spendwise.exception.ValidationException;
import com.spendwise.repository.BudgetRepository;
import com.spendwise.repository.CategoryRepository;
import com.spendwise.repository.ExpenseRepository;
import com.spendwise.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           UserRepository userRepository,
                           ExpenseRepository expenseRepository,
                           BudgetRepository budgetRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }

    @Transactional
    public CategoryResponse createCategory(UUID currentUserId, CreateCategoryRequest request) {
        loadUser(currentUserId);
        if (categoryRepository.existsByUser_IdAndName(currentUserId, request.name())) {
            throw new ValidationException("A category with this name already exists");
        }
        Category category = new Category();
        category.setName(request.name().trim());
        category.setUser(userRepository.getReferenceById(currentUserId));
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(UUID currentUserId) {
        loadUser(currentUserId);
        return categoryRepository.findByUser_Id(currentUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID currentUserId, UUID categoryId) {
        loadUser(currentUserId);
        Category category = categoryRepository.findByIdAndUser_Id(categoryId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID currentUserId, UUID categoryId, UpdateCategoryRequest request) {
        loadUser(currentUserId);
        Category category = categoryRepository.findByIdAndUser_Id(categoryId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        String newName = request.name().trim();
        if (!newName.equals(category.getName()) && categoryRepository.existsByUser_IdAndNameAndIdNot(currentUserId, newName, categoryId)) {
            throw new ValidationException("A category with this name already exists");
        }
        category.setName(newName);
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    public void deleteCategory(UUID currentUserId, UUID categoryId) {
        loadUser(currentUserId);
        Category category = categoryRepository.findByIdAndUser_Id(categoryId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (expenseRepository.existsByCategory_IdAndDeletedIsFalse(categoryId)) {
            throw new ValidationException("Cannot delete category: it is used by expenses");
        }
        if (budgetRepository.existsByCategoryIdAndDeletedAtIsNull(categoryId)) {
            throw new ValidationException("Cannot delete category: it is used by budgets");
        }
        categoryRepository.delete(category);
    }

    private void loadUser(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
