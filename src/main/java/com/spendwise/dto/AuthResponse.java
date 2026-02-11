package com.spendwise.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}
