package com.spendwise.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Owner of expenses, categories, and budgets.
 * A User can have many Expenses and define multiple Budgets (expressed via Expense.user and Budget.user).
 * Referenced unidirectionally by child entities (Expense, Category, Budget).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity implements UserDetails {  // Made the User class implement UserDetails, so Spring Security can use it directly:

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    /**
     * Hashed password (never store raw passwords).
     */
    @Column(name = "password_hash", nullable = false)
    private String password;

    /**
     * Application role, used for Spring Security authorities.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;



    // Below are UserDetails(Spring security Interface) methods. We are overriding here because user class implements UserDetails(interface)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        // Prefix with ROLE_ to follow Spring Security conventions
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
