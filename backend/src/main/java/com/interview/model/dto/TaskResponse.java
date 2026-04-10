package com.interview.model.dto;

import com.interview.model.enums.TaskPriority;
import com.interview.model.enums.TaskStatus;

import java.time.Instant;
import java.util.Set;

/**
 * Data transfer object returned to clients containing task details.
 *
 * <p>Includes flattened references to reporter and assignee (ID + name)
 * and a set of tag names to avoid exposing full entity graphs.</p>
 */
public record TaskResponse(
        Long id,
        String taskKey,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Integer storyPoints,
        Long reporterId,
        String reporterName,
        Long assigneeId,
        String assigneeName,
        Set<String> tags,
        Instant createdAt,
        Instant updatedAt
) {}
