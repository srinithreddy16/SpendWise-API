package com.spendwise.controller;

import com.spendwise.dto.request.CreateBudgetRequest;
import com.spendwise.dto.request.UpdateBudgetRequest;
import com.spendwise.dto.response.BudgetResponse;
import com.spendwise.dto.response.UserResponse;
import com.spendwise.service.BudgetService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final UserService userService;

    public BudgetController(BudgetService budgetService, UserService userService) {
        this.budgetService = budgetService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody CreateBudgetRequest request) {
        UserResponse currentUser = getCurrentUserOrThrow();
        BudgetResponse response = budgetService.createBudget(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> getBudget(@PathVariable UUID id) {
        UserResponse currentUser = getCurrentUserOrThrow();
        BudgetResponse response = budgetService.getBudget(currentUser.id(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> listBudgets(
            @RequestParam int year,
            @RequestParam int month) {
        UserResponse currentUser = getCurrentUserOrThrow();
        List<BudgetResponse> response = budgetService.getBudgetsForUser(currentUser.id(), year, month);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBudgetRequest request) {
        UserResponse currentUser = getCurrentUserOrThrow();
        BudgetResponse response = budgetService.updateBudget(currentUser.id(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID id) {
        UserResponse currentUser = getCurrentUserOrThrow();
        budgetService.deleteBudget(currentUser.id(), id);
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
