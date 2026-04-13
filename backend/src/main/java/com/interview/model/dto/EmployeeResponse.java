package com.interview.model.dto;

import com.interview.model.enums.EmployeeRole;

import java.time.Instant;

/**
 * Data transfer object returned to clients containing employee details.
 *
 * <p>Excludes internal entity relationships (reported/assigned tasks)
 * to avoid circular references and unnecessary data exposure.</p>
 */
public record EmployeeResponse(
        Long id,
        String username,
        String email,
        String fullName,
        EmployeeRole role,
        Instant createdAt,
        Instant updatedAt
) {}
