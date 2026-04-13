package com.interview.service;

import com.interview.exception.ConcurrentModificationException;
import com.interview.exception.DuplicateResourceException;
import com.interview.exception.ResourceNotFoundException;
import com.interview.exception.TaskAlreadyAssignedException;
import com.interview.exception.TaskNotAssignedException;
import com.interview.model.dto.TaskRequest;
import com.interview.model.dto.TaskResponse;
import com.interview.model.dto.TaskStatusRequest;
import com.interview.model.dto.TaskUpdateRequest;
import com.interview.model.entities.Employee;
import com.interview.model.entities.Tag;
import com.interview.model.entities.Task;
import com.interview.model.mapper.TaskMapper;
import com.interview.repository.EmployeeRepository;
import com.interview.repository.TagRepository;
import com.interview.repository.TaskRepository;
import com.interview.repository.specification.TaskSpecification;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
     * <p>Uses a two-query approach: first fetches task IDs with SQL-level pagination,
     * then loads the full entities with relations for those IDs. This avoids
     * Hibernate's in-memory pagination when fetching collection associations.</p>
     *
     * @param pageable pagination and sorting parameters
     * @return a page of {@link TaskResponse} DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "task.service", extraTags = {"method", "getAllTasks"})
    public Page<TaskResponse> getAllTasks(Pageable pageable) {
        log.debug("Fetching tasks page: {}", pageable);
        Page<Long> idPage = taskRepository.findAllIds(pageable);
        return loadTaskPageByIds(idPage);
    }

    /**
     * Retrieves a single task by its ID.
     *
     * @param id the task ID
     * @return the task as a response DTO
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    @Transactional(readOnly = true)
    @Timed(value = "task.service", extraTags = {"method", "getTaskById"})
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findWithRelationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return TaskMapper.toResponse(task);
    }

    /**
     * Searches for tasks whose title or description contains any of the words
     * in the given query string (case-insensitive, word-level matching).
     *
     * <p>The query is split into individual words, and a task is returned if
     * any word appears as a substring in its title or description. This allows
     * queries like {@code "Login feature implementation"} to match tasks such as
     * {@code "Implement login page"}.</p>
     *
     * <p>Uses a two-query approach to keep pagination at the SQL level.</p>
     *
     * @param query    the search term (one or more words)
     * @param pageable pagination and sorting parameters
     * @return a page of matching {@link TaskResponse} DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "task.service", extraTags = {"method", "searchTasks"})
    public Page<TaskResponse> searchTasks(String query, Pageable pageable) {
        log.debug("Searching tasks with query: '{}', page: {}", query, pageable);
        // Step 1: fetch matching tasks with SQL-level pagination (tags collection not loaded)
        Page<Task> taskPage = taskRepository.findAll(
                TaskSpecification.titleOrDescriptionContainsAnyWord(query), pageable);
        List<Long> ids = taskPage.getContent().stream().map(Task::getId).toList();

        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }

        // Step 2: load full entities with relations for just those IDs
        Map<Long, Task> taskMap = taskRepository.findAllWithRelationsByIdIn(ids).stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        List<TaskResponse> ordered = ids.stream()
                .map(taskMap::get)
                .map(TaskMapper::toResponse)
                .toList();

        return new PageImpl<>(ordered, pageable, taskPage.getTotalElements());
    }

    /**
     * Creates a new task after validating uniqueness of the task key
     * and resolving reporter, assignee, and tag references.
     *
     * <p>If no reporter ID is provided, the authenticated user (identified by
     * {@code username}) is automatically set as the reporter.</p>
     *
     * @param request  the task creation request
     * @param username the username of the authenticated user (from JWT subject)
     * @return the created task as a response DTO
     * @throws DuplicateResourceException if the task key is already taken
     * @throws ResourceNotFoundException  if the reporter, assignee, or any tag ID is invalid
     */
    @Transactional
    @Timed(value = "task.service", extraTags = {"method", "createTask"})
    public TaskResponse createTask(TaskRequest request, String username) {
        if (taskRepository.existsByTaskKey(request.taskKey())) {
            throw new DuplicateResourceException("Task key '" + request.taskKey() + "' is already taken");
        }

        Employee reporter;
        if (request.reporterId() == null) {
            reporter = employeeRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Reporter not found with username: " + username));
        } else {
            reporter = resolveEmployee(request.reporterId(), "Reporter");
        }
        Employee assignee = request.assigneeId() != null
                ? resolveEmployee(request.assigneeId(), "Assignee")
                : null;
        Set<Tag> tags = resolveTags(request.tagIds());

        Task task = TaskMapper.toEntity(request, reporter, assignee, tags);
        try {
            Task saved = taskRepository.save(task);
            log.info("Created task with id: {} and key: {}", saved.getId(), saved.getTaskKey());
            return TaskMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Task key '" + request.taskKey() + "' is already taken");
        }
    }

    /**
     * Fully updates an existing task with all provided fields.
     *
     * <p>All fields are overwritten. Validates that the task key does not
     * conflict with existing records. Resolves reporter, assignee, and tags.</p>
     *
     * @param id      the ID of the task to update
     * @param request the full update request containing all fields
     * @return the updated task as a response DTO
     * @throws ResourceNotFoundException  if no task exists with the given ID
     * @throws DuplicateResourceException if the new task key is already taken
     */
    @Transactional
    @Timed(value = "task.service", extraTags = {"method", "updateTask"})
    public TaskResponse updateTask(Long id, TaskRequest request) {
        try {
            Task task = taskRepository.findWithRelationsById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

            if (!request.taskKey().equals(task.getTaskKey())
                    && taskRepository.existsByTaskKey(request.taskKey())) {
                throw new DuplicateResourceException("Task key '" + request.taskKey() + "' is already taken");
            }

            TaskMapper.fullUpdateEntity(task, request);

            task.setReporter(request.reporterId() != null
                    ? resolveEmployee(request.reporterId(), "Reporter")
                    : null);
            task.setAssignee(request.assigneeId() != null
                    ? resolveEmployee(request.assigneeId(), "Assignee")
                    : null);
            task.setTags(resolveTags(request.tagIds()));

            taskRepository.flush();
            log.info("Fully updated task with id: {}", id);
            return TaskMapper.toResponse(task);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConcurrentModificationException(
                    "Task with id " + id + " was modified by another request. Please retry.");
        }
    }

    /**
     * Partially updates an existing task with the provided fields.
     *
     * <p>Only non-null fields in the request are applied (partial update).
     * Validates that any changed task key does not conflict with existing records.
     * Re-resolves reporter, assignee, and tags if provided.</p>
     *
     * @param id      the ID of the task to patch
     * @param request the partial update request containing fields to change
     * @return the updated task as a response DTO
     * @throws ResourceNotFoundException  if no task exists with the given ID
     * @throws DuplicateResourceException if the new task key is already taken
     */
    @Transactional
    @Timed(value = "task.service", extraTags = {"method", "patchTask"})
    public TaskResponse patchTask(Long id, TaskUpdateRequest request) {
        try {
            Task task = taskRepository.findWithRelationsById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

            if (request.taskKey() != null && !request.taskKey().equals(task.getTaskKey())
                    && taskRepository.existsByTaskKey(request.taskKey())) {
                throw new DuplicateResourceException("Task key '" + request.taskKey() + "' is already taken");
            }

            TaskMapper.patchEntity(task, request);

            if (request.reporterId() != null) {
                task.setReporter(resolveEmployee(request.reporterId(), "Reporter"));
            }
            if (request.assigneeId() != null) {
                task.setAssignee(resolveEmployee(request.assigneeId(), "Assignee"));
            }
            if (request.tagIds() != null) {
                task.setTags(resolveTags(request.tagIds()));
            }

            taskRepository.flush();
            log.info("Partially updated task with id: {}", id);
            return TaskMapper.toResponse(task);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConcurrentModificationException(
                    "Task with id " + id + " was modified by another request. Please retry.");
        }
    }

    /**
     * Updates only the status of a task assigned to the given employee.
     *
     * <p>Verifies that the authenticated employee is the current assignee
     * of the task before applying the status change.</p>
     *
     * @param id       the ID of the task to update
     * @param request  the update request containing the new status
     * @param username the username of the authenticated employee (from JWT)
     * @return the updated task as a response DTO
     * @throws ResourceNotFoundException if no task or employee exists
     * @throws TaskNotAssignedException if the task is not assigned to the authenticated employee
     */
    @Transactional
    @Timed(value = "task.service", extraTags = {"method", "selfUpdateTaskStatus"})
    public TaskResponse selfUpdateTaskStatus(Long id, TaskStatusRequest request, String username) {
        try {
            Task task = taskRepository.findWithRelationsById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
            Employee employee = employeeRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with username: " + username));

            if (task.getAssignee() != null && task.getAssignee().getId().equals(employee.getId())) {
                task.setStatus(request.status());
                taskRepository.flush();
                log.info("Updated task status for id: {}", id);
                return TaskMapper.toResponse(task);
            }

            throw new TaskNotAssignedException("Task with id: " + id + " is not assigned to employee with username: " + username);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConcurrentModificationException(
                    "Task with id " + id + " was modified by another request. Please retry.");
        }
    }

    /**
     * Self-assigns a task to the currently authenticated employee.
     *
     * <p>Resolves the employee from the JWT subject (username) and
     * sets them as the task's assignee. Throws an exception if the task
     * is already assigned to a different employee.</p>
     *
     * @param id       the ID of the task to self-assign
     * @param username the username of the authenticated employee (from JWT)
     * @return the updated task as a response DTO
     * @throws ResourceNotFoundException      if no task or employee exists
     * @throws TaskAlreadyAssignedException   if the task is already assigned to another employee
     */
    @Transactional
    @Timed(value = "task.service", extraTags = {"method", "selfAssignTask"})
    public TaskResponse selfAssignTask(Long id, String username) {
        try {
            Task task = taskRepository.findWithRelationsById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

            if (task.getAssignee() != null && !task.getAssignee().getUsername().equals(username)) {
                throw new TaskAlreadyAssignedException(
                        "Task is already assigned to employee '" + task.getAssignee().getFullName() + "'");
            }

            Employee assignee = employeeRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with username: " + username));

            task.setAssignee(assignee);

            taskRepository.flush();
            log.info("Task {} self-assigned by employee '{}'", id, username);
            return TaskMapper.toResponse(task);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConcurrentModificationException(
                    "Task with id " + id + " was modified by another request. Please retry.");
        }
    }

    /**
     * Deletes a task by its ID.
     *
     * @param id the ID of the task to delete
     * @throws ResourceNotFoundException if no task exists with the given ID
     */
    @Transactional
    @Timed(value = "task.service", extraTags = {"method", "deleteTask"})
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

    /**
     * Loads full task entities for a page of IDs and maps them to response DTOs.
     *
     * <p>This is the second step of the two-query approach. The tasks are fetched
     * with all relations eagerly loaded, then reordered to match the original
     * page's sort order.</p>
     *
     * @param idPage the page of task IDs (with pagination metadata)
     * @return a page of {@link TaskResponse} DTOs preserving the original order
     */
    private Page<TaskResponse> loadTaskPageByIds(Page<Long> idPage) {
        List<Long> ids = idPage.getContent();
        if (ids.isEmpty()) {
            return Page.empty(idPage.getPageable());
        }

        Map<Long, Task> taskMap = taskRepository.findAllWithRelationsByIdIn(ids).stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        // Preserve the original sort order from the ID query
        List<TaskResponse> ordered = ids.stream()
                .map(taskMap::get)
                .map(TaskMapper::toResponse)
                .toList();

        return new PageImpl<>(ordered, idPage.getPageable(), idPage.getTotalElements());
    }
}
