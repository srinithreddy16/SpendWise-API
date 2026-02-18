package com.spendwise.security;

import com.spendwise.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT utility for generating and validating access and refresh tokens.
 * Handles expiration, signature verification, and claim extraction.
 * No HTTP or controller references.
 */
@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = buildSigningKey(properties.secret());
    }


    //This converts your secret string into a strong, safe key that JWT can use to sign tokens
    private static SecretKey buildSigningKey(String secret) {  //what ever is given during production
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8); //string gets converted into bytes
        if (keyBytes.length < 32) {
            try {
                keyBytes = MessageDigest.getInstance("SHA-256").digest(keyBytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 not available", e);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);  //hmacShaKey algorithm convert bytes to a key used for signing
    }

    /**
     * Generates a short-lived access token.
     */
    public String generateAccessToken(String username, UUID userId, java.util.Collection<String> roles) {
        Date now = new Date();
        Date expiry = Date.from(now.toInstant().plus(properties.accessTokenExpiration()));

        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId.toString())
                .claim(CLAIM_ROLES, roles != null ? List.copyOf(roles) : List.of())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)  // signature at bottom
                .compact();
    }

    /**
     * Generates a longer-lived refresh token.
     */
    public String generateRefreshToken(String username, UUID userId) {
        Date now = new Date();
        Date expiry = Date.from(now.toInstant().plus(properties.refreshTokenExpiration()));

        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId.toString())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates the token signature and expiration.
     * Returns false for expired, malformed, or invalid tokens.
     * PARSER DECODES THE TOKEN
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser() //The JWT parser is a gatekeeper that fully validates the token’s structure, signature, and expiry before letting it pass by breaking it and decoding.
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Returns the validation error code for an invalid token.
     * Returns empty if the token is valid.
     */
    public Optional<String> getValidationErrorCode(String token) {
        if (token == null || token.isBlank()) {
            return Optional.of("INVALID_TOKEN");
        }
        try {
            parseClaims(token);
            return Optional.empty();
        } catch (ExpiredJwtException e) {
            return Optional.of("EXPIRED_TOKEN");
        } catch (JwtException e) {
            return Optional.of("INVALID_TOKEN");
        }
    }

    /**
     * CLAIMS EXTRACTION
     *1. Extracts the username (sub claim) from the token.
     * Does not validate the token; use {@link #validateToken(String)} first.
     **** To extract the username (or user ID) that was stored inside the JWT so the application knows which user is making the request.
     */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     *2. Extracts the user id from the token.
     * Does not validate the token; use {@link #validateToken(String)} first.
     */
    public UUID getUserId(String token) {
        String userIdStr = parseClaims(token).get(CLAIM_USER_ID, String.class);
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     *3. Extracts the roles from the token.
     * Returns empty list if no roles claim or for refresh tokens.
     * Does not validate the token; use {@link #validateToken(String)} first.
     */
    public java.util.Collection<String> getRoles(String token) {
        return extractRolesFromClaims(parseClaims(token));
    }

    /**
     *4. Validates the token and extracts all claims in one call.
     * Returns empty if the token is invalid or expired.
     */
    public Optional<TokenClaims> extractClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = parseClaims(token);
            String username = claims.getSubject();
            String userIdStr = claims.get(CLAIM_USER_ID, String.class);
            UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
            java.util.Collection<String> roles = extractRolesFromClaims(claims);
            String type = claims.get(CLAIM_TYPE, String.class);
            if (type == null) {
                type = TYPE_ACCESS;
            }
            return Optional.of(new TokenClaims(username, userId, roles, type));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    //This method safely reads the roles from the JWT, makes sure they are strings, and returns them as a list.
    //If roles are missing or in the wrong format, it returns an empty list instead of crashing.
    private java.util.Collection<String> extractRolesFromClaims(Claims claims) {
        Object rolesObj = claims.get(CLAIM_ROLES);
        if (rolesObj instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return Collections.emptyList();
    }


    // This method validates the JWT using the signing key and returns the decoded claims so the application can safely read user data. Centralizes JWT parsing in one place
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
