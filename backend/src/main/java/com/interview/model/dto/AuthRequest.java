package com.interview.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object for authentication login requests.
 *
 * <p>Both username and password are required and validated
 * via bean validation constraints.</p>
 */
public record AuthRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
) {}
