package com.interview.model.enums;

/**
 * Enumeration of possible roles an employee can hold within the organization.
 *
 * <p>Stored as a string in the database via {@code @Enumerated(EnumType.STRING)}.</p>
 */
public enum EmployeeRole {
    DEVELOPER,
    PROJECT_MANAGER,
    QA,
    ADMIN
}
