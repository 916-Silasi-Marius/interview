package com.interview.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object for assigning or reassigning a task to an employee.
 *
 * <p>Contains the employee ID of the new assignee. Pass {@code null}
 * to unassign the task (handled at the service level).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssigneeRequest {

    @NotNull(message = "Assignee ID is required")
    private Long assigneeId;
}

