package com.interview.model.dto;

import com.interview.model.enums.TaskPriority;
import com.interview.model.enums.TaskStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Data transfer object for partially updating a task (PATCH).
 *
 * <p>All fields are optional. Only non-null fields will be applied
 * to the existing entity, preserving unchanged values.
 * Reporter, assignee, and tags are resolved by the service layer.</p>
 */
public record TaskUpdateRequest(
        @Size(max = 20, message = "Task key must not exceed 20 characters")
        String taskKey,

        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        String description,

        TaskStatus status,

        TaskPriority priority,

        @Positive(message = "Story points must be a positive number")
        Integer storyPoints,

        Long reporterId,

        Long assigneeId,

        Set<Long> tagIds
) {}
