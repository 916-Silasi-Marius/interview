package com.interview.controller;

import com.interview.model.dto.TaskAssigneeRequest;
import com.interview.model.dto.TaskRequest;
import com.interview.model.dto.TaskResponse;
import com.interview.model.dto.TaskStatusRequest;
import com.interview.model.dto.TaskTagsRequest;
import com.interview.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for task management operations.
 *
 * <p>All endpoints require authentication. Accessible at {@code /api/v1/task}.</p>
 */
@RestController
@RequestMapping("/api/v1/task")
@RequiredArgsConstructor
@Validated
public class TaskController {

    private final TaskService taskService;

    /**
     * Retrieves a paginated list of all tasks.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @return a page of task responses
     */
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getAllTasks(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.getAllTasks(pageable));
    }

    /**
     * Searches for tasks whose title or description matches the given query.
     *
     * <p>Performs a case-insensitive substring match. Useful for checking
     * whether a similar task already exists before creating a new one.</p>
     *
     * @param query    the search term (2–100 characters)
     * @param pageable pagination parameters (page, size, sort)
     * @return a page of matching task responses
     */
    @GetMapping("/search")
    public ResponseEntity<Page<TaskResponse>> searchTasks(
            @RequestParam
            @NotBlank(message = "Search query must not be blank")
            @Size(min = 2, max = 100, message = "Search query must be between 2 and 100 characters")
            String query,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.searchTasks(query, pageable));
    }

    /**
     * Retrieves a single task by its ID.
     *
     * @param id the task ID
     * @return the task details
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    /**
     * Creates a new task.
     *
     * @param request the task creation request (validated)
     * @return the created task with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    /**
     * Updates an existing task.
     *
     * @param id      the ID of the task to update
     * @param request the update request (validated, supports partial updates)
     * @return the updated task details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    /**
     * Update an existing task's status.
     *
     * @param id      the ID of the task to update
     * @param request the update request (validated)
     * @return the updated task details
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, request));
    }

    /**
     * Assigns or reassigns a task to an employee.
     *
     * @param id      the ID of the task to assign
     * @param request the request containing the assignee's employee ID
     * @return the updated task details
     */
    @PatchMapping("/{id}/assignee")
    public ResponseEntity<TaskResponse> assignTask(@PathVariable Long id, @Valid @RequestBody TaskAssigneeRequest request) {
        return ResponseEntity.ok(taskService.assignTask(id, request));
    }

    /**
     * Replaces the tags associated with a task.
     *
     * @param id      the ID of the task to update
     * @param request the request containing the set of tag IDs
     * @return the updated task details
     */
    @PutMapping("/{id}/tags")
    public ResponseEntity<TaskResponse> updateTaskTags(@PathVariable Long id, @Valid @RequestBody TaskTagsRequest request) {
        return ResponseEntity.ok(taskService.updateTaskTags(id, request));
    }

    /**
     * Deletes a task by its ID.
     *
     * @param id the ID of the task to delete
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}

