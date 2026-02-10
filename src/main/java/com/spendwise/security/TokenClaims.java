package com.spendwise.security;

import java.util.Collection;
import java.util.UUID;

/**
 * Extracted claims from a validated JWT.
 */
public record TokenClaims(String username, UUID userId, Collection<String> roles) {
}
