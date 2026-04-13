package com.interview.exception;

/**
 * Exception thrown when an employee attempts to update the status of a task
 * that is not assigned to them.
 *
 * <p>Handled by {@link GlobalExceptionHandler} to return a 403 Forbidden response.</p>
 */
public class TaskNotAssignedException extends RuntimeException {

    public TaskNotAssignedException(String message) {
        super(message);
    }
}
