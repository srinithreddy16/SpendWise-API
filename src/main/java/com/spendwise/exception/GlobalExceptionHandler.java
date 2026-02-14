package com.spendwise.exception;

import com.spendwise.dto.error.ErrorResponse;
import com.spendwise.dto.error.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        HttpStatus status = toHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(ex.getErrorCode(), path));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ErrorCode.EMAIL_ALREADY_EXISTS, request.getRequestURI()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ErrorCode.INVALID_CREDENTIALS, request.getRequestURI()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ErrorCode.INVALID_REFRESH_TOKEN, request.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ErrorCode.ACCESS_DENIED, request.getRequestURI()));
    }

    @ExceptionHandler(com.spendwise.exception.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCustomAccessDenied(com.spendwise.exception.AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ErrorCode.ACCESS_DENIED, request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateBudgetException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateBudget(DuplicateBudgetException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ErrorCode.DUPLICATE_BUDGET, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, e -> sanitizeMessage(e.getDefaultMessage()), (a, b) -> a));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ValidationErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR.name(),
                        ErrorCode.VALIDATION_ERROR.getClientMessage(),
                        request.getRequestURI(),
                        fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(v -> v.getPropertyPath().toString(), v -> sanitizeMessage(v.getMessage()), (a, b) -> a));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ValidationErrorResponse.of(
                        ErrorCode.VALIDATION_ERROR.name(),
                        ErrorCode.VALIDATION_ERROR.getClientMessage(),
                        request.getRequestURI(),
                        fieldErrors));
    }

    private static String sanitizeMessage(String message) {
        return (message != null && !message.isBlank()) ? message : "Invalid value";
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, request.getRequestURI()));
    }

    private static HttpStatus toHttpStatus(ErrorCode code) {
        return switch (code) {
            case RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_CREDENTIALS, INVALID_REFRESH_TOKEN, INVALID_TOKEN -> HttpStatus.UNAUTHORIZED;
            case UNAUTHORIZED, ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case BUDGET_EXCEEDED, DUPLICATE_BUDGET, EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
