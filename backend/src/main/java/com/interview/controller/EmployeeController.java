package com.interview.controller;

import com.interview.model.dto.EmployeeRequest;
import com.interview.model.dto.EmployeeResponse;
import com.interview.model.dto.EmployeeUpdateRequest;
import com.interview.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for employee management operations.
 *
 * <p>All endpoints require the {@code ADMIN} role. Accessible at {@code /api/v1/employee}.</p>
 */
@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * Retrieves a paginated list of all employees.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @return a page of employee responses
     */
    @GetMapping
    public ResponseEntity<Page<EmployeeResponse>> getAllEmployees(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(employeeService.getAllEmployees(pageable));
    }

    /**
     * Retrieves a single employee by their ID.
     *
     * @param id the employee ID
     * @return the employee details
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    /**
     * Creates a new employee.
     *
     * @param request the employee creation request (validated)
     * @return the created employee with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(request));
    }

    /**
     * Fully updates an existing employee.
     *
     * @param id      the ID of the employee to update
     * @param request the full update request (validated, all fields required)
     * @return the updated employee details
     */
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    /**
     * Partially updates an existing employee.
     *
     * @param id      the ID of the employee to patch
     * @param request the partial update request (validated, only provided fields are applied)
     * @return the updated employee details
     */
    @PatchMapping("/{id}")
    public ResponseEntity<EmployeeResponse> patchEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return ResponseEntity.ok(employeeService.patchEmployee(id, request));
    }

    /**
     * Deletes an employee by their ID.
     *
     * @param id the ID of the employee to delete
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }
}
