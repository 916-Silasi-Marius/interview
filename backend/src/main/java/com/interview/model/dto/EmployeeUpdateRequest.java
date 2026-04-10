package com.interview.model.dto;

import com.interview.model.enums.EmployeeRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for partially updating an employee (PATCH).
 *
 * <p>All fields are optional. Only non-null fields will be applied
 * to the existing entity, preserving unchanged values.</p>
 */
public record EmployeeUpdateRequest(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Email(message = "Email must be valid")
        String email,

        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        EmployeeRole role,
        
        Boolean isActive
) {}
