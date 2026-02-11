package com.spendwise.service;

import com.spendwise.domain.entity.Role;
import com.spendwise.domain.entity.User;
import com.spendwise.dto.AuthResponse;
import com.spendwise.dto.LoginRequest;
import com.spendwise.dto.RefreshRequest;
import com.spendwise.dto.RegisterRequest;
import com.spendwise.exception.EmailAlreadyExistsException;
import com.spendwise.exception.InvalidCredentialsException;
import com.spendwise.exception.InvalidRefreshTokenException;
import com.spendwise.repository.UserRepository;
import com.spendwise.security.JwtUtil;
import com.spendwise.security.TokenClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Autowired
    private final UserRepository userRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already in use: " + req.email());
        }

        User user = new User();
        user.setEmail(req.email());
        user.setName(req.name());
        user.setPassword(passwordEncoder.encode(req.password())); //Encoded password will be saved
        user.setRole(Role.USER);
        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user);
    }


    //This method generates a new access token (and maybe new refresh token for next access token)
    public AuthResponse refresh(RefreshRequest req) {
        String token = req.refreshToken();
        if (token == null || token.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is required");
        }

        if (!jwtUtil.validateToken(token)) { //Validate JWT structure & signature
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        Optional<TokenClaims> claimsOpt = jwtUtil.extractClaims(token); //Extract claims: username, userId, roles, type (important here). If extraction fails → reject.
        if (claimsOpt.isEmpty()) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }

        TokenClaims claims = claimsOpt.get();  // Ensure it's actually a refresh token. Type refresh only or access.
        if (!REFRESH_TOKEN_TYPE.equals(claims.type())) {
            throw new InvalidRefreshTokenException("Token is not a refresh token");
        }

        User user = userRepository.findByEmail(claims.username())  // If user not found → reject.
                .orElseThrow(() -> new InvalidRefreshTokenException("User not found"));

        return buildAuthResponse(user);  //none of the above Generate new tokens
    }


    //This method creates the new access token + refresh token and sends them back to the client.
    private AuthResponse buildAuthResponse(User user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), user.getId(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), user.getId());
        return new AuthResponse(accessToken, refreshToken);
    }
}
