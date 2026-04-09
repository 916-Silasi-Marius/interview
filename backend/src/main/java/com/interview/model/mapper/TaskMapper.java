package com.interview.model.mapper;

import com.interview.model.dto.TaskRequest;
import com.interview.model.dto.TaskResponse;
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
        return TaskResponse.builder()
                .id(task.getId())
                .taskKey(task.getTaskKey())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .storyPoints(task.getStoryPoints())
                .reporterId(task.getReporter().getId())
                .reporterName(task.getReporter().getFullName())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getFullName() : null)
                .tags(task.getTags() != null
                        ? task.getTags().stream().map(Tag::getName).collect(Collectors.toSet())
                        : Collections.emptySet())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    /**
     * Converts a {@link TaskRequest} DTO to a new {@link Task} entity.
     *
     * <p>The reporter and assignee {@link Employee} references and the {@link Tag} set
     * must be resolved and set separately by the service layer. Applies default values
     * for {@code status} (TODO) and {@code priority} (MEDIUM) when not provided.</p>
     *
     * @param request  the task creation request
     * @param reporter the reporter employee (must not be null)
     * @param assignee the assignee employee (may be null)
     * @param tags     the set of tags to associate with the task
     * @return a new task entity (not yet persisted)
     */
    public static Task toEntity(TaskRequest request, Employee reporter, Employee assignee, Set<Tag> tags) {
        return Task.builder()
                .taskKey(request.getTaskKey())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM)
                .storyPoints(request.getStoryPoints())
                .reporter(reporter)
                .assignee(assignee)
                .tags(tags != null ? tags : Collections.emptySet())
                .build();
    }

    /**
     * Applies a partial update to an existing {@link Task} entity.
     *
     * <p>Only non-null fields from the request are applied, allowing
     * clients to send partial updates without overwriting existing values.
     * Reporter, assignee, and tags must be resolved and set separately by the service layer.</p>
     *
     * @param task    the existing task entity to update
     * @param request the update request containing fields to change
     */
    public static void updateEntity(Task task, TaskRequest request) {
        if (request.getTaskKey() != null) {
            task.setTaskKey(request.getTaskKey());
        }
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getStoryPoints() != null) {
            task.setStoryPoints(request.getStoryPoints());
        }
    }
}

