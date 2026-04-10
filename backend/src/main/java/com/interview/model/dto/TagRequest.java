package com.interview.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for creating a tag or performing a full update (PUT).
 *
 * <p>Contains bean validation constraints enforced when used
 * with {@code @Valid} in controller methods. All required fields must be provided.</p>
 */
public record TagRequest(
        @NotBlank(message = "Tag name is required")
        @Size(min = 1, max = 50, message = "Tag name must be between 1 and 50 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {}
