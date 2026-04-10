package com.interview.exception;

/**
 * Exception thrown when attempting to self-assign a task that is already
 * assigned to another employee.
 *
 * <p>Handled by {@link GlobalExceptionHandler} to return a 409 Conflict response.</p>
 */
public class TaskAlreadyAssignedException extends RuntimeException {

    /**
     * Constructs a new task already assigned exception with the specified detail message.
     *
     * @param message a description of the conflict
     */
    public TaskAlreadyAssignedException(String message) {
        super(message);
    }
}

