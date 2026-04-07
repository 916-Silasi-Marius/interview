package com.interview.service;

import com.interview.exception.DuplicateResourceException;
import com.interview.exception.ResourceNotFoundException;
import com.interview.model.dto.EmployeeRequest;
import com.interview.model.dto.EmployeeResponse;
import com.interview.model.entities.Employee;
import com.interview.model.mapper.EmployeeMapper;
import com.interview.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for employee management operations.
 *
 * <p>Handles business logic including duplicate validation for username and email,
 * entity mapping, and transactional boundaries.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    /**
     * Retrieves a paginated list of all employees.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of {@link EmployeeResponse} DTOs
     */
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> getAllEmployees(Pageable pageable) {
        log.debug("Fetching employees page: {}", pageable);
        return employeeRepository.findAll(pageable)
                .map(EmployeeMapper::toResponse);
    }

    /**
     * Retrieves a single employee by their ID.
     *
     * @param id the employee ID
     * @return the employee as a response DTO
     * @throws ResourceNotFoundException if no employee exists with the given ID
     */
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return EmployeeMapper.toResponse(employee);
    }

    /**
     * Creates a new employee after validating uniqueness of username and email.
     *
     * @param request the employee creation request
     * @return the created employee as a response DTO
     * @throws DuplicateResourceException if the username or email is already taken
     */
    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already taken");
        }

        Employee employee = EmployeeMapper.toEntity(request);
        Employee saved = employeeRepository.save(employee);
        log.info("Created employee with id: {}", saved.getId());
        return EmployeeMapper.toResponse(saved);
    }

    /**
     * Updates an existing employee with the provided fields.
     *
     * <p>Only non-null fields in the request are applied (partial update).
     * Validates that any changed username or email does not conflict with existing records.</p>
     *
     * @param id      the ID of the employee to update
     * @param request the update request containing fields to change
     * @return the updated employee as a response DTO
     * @throws ResourceNotFoundException  if no employee exists with the given ID
     * @throws DuplicateResourceException if the new username or email is already taken
     */
    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        if (request.getUsername() != null && !request.getUsername().equals(employee.getUsername())
                && employeeRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (request.getEmail() != null && !request.getEmail().equals(employee.getEmail())
                && employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already taken");
        }

        EmployeeMapper.updateEntity(employee, request);
        log.info("Updated employee with id: {}", id);
        return EmployeeMapper.toResponse(employee);
    }

    /**
     * Deletes an employee by their ID.
     *
     * @param id the ID of the employee to delete
     * @throws ResourceNotFoundException if no employee exists with the given ID
     */
    @Transactional
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
        log.info("Deleted employee with id: {}", id);
    }
}
