package com.interview.exception;

/**
 * Exception thrown when a resource was concurrently modified by another request,
 * causing an optimistic locking failure.
 *
 * <p>Handled by {@link GlobalExceptionHandler} to return a 409 Conflict response
 * advising the client to retry the operation.</p>
 */
public class ConcurrentModificationException extends RuntimeException {

    public ConcurrentModificationException(String message) {
        super(message);
    }
}
