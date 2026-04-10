package com.interview.model.dto;

import jakarta.validation.constraints.Size;

/**
 * Data transfer object for partially updating a tag (PATCH).
 *
 * <p>All fields are optional. Only non-null fields will be applied
 * to the existing entity, preserving unchanged values.</p>
 */
public record TagUpdateRequest(
        @Size(min = 1, max = 50, message = "Tag name must be between 1 and 50 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {}
