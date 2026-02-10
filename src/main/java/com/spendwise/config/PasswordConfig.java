package com.spendwise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Password encoding configuration.
 *
 * Uses BCrypt as recommended by Spring Security for password hashing.
 * The strength (cost factor) is not hardcoded in application code; the default
 * provided by {@link BCryptPasswordEncoder} is used and can be tuned by
 * replacing this bean configuration if needed.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use BCrypt with library-provided default strength (no hardcoded cost value here)
        return new BCryptPasswordEncoder();
    }
}

