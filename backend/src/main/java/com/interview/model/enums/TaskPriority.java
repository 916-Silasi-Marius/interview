package com.interview.model.enums;

/**
 * Enumeration of priority levels that can be assigned to a task.
 *
 * <p>Stored as a string in the database via {@code @Enumerated(EnumType.STRING)}.</p>
 */
public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

