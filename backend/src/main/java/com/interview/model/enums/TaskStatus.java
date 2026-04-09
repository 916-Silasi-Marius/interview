package com.interview.model.enums;

/**
 * Enumeration of possible statuses a task can have within its lifecycle.
 *
 * <p>Stored as a string in the database via {@code @Enumerated(EnumType.STRING)}.</p>
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE
}

