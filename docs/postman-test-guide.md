# Postman Test Guide

Instructions for testing the SpendWise API with Postman.

---

## Prerequisites

- **Base URL**: `http://localhost:8080` (or your configured server port)
- Application must be running with database available

---

## 1. Register → Login → Access Protected Endpoint

### Step 1: Register

- **Method**: `POST`
- **URL**: `{{baseUrl}}/auth/register`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):

```json
{
  "email": "test@example.com",
  "password": "password123",
  "name": "Test User"
}
```

- **Expected**: 201 Created
- **Response body**: `{ "accessToken": "...", "refreshToken": "..." }`
- Save `accessToken` for the next request (e.g. copy to a Postman variable)

### Step 2: Login

- **Method**: `POST`
- **URL**: `{{baseUrl}}/auth/login`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):

```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

- **Expected**: 200 OK
- **Response body**: `{ "accessToken": "...", "refreshToken": "..." }`
- Save `accessToken` and `refreshToken` (e.g. to Postman environment variables)

### Step 3: Access Protected Endpoint

- **Method**: `GET`
- **URL**: `{{baseUrl}}/users/me`
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Bearer <accessToken>`
- **Expected**: 200 OK
- **Response body example**: `{ "id": "...", "email": "test@example.com", "name": "Test User", "role": "USER" }`

---

## 2. Token Refresh Flow

- **Method**: `POST`
- **URL**: `{{baseUrl}}/auth/refresh`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):

```json
{
  "refreshToken": "<paste refresh token here>"
}
```

- **Expected**: 200 OK
- **Response body**: `{ "accessToken": "...", "refreshToken": "..." }`
- Use the new `accessToken` for subsequent protected requests
- Optional: Save tokens to variables for chained requests

---

## 3. Example Authorization Header

**Format:**

```
Authorization: Bearer <access_token>
```

**Example (with placeholder):**

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIi...
```

**In Postman**: Add a header with Key `Authorization` and Value `Bearer ` plus the token value. Or use the "Authorization" tab, type "Bearer Token", and paste the token.

---

## 4. Postman Environment Variables (Optional)

- `baseUrl`: `http://localhost:8080`
- `accessToken`: set from login/register/refresh response
- `refreshToken`: set from login/register/refresh response

Reference in headers: `Authorization: Bearer {{accessToken}}`

---

## 5. Error Response Format

On validation or auth failures, responses follow:

```json
{
  "errorCode": "INVALID_CREDENTIALS",
  "message": "Invalid email or password",
  "timestamp": "2025-02-10T12:00:00Z"
}
```
