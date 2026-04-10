package com.interview.exception;

import com.interview.model.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler for all REST controllers.
 *
 * <p>Catches validation errors, business exceptions, and unexpected errors,
 * returning structured {@link ErrorResponse} JSON bodies with appropriate HTTP status codes.
 * Includes a catch-all handler that logs the full stack trace but never exposes
 * internal details to the client.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles bean validation errors from {@code @Valid @RequestBody} parameters.
     *
     * @param ex the validation exception containing field-level errors
     * @return a 400 Bad Request response with field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Validation failed", fieldErrors));
    }

    /**
     * Handles constraint violations from {@code @Validated} method parameters
     * (e.g., {@code @RequestParam} with {@code @NotBlank} or {@code @Size}).
     *
     * @param ex the constraint violation exception
     * @return a 400 Bad Request response with parameter error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations()
                .forEach(violation -> {
                    String field = violation.getPropertyPath().toString();
                    // Extract the parameter name (last segment of the path)
                    if (field.contains(".")) {
                        field = field.substring(field.lastIndexOf('.') + 1);
                    }
                    fieldErrors.put(field, violation.getMessage());
                });

        log.warn("Constraint violation: {}", fieldErrors);
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Validation failed", fieldErrors));
    }

    /**
     * Handles malformed or unreadable request bodies (e.g., invalid JSON or enum values).
     *
     * @param ex the message not readable exception
     * @return a 400 Bad Request response with a descriptive error message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFormat(HttpMessageNotReadableException ex) {
        log.warn("Invalid request body: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Invalid request body: " + ex.getMostSpecificCause().getMessage()));
    }

    /**
     * Handles requests for resources that do not exist.
     *
     * @param ex the resource not found exception
     * @return a 404 Not Found response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    /**
     * Handles attempts to create or update a resource that would violate a uniqueness constraint.
     *
     * @param ex the duplicate resource exception
     * @return a 409 Conflict response
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /**
     * Handles attempts to self-assign a task that is already assigned to another employee.
     *
     * @param ex the task already assigned exception
     * @return a 409 Conflict response
     */
    @ExceptionHandler(TaskAlreadyAssignedException.class)
    public ResponseEntity<ErrorResponse> handleTaskAlreadyAssigned(TaskAlreadyAssignedException ex) {
        log.warn("Task already assigned: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /**
     * Handles illegal state errors (e.g., attempting to update a task not assigned to the user).
     *
     * @param ex the illegal state exception
     * @return a 403 Forbidden response
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    /**
     * Handles method-level authorization denials from {@code @PreAuthorize} checks.
     *
     * @param ex the authorization denied exception
     * @return a 403 Forbidden response
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.warn("Authorization denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Access denied — insufficient permissions"));
    }

    /**
     * Handles authentication failures (e.g., bad credentials during login).
     *
     * @param ex the authentication exception
     * @return a 401 Unauthorized response
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Invalid username or password"));
    }

    /**
     * Catch-all handler for any unexpected exceptions not matched by other handlers.
     *
     * <p>Logs the full stack trace but returns a generic error message
     * to avoid leaking internal details to clients.</p>
     *
     * @param ex the unexpected exception
     * @return a 500 Internal Server Error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred"));
    }
}
