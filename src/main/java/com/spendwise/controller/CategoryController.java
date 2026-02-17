package com.spendwise.controller;

import com.spendwise.dto.request.CreateCategoryRequest;
import com.spendwise.dto.request.UpdateCategoryRequest;
import com.spendwise.dto.response.CategoryResponse;
import com.spendwise.dto.response.UserResponse;
import com.spendwise.service.CategoryService;
import com.spendwise.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    public CategoryController(CategoryService categoryService, UserService userService) {
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        UserResponse currentUser = getCurrentUserOrThrow();
        CategoryResponse response = categoryService.createCategory(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        UserResponse currentUser = getCurrentUserOrThrow();
        List<CategoryResponse> response = categoryService.listCategories(currentUser.id());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID id) {
        UserResponse currentUser = getCurrentUserOrThrow();
        CategoryResponse response = categoryService.getCategory(currentUser.id(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        UserResponse currentUser = getCurrentUserOrThrow();
        CategoryResponse response = categoryService.updateCategory(currentUser.id(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        UserResponse currentUser = getCurrentUserOrThrow();
        categoryService.deleteCategory(currentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    private UserResponse getCurrentUserOrThrow() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            throw new org.springframework.security.authentication.BadCredentialsException("Authentication required");
        }
        return userService.getCurrentUser(userDetails.getUsername());
    }
}
