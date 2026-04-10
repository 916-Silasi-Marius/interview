package com.interview.model.dto;

/**
 * Data transfer object returned to clients containing tag details.
 *
 * <p>Excludes the inverse many-to-many task relationship
 * to avoid circular references and unnecessary data exposure.</p>
 */
public record TagResponse(
        Long id,
        String name,
        String description
) {}
