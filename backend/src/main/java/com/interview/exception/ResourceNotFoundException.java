package com.interview.exception;

/**
 * Exception thrown when a requested resource is not found in the database.
 *
 * <p>Handled by {@link GlobalExceptionHandler} to return a 404 response.</p>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
