package com.spendwise.dto.response;

import java.util.UUID;

/**
 * Response DTO for user data.
 * Exposes only fields needed by API clients. ID is included for user identification.
 */
public record UserResponse(
        UUID id,
        String email,
        String name,
        String role
) {}
