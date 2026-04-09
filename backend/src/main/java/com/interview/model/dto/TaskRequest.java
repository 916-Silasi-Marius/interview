package com.interview.model.dto;

import com.interview.model.enums.TaskPriority;
import com.interview.model.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Data transfer object for creating or updating a task.
 *
 * <p>Contains bean validation constraints that are enforced when used
 * with {@code @Valid} in controller methods. Supports partial updates —
 * only non-null fields are applied during an update.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequest {

    @NotBlank(message = "Task key is required")
    @Size(max = 20, message = "Task key must not exceed 20 characters")
    private String taskKey;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    @Positive(message = "Story points must be a positive number")
    private Integer storyPoints;

    @NotNull(message = "Reporter ID is required")
    private Long reporterId;

    private Long assigneeId;

    private Set<Long> tagIds;
}

