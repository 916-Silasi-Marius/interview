package com.interview.controller;

import com.interview.model.dto.TaskRequest;
import com.interview.model.dto.TaskResponse;
import com.interview.model.dto.TaskStatusRequest;
import com.interview.model.dto.TaskUpdateRequest;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for task management operations.
 *
 * <p>Read operations are available to all authenticated users.
 * Write operations (create, update, delete) require the {@code ADMIN} or {@code PROJECT_MANAGER} role, unless the
 * employee updates their own task.
 * Accessible at {@code /api/v1/task}.</p>
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
     * @param jwt     the JWT of the authenticated user (injected by Spring Security)
     * @return the created task with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request, jwt.getSubject()));
    }

    /**
     * Fully updates an existing task.
     *
     * @param id      the ID of the task to update
     * @param request the full update request (validated, all fields required)
     * @return the updated task details
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    /**
     * Partially updates an existing task.
     *
     * @param id      the ID of the task to patch
     * @param request the partial update request (validated, only provided fields are applied)
     * @return the updated task details
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<TaskResponse> patchTask(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(taskService.patchTask(id, request));
    }

    /**
     * Updates the status of a task assigned to the currently authenticated employee.
     *
     * @param id      the ID of the task to update
     * @param request the update request containing the new status (validated)
     * @param jwt     the JWT of the authenticated user (injected by Spring Security)
     * @return the updated task details
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> selfUpdateTaskStatus(@PathVariable Long id,
                                                             @Valid @RequestBody TaskStatusRequest request,
                                                             @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(taskService.selfUpdateTaskStatus(id, request, jwt.getSubject()));
    }

    /**
     * Self-assigns a task to the currently authenticated employee.
     *
     * <p>Uses the JWT subject (username) to resolve the employee.
     * Any authenticated user can self-assign a task.</p>
     *
     * @param id  the ID of the task to self-assign
     * @param jwt the JWT of the authenticated user (injected by Spring Security)
     * @return the updated task details
     */
    @PatchMapping("/{id}/self-assign")
    public ResponseEntity<TaskResponse> selfAssignTask(@PathVariable Long id,
                                                       @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(taskService.selfAssignTask(id, jwt.getSubject()));
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

