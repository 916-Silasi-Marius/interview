package com.interview.model.dto;

import com.interview.model.enums.EmployeeRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for creating an employee or performing a full update (PUT).
 *
 * <p>Contains bean validation constraints enforced when used
 * with {@code @Valid} in controller methods. All required fields must be provided.</p>
 */
public record EmployeeRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must not exceed 100 characters")
        String fullName,

        EmployeeRole role
) {}
