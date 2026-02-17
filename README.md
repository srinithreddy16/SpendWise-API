# SpendWise API

## Project overview

SpendWise API is a Spring Boot?based backend for managing personal expenses and budgets.
It focuses on a clean domain model (users, categories, expenses, budgets, audit logs) and robust persistence against PostgreSQL using Flyway migrations.
This repository contains the REST API backend only (no UI); controllers and business endpoints will be layered on top of the existing domain model.

## Tech stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.5.x
- **Web**: Spring Web (REST)
- **Persistence**: Spring Data JPA with PostgreSQL
- **Migrations**: Flyway (see `src/main/resources/db/migration`)
- **Validation**: Bean Validation (`spring-boot-starter-validation`, `jakarta.validation.*`)
- **Security**: Spring Security (dependency present, configuration to be added)
- **Monitoring**: Spring Boot Actuator (health/info)
- **Utilities**: Lombok for boilerplate reduction

## Domain model (high level)

Core entities live under `src/main/java/com/spendwise/domain/entity`:

- **User**
  - Represents an account owner, uniquely identified by email.
  - Owns expenses, categories, and budgets.

- **Category**
  - User-specific label used to group expenses (e.g. Food, Transport).
  - Each category belongs to exactly one user.
  - Category name is unique per user.

- **Expense**
  - Single expense record with `amount`, `description`, and `expenseDate`.
  - Belongs to one user and one category.

- **Budget**
  - Spending limit for a given period.
  - Belongs to one user.
  - Can apply to multiple categories via a many-to-many relation (join table `budget_categories`).
  - Has a positive `amount` and a period defined by `year` and optional `month` (null month can represent a yearly budget).

- **ExpenseAuditLog**
  - Immutable audit entry recording changes to an expense (e.g. CREATED, UPDATED, DELETED).
  - References a single expense and can store optional textual `details` (snapshot/diff).

Relationship summary:

- One **User** ? many **Expenses**, **Categories**, **Budgets**
- One **Expense** ? many **ExpenseAuditLog** entries
- One **Budget** ? many **Categories** (many-to-many via `budget_categories`)

## Running locally (dev profile)

### Prerequisites

- JDK 17+
- Maven 3.x
- PostgreSQL running locally (e.g. on `localhost:5432`)

### Database setup

1. Create a PostgreSQL database for development, for example:
   - Database: `spendwise_dev`
2. Create (or reuse) a PostgreSQL user with access to this database (for example, `spendwise_user`).
3. On application startup, Flyway will apply migrations from `src/main/resources/db/migration`, including `V1__initial_schema.sql`, to create all tables and constraints.

### Configuration (dev profile)

The `dev` profile is configured in `src/main/resources/application-dev.yml`:

- `spring.datasource.url=jdbc:postgresql://localhost:5432/spendwise_dev`
- `spring.datasource.username=${DB_USERNAME:spendwise_user}`
- `spring.datasource.password=${DB_PASSWORD}`

Set the following environment variables before running (or configure them in your IDE run configuration):

- `DB_USERNAME` ? database username (defaults to `spendwise_user` if not set)
- `DB_PASSWORD` ? database password (required, no default)

### Start the application

Using Maven (recommended for development):

```bash
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

Using the built JAR (after `mvn clean package`):

```bash
SPRING_PROFILES_ACTIVE=dev java -jar target/spendwise-api-0.0.1-SNAPSHOT.jar
```

### Profile switching

Activate a profile via environment variable or JVM argument:

```bash
# Environment variable
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# JVM argument
java -jar target/spendwise-api-0.0.1-SNAPSHOT.jar -Dspring.profiles.active=dev
```

**Profile differences:**

| Profile | Use case | Datasource | Logging |
|---------|----------|------------|---------|
| `dev` | Local development | Local PostgreSQL; URL/username have defaults | DEBUG for application, pretty console |
| `prod` | Production deployment | Env vars only (DB_URL, DB_USERNAME, DB_PASSWORD) | INFO default, WARN for Spring Security |

Flyway is enabled in both profiles. No credentials are hardcoded; use environment variables for all secrets.

With the application running on the dev profile:

- Health endpoint: `/actuator/health`
- Info endpoint: `/actuator/info`

## Testing

### Strategy overview

Tests are organized by type under `src/test/java/com/spendwise/`:

- **Unit tests** (`unit/service`): Mockito-based tests that isolate service logic from external dependencies.
- **Integration tests** (`integration`): Full application tests that hit REST endpoints with a real database.
- **Repository tests** (`repository`): JPA layer tests that validate persistence and queries.

Unit tests use mocks; integration and repository tests use a real PostgreSQL database via Testcontainers.

### How to run tests

```bash
mvn test
```

Or with a clean build:

```bash
mvn clean test
```

**Prerequisite:** Docker must be running (required for Testcontainers).

### Unit vs Integration tests

- **Unit tests**: Exercise service logic in isolation with mocked dependencies (`@ExtendWith(MockitoExtension.class)`). No Spring context, no database. Fast and focused on business rules.
- **Integration tests**: Start the full application (`@SpringBootTest`) and hit REST endpoints via `TestRestTemplate`. Use a real PostgreSQL container. Validate end-to-end flows (auth, HTTP status, response body).
- **Repository tests**: Use `@DataJpaTest` against a Testcontainers PostgreSQL instance. Validate JPA queries and persistence behavior without starting the whole application.

### Why Testcontainers

- Uses the same PostgreSQL engine as production, avoiding dialect or SQL differences from in-memory databases (e.g. H2).
- No manual database setup or shared test database; each test class spins up its own container.
- Tests run with a single command (`mvn test`) with no external database configuration.

> Note: API endpoints for business operations (expense creation, budgeting, etc.) are **not** documented yet and will be added once controllers and services are implemented.

