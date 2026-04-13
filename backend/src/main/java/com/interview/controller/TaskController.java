package com.interview.controller;

import com.interview.model.dto.ErrorResponse;
import com.interview.model.dto.TaskRequest;
import com.interview.model.dto.TaskResponse;
import com.interview.model.dto.TaskStatusRequest;
import com.interview.model.dto.TaskUpdateRequest;
import com.interview.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Tasks", description = "Task management — CRUD operations, search, self-assign, and status updates")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "List all tasks", description = "Retrieves a paginated list of all tasks with their reporter, assignee, and tags.")
    @ApiResponse(responseCode = "200", description = "Page of tasks returned")
    @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getAllTasks(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.getAllTasks(pageable));
    }

    @Operation(summary = "Search tasks",
            description = "Searches for tasks whose title or description contains any of the given keywords. "
                    + "Case-insensitive substring match. Query is split by whitespace into individual keywords.")
    @ApiResponse(responseCode = "200", description = "Page of matching tasks returned")
    @ApiResponse(responseCode = "400", description = "Invalid search query (blank or out of range)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/search")
    public ResponseEntity<Page<TaskResponse>> searchTasks(
            @RequestParam
            @NotBlank(message = "Search query must not be blank")
            @Size(min = 2, max = 100, message = "Search query must be between 2 and 100 characters")
            @Parameter(description = "Search term (2–100 characters)", example = "login")
            String query,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.searchTasks(query, pageable));
    }

    @Operation(summary = "Get task by ID", description = "Retrieves a single task by its ID, including reporter, assignee, and tags.")
    @ApiResponse(responseCode = "200", description = "Task found")
    @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Task not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @Operation(summary = "Create a task",
            description = "Creates a new task. Requires ADMIN or PROJECT_MANAGER role. "
                    + "If no reporterId is provided, the authenticated user is set as the reporter.")
    @ApiResponse(responseCode = "201", description = "Task created")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Insufficient permissions — requires ADMIN or PROJECT_MANAGER",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Reporter, assignee, or tag ID not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Task key already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request,
                                                   @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request, jwt.getSubject()));
    }

    @Operation(summary = "Full update a task",
            description = "Fully updates an existing task. All fields are overwritten. Requires ADMIN or PROJECT_MANAGER role.")
    @ApiResponse(responseCode = "200", description = "Task updated")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Task, reporter, assignee, or tag not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Task key already exists or concurrent modification",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @Operation(summary = "Partial update a task",
            description = "Partially updates a task. Only non-null fields are applied. Requires ADMIN or PROJECT_MANAGER role.")
    @ApiResponse(responseCode = "200", description = "Task patched")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Task, reporter, assignee, or tag not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Task key already exists or concurrent modification",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<TaskResponse> patchTask(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(taskService.patchTask(id, request));
    }

    @Operation(summary = "Update own task status",
            description = "Updates the status of a task assigned to the currently authenticated employee. "
                    + "Only the assignee of the task can use this endpoint.")
    @ApiResponse(responseCode = "200", description = "Task status updated")
    @ApiResponse(responseCode = "400", description = "Validation failed — status is required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Task is not assigned to the authenticated employee",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Task or employee not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> selfUpdateTaskStatus(@PathVariable Long id,
                                                             @Valid @RequestBody TaskStatusRequest request,
                                                             @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(taskService.selfUpdateTaskStatus(id, request, jwt.getSubject()));
    }

    @Operation(summary = "Self-assign a task",
            description = "Assigns the task to the currently authenticated employee. "
                    + "Fails if the task is already assigned to a different employee.")
    @ApiResponse(responseCode = "200", description = "Task self-assigned")
    @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Task or employee not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Task is already assigned to another employee",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}/self-assign")
    public ResponseEntity<TaskResponse> selfAssignTask(@PathVariable Long id,
                                                       @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(taskService.selfAssignTask(id, jwt.getSubject()));
    }

    @Operation(summary = "Delete a task", description = "Deletes a task by its ID. Requires ADMIN or PROJECT_MANAGER role.")
    @ApiResponse(responseCode = "204", description = "Task deleted")
    @ApiResponse(responseCode = "401", description = "Authentication required",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Insufficient permissions",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Task not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
