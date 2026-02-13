package com.spendwise.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}
