package com.interview.model.dto;

import com.interview.model.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object for updating a task's status.
 *
 * <p>Contains bean validation constraints that are enforced when used
 * with {@code @Valid} in controller methods.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStatusRequest {

    @NotNull(message = "Task status is required")
    private TaskStatus status;
}
