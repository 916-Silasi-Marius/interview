package com.interview.exception;

import com.interview.model.dto.ErrorResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MeterRegistry meterRegistry;

    private Counter validationErrorCounter;
    private Counter notFoundErrorCounter;
    private Counter duplicateErrorCounter;
    private Counter conflictErrorCounter;
    private Counter authErrorCounter;
    private Counter forbiddenErrorCounter;
    private Counter unexpectedErrorCounter;

    @PostConstruct
    void initCounters() {
        validationErrorCounter = Counter.builder("app.errors")
                .tag("type", "validation").tag("status", "400")
                .description("Validation errors").register(meterRegistry);
        notFoundErrorCounter = Counter.builder("app.errors")
                .tag("type", "not_found").tag("status", "404")
                .description("Resource not found errors").register(meterRegistry);
        duplicateErrorCounter = Counter.builder("app.errors")
                .tag("type", "duplicate").tag("status", "409")
                .description("Duplicate resource errors").register(meterRegistry);
        conflictErrorCounter = Counter.builder("app.errors")
                .tag("type", "conflict").tag("status", "409")
                .description("Concurrent modification errors").register(meterRegistry);
        authErrorCounter = Counter.builder("app.errors")
                .tag("type", "authentication").tag("status", "401")
                .description("Authentication failures").register(meterRegistry);
        forbiddenErrorCounter = Counter.builder("app.errors")
                .tag("type", "forbidden").tag("status", "403")
                .description("Authorization denied errors").register(meterRegistry);
        unexpectedErrorCounter = Counter.builder("app.errors")
                .tag("type", "unexpected").tag("status", "500")
                .description("Unexpected server errors").register(meterRegistry);
    }

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

        validationErrorCounter.increment();
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

        validationErrorCounter.increment();
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
        validationErrorCounter.increment();
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
        notFoundErrorCounter.increment();
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
        duplicateErrorCounter.increment();
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /**
     * Handles optimistic locking failures when a resource was concurrently
     * modified by another request.
     *
     * @param ex the concurrent modification exception
     * @return a 409 Conflict response advising the client to retry
     */
    @ExceptionHandler(ConcurrentModificationException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentModification(ConcurrentModificationException ex) {
        conflictErrorCounter.increment();
        log.warn("Concurrent modification: {}", ex.getMessage());
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
        conflictErrorCounter.increment();
        log.warn("Task already assigned: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    /**
     * Handles attempts to update the status of a task not assigned to the requesting employee.
     *
     * @param ex the task not assigned exception
     * @return a 403 Forbidden response
     */
    @ExceptionHandler(TaskNotAssignedException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotAssigned(TaskNotAssignedException ex) {
        forbiddenErrorCounter.increment();
        log.warn("Task not assigned: {}", ex.getMessage());
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
        forbiddenErrorCounter.increment();
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
        authErrorCounter.increment();
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
        unexpectedErrorCounter.increment();
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred"));
    }
}
