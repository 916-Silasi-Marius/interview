package com.interview.exception;

/**
 * Exception thrown when attempting to create or update a resource
 * that would violate a uniqueness constraint (e.g., duplicate username or email).
 *
 * <p>Handled by {@link GlobalExceptionHandler} to return a 409 Conflict response.</p>
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
