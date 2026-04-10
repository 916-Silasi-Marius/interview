package com.interview.model.mapper;

import com.interview.model.dto.TaskRequest;
import com.interview.model.dto.TaskResponse;
import com.interview.model.dto.TaskUpdateRequest;
import com.interview.model.entities.Employee;
import com.interview.model.entities.Tag;
import com.interview.model.entities.Task;
import com.interview.model.enums.TaskPriority;
import com.interview.model.enums.TaskStatus;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for mapping between {@link Task} entities and DTOs.
 *
 * <p>Provides static methods for converting entities to response DTOs,
 * request DTOs to entities, and partial updates on existing entities.</p>
 */
public class TaskMapper {

    private TaskMapper() {
    }

    /**
     * Converts a {@link Task} entity to a {@link TaskResponse} DTO.
     *
     * <p>Flattens reporter and assignee relationships to their ID and full name,
     * and maps tags to a set of tag names.</p>
     *
     * @param task the task entity to convert
     * @return the corresponding response DTO
     */
    public static TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTaskKey(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getStoryPoints(),
                task.getReporter() != null ? task.getReporter().getId() : null,
                task.getReporter() != null ? task.getReporter().getFullName() : null,
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getFullName() : null,
                task.getTags() != null
                        ? task.getTags().stream().map(Tag::getName).collect(Collectors.toSet())
                        : Collections.emptySet(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    /**
     * Converts a {@link TaskRequest} DTO to a new {@link Task} entity.
     *
     * <p>The reporter, assignee, and tags must be resolved by the service layer
     * and passed as parameters. Applies default values for {@code status} (TODO)
     * and {@code priority} (MEDIUM) when not provided.</p>
     *
     * @param request  the task creation request
     * @param reporter the reporter employee (must not be null)
     * @param assignee the assignee employee (may be null)
     * @param tags     the set of tags to associate with the task
     * @return a new task entity (not yet persisted)
     */
    public static Task toEntity(TaskRequest request, Employee reporter, Employee assignee, Set<Tag> tags) {
        return Task.builder()
                .taskKey(request.taskKey())
                .title(request.title())
                .description(request.description())
                .status(request.status() != null ? request.status() : TaskStatus.TODO)
                .priority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM)
                .storyPoints(request.storyPoints())
                .reporter(reporter)
                .assignee(assignee)
                .tags(tags != null ? tags : Collections.emptySet())
                .build();
    }

    /**
     * Applies a full update to an existing {@link Task} entity's scalar fields.
     *
     * <p>All scalar fields from the request overwrite existing values.
     * Reporter, assignee, and tags must be resolved and set separately by the service layer.</p>
     *
     * @param task    the existing task entity to update
     * @param request the full update request containing all fields
     */
    public static void fullUpdateEntity(Task task, TaskRequest request) {
        task.setTaskKey(request.taskKey());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.TODO);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
        task.setStoryPoints(request.storyPoints());
    }

    /**
     * Applies a partial update to an existing {@link Task} entity.
     *
     * <p>Only non-null fields from the request are applied, allowing
     * clients to send partial updates without overwriting existing values.
     * Reporter, assignee, and tags must be resolved and set separately by the service layer.</p>
     *
     * @param task    the existing task entity to update
     * @param request the partial update request containing fields to change
     */
    public static void patchEntity(Task task, TaskUpdateRequest request) {
        if (request.taskKey() != null) {
            task.setTaskKey(request.taskKey());
        }
        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.storyPoints() != null) {
            task.setStoryPoints(request.storyPoints());
        }
    }
}
