package com.interview.model.dto;

import com.interview.model.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer object for updating a task's status.
 *
 * <p>Contains bean validation constraints that are enforced when used
 * with {@code @Valid} in controller methods.</p>
 */
public record TaskStatusRequest(
        @NotNull(message = "Task status is required") TaskStatus status
) {}
