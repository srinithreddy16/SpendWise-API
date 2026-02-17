package com.spendwise.controller;

import com.spendwise.dto.request.CreateExpenseRequest;
import com.spendwise.dto.request.ExpenseListParams;
import com.spendwise.dto.request.UpdateExpenseRequest;
import com.spendwise.dto.response.ExpenseResponse;
import com.spendwise.dto.response.PageResponse;
import com.spendwise.dto.response.UserResponse;
import com.spendwise.service.ExpenseService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserService userService;

    public ExpenseController(ExpenseService expenseService, UserService userService) {
        this.expenseService = expenseService;
        this.userService = userService;
    }

    //This endpoint securely returns a paginated, filtered, and sorted list of expenses for the currently logged-in user.
    @GetMapping
    public ResponseEntity<PageResponse<ExpenseResponse>> listExpenses(
            //filtering
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            //pagination
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            //sorting
            @RequestParam(required = false) List<String> sort) {
        UserResponse currentUser = getCurrentUserOrThrow();
        ExpenseListParams params = ExpenseListParams.of(categoryId, fromDate, toDate, minAmount, maxAmount);
        PageResponse<ExpenseResponse> response = expenseService.listExpenses(currentUser.id(), params, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpense(@PathVariable UUID id) {
        UserResponse currentUser = getCurrentUserOrThrow();
        ExpenseResponse response = expenseService.getExpense(currentUser.id(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody CreateExpenseRequest request) {
        UserResponse currentUser = getCurrentUserOrThrow();
        ExpenseResponse response = expenseService.createExpense(currentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExpenseRequest request) {
        UserResponse currentUser = getCurrentUserOrThrow();
        ExpenseResponse response = expenseService.updateExpense(currentUser.id(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID id) {
        UserResponse currentUser = getCurrentUserOrThrow();
        expenseService.deleteExpense(currentUser.id(), id);
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
