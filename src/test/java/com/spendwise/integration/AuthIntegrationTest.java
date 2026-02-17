package com.spendwise.integration;

import com.spendwise.dto.error.ErrorResponse;
import com.spendwise.dto.request.LoginRequest;
import com.spendwise.dto.request.RegisterRequest;
import com.spendwise.dto.response.AuthResponse;
import com.spendwise.dto.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Auth flow integration")
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("returns JWT on success")
        void returnsJwtOnRegister() {
            RegisterRequest request = new RegisterRequest(
                    "register-test@example.com",
                    "password123",
                    "Test User"
            );

            ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                    baseUrl() + "/auth/register",
                    request,
                    AuthResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();
            assertThat(response.getBody().refreshToken()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns JWT with registered credentials")
        void returnsJwtOnLogin() {
            RegisterRequest registerRequest = new RegisterRequest(
                    "login-test@example.com",
                    "password123",
                    "Login Test"
            );
            restTemplate.postForEntity(baseUrl() + "/auth/register", registerRequest, AuthResponse.class);

            LoginRequest loginRequest = new LoginRequest("login-test@example.com", "password123");

            ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                    baseUrl() + "/auth/login",
                    loginRequest,
                    AuthResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().accessToken()).isNotBlank();
            assertThat(response.getBody().refreshToken()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("protected endpoint")
    class ProtectedEndpoint {

        @Test
        @DisplayName("returns 200 when request has valid JWT")
        void returns200WithValidToken() {
            RegisterRequest registerRequest = new RegisterRequest(
                    "me-test@example.com",
                    "password123",
                    "Me Test"
            );
            ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
                    baseUrl() + "/auth/register",
                    registerRequest,
                    AuthResponse.class
            );
            String accessToken = authResponse.getBody().accessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    baseUrl() + "/users/me",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    UserResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().email()).isEqualTo("me-test@example.com");
        }

        @Test
        @DisplayName("returns 401 when request has no token")
        void returns401WithoutToken() {
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    baseUrl() + "/users/me",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    ErrorResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("UNAUTHORIZED");
            assertThat(response.getBody().message()).isEqualTo("Authentication required");
        }

        @Test
        @DisplayName("returns 401 when request has invalid token")
        void returns401WithInvalidToken() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer invalid-token");

            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    baseUrl() + "/users/me",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    ErrorResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode())
                    .isIn("UNAUTHORIZED", "INVALID_TOKEN");
        }
    }
}
