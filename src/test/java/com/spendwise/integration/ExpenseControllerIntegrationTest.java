package com.spendwise.integration;

import com.spendwise.domain.entity.Category;
import com.spendwise.domain.entity.Expense;
import com.spendwise.domain.entity.User;
import com.spendwise.dto.error.ErrorResponse;
import com.spendwise.dto.request.LoginRequest;
import com.spendwise.dto.request.RegisterRequest;
import com.spendwise.dto.response.AuthResponse;
import com.spendwise.dto.response.ExpenseResponse;
import com.spendwise.dto.response.PageResponse;
import com.spendwise.repository.CategoryRepository;
import com.spendwise.repository.ExpenseRepository;
import com.spendwise.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("GET /expenses integration")
class ExpenseControllerIntegrationTest {

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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private String userAEmail;
    private String userBEmail;
    private String accessTokenA;
    private String accessTokenB;

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        userAEmail = "expenses-test-a-" + suffix + "@example.com";
        userBEmail = "expenses-test-b-" + suffix + "@example.com";

        restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                new RegisterRequest(userAEmail, "password123", "User A"),
                AuthResponse.class
        );
        restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                new RegisterRequest(userBEmail, "password123", "User B"),
                AuthResponse.class
        );

        accessTokenA = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                new LoginRequest(userAEmail, "password123"),
                AuthResponse.class
        ).getBody().accessToken();
        accessTokenB = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                new LoginRequest(userBEmail, "password123"),
                AuthResponse.class
        ).getBody().accessToken();

        User userA = userRepository.findByEmail(userAEmail).orElseThrow();
        Category category = new Category();
        category.setName("Food");
        category.setUser(userA);
        category = categoryRepository.save(category);

        Expense expense = new Expense();
        expense.setUser(userA);
        expense.setCategory(category);
        expense.setAmount(new BigDecimal("50.00"));
        expense.setDescription("Test expense");
        expense.setExpenseDate(LocalDate.of(2025, 3, 15));
        expenseRepository.save(expense);
    }

    @Nested
    @DisplayName("valid JWT returns 200")
    class ValidJwt {

        @Test
        @DisplayName("returns 200 with content and pagination metadata")
        void returns200WithContentAndPagination() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessTokenA);

            ResponseEntity<PageResponse<ExpenseResponse>> response = restTemplate.exchange(
                    baseUrl() + "/expenses",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<PageResponse<ExpenseResponse>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            PageResponse<ExpenseResponse> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.content()).hasSize(1);
            assertThat(body.content().get(0).amount()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(body.content().get(0).description()).isEqualTo("Test expense");
            assertThat(body.page()).isEqualTo(0);
            assertThat(body.size()).isEqualTo(10);
            assertThat(body.totalElements()).isEqualTo(1);
            assertThat(body.totalPages()).isEqualTo(1);
            assertThat(body.first()).isTrue();
            assertThat(body.last()).isTrue();
            assertThat(body.numberOfElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("no JWT returns 401")
    class NoJwt {

        @Test
        @DisplayName("returns 401 when request has no token")
        void returns401WhenNoToken() {
            ResponseEntity<ErrorResponse> response = restTemplate.exchange(
                    baseUrl() + "/expenses",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    ErrorResponse.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().errorCode()).isEqualTo("UNAUTHORIZED");
        }
    }

    @Nested
    @DisplayName("ownership filtering")
    class OwnershipFiltering {

        @Test
        @DisplayName("user B does not see user A expenses")
        void userBDoesNotSeeUserAExpenses() {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessTokenB);

            ResponseEntity<PageResponse<ExpenseResponse>> response = restTemplate.exchange(
                    baseUrl() + "/expenses",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<PageResponse<ExpenseResponse>>() {}
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            PageResponse<ExpenseResponse> body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.content()).isEmpty();
            assertThat(body.totalElements()).isEqualTo(0);
            assertThat(body.numberOfElements()).isEqualTo(0);
            assertThat(body.page()).isEqualTo(0);
            assertThat(body.size()).isEqualTo(10);
            assertThat(body.totalPages()).isEqualTo(0);
        }
    }
}
