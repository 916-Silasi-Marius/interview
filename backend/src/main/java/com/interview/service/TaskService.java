package com.interview.service;

import com.interview.exception.DuplicateResourceException;
import com.interview.exception.ResourceNotFoundException;
import com.interview.model.dto.TaskRequest;
import com.interview.model.dto.TaskResponse;
import com.interview.model.dto.TaskStatusRequest;
import com.interview.model.entities.Employee;
import com.interview.model.entities.Tag;
import com.interview.model.entities.Task;
import com.interview.model.mapper.TaskMapper;
import com.interview.repository.EmployeeRepository;
import com.interview.repository.TagRepository;
import com.interview.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Service layer for task management operations.
 *
 * <p>Handles business logic including duplicate task key validation,
 * resolution of reporter/assignee/tag references, entity mapping,
 * and transactional boundaries.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final TagRepository tagRepository;

    /**
     * Retrieves a paginated list of all tasks.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of {@link TaskResponse} DTOs
     */
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        log.debug("Fetching tasks page: {}", pageable);
        return taskRepository.findAll(pageable)
                .map(TaskMapper::toResponse);
    }

    /**
     * Retrieves a single task by its ID.
     *
     * @param id the task ID
     * @return the task as a response DTO
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return TaskMapper.toResponse(task);
    }

    /**
     * Creates a new task after validating uniqueness of the task key
     * and resolving reporter, assignee, and tag references.
     *
     * @param request the task creation request
     * @return the created task as a response DTO
     * @throws DuplicateResourceException if the task key is already taken
     * @throws ResourceNotFoundException  if the reporter, assignee, or any tag ID is invalid
     */
    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        if (taskRepository.existsByTaskKey(request.getTaskKey())) {
            throw new DuplicateResourceException("Task key '" + request.getTaskKey() + "' is already taken");
        }

        Employee reporter = resolveEmployee(request.getReporterId(), "Reporter");
        Employee assignee = request.getAssigneeId() != null
                ? resolveEmployee(request.getAssigneeId(), "Assignee")
                : null;
        Set<Tag> tags = resolveTags(request.getTagIds());

        Task task = TaskMapper.toEntity(request, reporter, assignee, tags);
        Task saved = taskRepository.save(task);
        log.info("Created task with id: {} and key: {}", saved.getId(), saved.getTaskKey());
        return TaskMapper.toResponse(saved);
    }

    /**
     * Updates an existing task with the provided fields.
     *
     * <p>Only non-null fields in the request are applied (partial update).
     * Validates that any changed task key does not conflict with existing records.
     * Re-resolves reporter, assignee, and tags if provided.</p>
     *
     * @param id      the ID of the task to update
     * @param request the update request containing fields to change
     * @return the updated task as a response DTO
     * @throws ResourceNotFoundException  if no task exists with the given ID
     * @throws DuplicateResourceException if the new task key is already taken
     */
    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        if (request.getTaskKey() != null && !request.getTaskKey().equals(task.getTaskKey())
                && taskRepository.existsByTaskKey(request.getTaskKey())) {
            throw new DuplicateResourceException("Task key '" + request.getTaskKey() + "' is already taken");
        }

        TaskMapper.updateEntity(task, request);

        if (request.getReporterId() != null) {
            task.setReporter(resolveEmployee(request.getReporterId(), "Reporter"));
        }
        if (request.getAssigneeId() != null) {
            task.setAssignee(resolveEmployee(request.getAssigneeId(), "Assignee"));
        }
        if (request.getTagIds() != null) {
            task.setTags(resolveTags(request.getTagIds()));
        }

        log.info("Updated task with id: {}", id);
        return TaskMapper.toResponse(task);
    }

    /**
     * Updates only the status of an existing task.
     *
     *
     * @param id      the ID of the task to update
     * @param request the update request containing the new status
     * @return the updated task as a response DTO
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    @Transactional
    public TaskResponse updateTaskStatus(Long id, TaskStatusRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setStatus(request.getStatus());

        log.info("Updated task status for id: {}", id);
        return TaskMapper.toResponse(task);
    }

    /**
     * Deletes a task by its ID.
     *
     * @param id the ID of the task to delete
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    @Transactional
    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
        log.info("Deleted task with id: {}", id);
    }

    /**
     * Resolves an {@link Employee} by ID, throwing if not found.
     *
     * @param employeeId the employee ID to look up
     * @param label      a descriptive label (e.g., "Reporter", "Assignee") used in the error message
     * @return the resolved employee entity
     * @throws ResourceNotFoundException if no employee exists with the given ID
     */
    private Employee resolveEmployee(Long employeeId, String label) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(label + " not found with id: " + employeeId));
    }

    /**
     * Resolves a set of {@link Tag} entities by their IDs.
     *
     * @param tagIds the tag IDs to look up (may be null or empty)
     * @return the resolved set of tag entities, or an empty set if input is null/empty
     * @throws ResourceNotFoundException if any tag ID does not exist
     */
    private Set<Tag> resolveTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Tag> tags = new HashSet<>(tagRepository.findAllById(tagIds));
        if (tags.size() != tagIds.size()) {
            throw new ResourceNotFoundException("One or more tag IDs are invalid");
        }
        return tags;
    }
}

