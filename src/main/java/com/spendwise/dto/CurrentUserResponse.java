package com.spendwise.dto;

import java.util.UUID;

public record CurrentUserResponse(UUID id, String email, String name, String role) {}
