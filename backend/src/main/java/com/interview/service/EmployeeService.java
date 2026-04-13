package com.interview.service;

import com.interview.exception.ConcurrentModificationException;
import com.interview.exception.DuplicateResourceException;
import com.interview.exception.ResourceNotFoundException;
import com.interview.model.dto.EmployeeRequest;
import com.interview.model.dto.EmployeeResponse;
import com.interview.model.dto.EmployeeUpdateRequest;
import com.interview.model.entities.Employee;
import com.interview.model.mapper.EmployeeMapper;
import com.interview.repository.EmployeeRepository;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for employee management operations.
 *
 * <p>Handles business logic including duplicate validation for username and email,
 * password hashing, entity mapping, and transactional boundaries.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Retrieves a paginated list of all employees.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of {@link EmployeeResponse} DTOs
     */
    @Transactional(readOnly = true)
    @Timed(value = "employee.service", extraTags = {"method", "getAllEmployees"})
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
    @Timed(value = "employee.service", extraTags = {"method", "getEmployeeById"})
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
    @Timed(value = "employee.service", extraTags = {"method", "createEmployee"})
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
        }
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email '" + request.email() + "' is already taken");
        }

        Employee employee = EmployeeMapper.toEntity(request, passwordEncoder.encode(request.password()));
        try {
            Employee saved = employeeRepository.save(employee);
            log.info("Created employee with id: {}", saved.getId());
            return EmployeeMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                    "Username '" + request.username() + "' or email '" + request.email() + "' is already taken");
        }
    }

    /**
     * Fully updates an existing employee with all provided fields.
     *
     * <p>All fields are overwritten. Validates that the username and email
     * do not conflict with existing records.</p>
     *
     * @param id      the ID of the employee to update
     * @param request the full update request containing all fields
     * @return the updated employee as a response DTO
     * @throws ResourceNotFoundException  if no employee exists with the given ID
     * @throws DuplicateResourceException if the username or email is already taken
     */
    @Transactional
    @Timed(value = "employee.service", extraTags = {"method", "updateEmployee"})
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        try {
            Employee employee = employeeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

            if (!request.username().equals(employee.getUsername())
                    && employeeRepository.existsByUsername(request.username())) {
                throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
            }
            if (!request.email().equals(employee.getEmail())
                    && employeeRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("Email '" + request.email() + "' is already taken");
            }

            EmployeeMapper.fullUpdateEntity(employee, request);
            employee.setPassword(passwordEncoder.encode(request.password()));
            employeeRepository.flush();
            log.info("Fully updated employee with id: {}", id);
            return EmployeeMapper.toResponse(employee);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConcurrentModificationException(
                    "Employee with id " + id + " was modified by another request. Please retry.");
        }
    }

    /**
     * Partially updates an existing employee with the provided fields.
     *
     * <p>Only non-null fields in the request are applied (partial update).
     * Validates that any changed username or email does not conflict with existing records.</p>
     *
     * @param id      the ID of the employee to patch
     * @param request the partial update request containing fields to change
     * @return the updated employee as a response DTO
     * @throws ResourceNotFoundException  if no employee exists with the given ID
     * @throws DuplicateResourceException if the new username or email is already taken
     */
    @Transactional
    @Timed(value = "employee.service", extraTags = {"method", "patchEmployee"})
    public EmployeeResponse patchEmployee(Long id, EmployeeUpdateRequest request) {
        try {
            Employee employee = employeeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

            if (request.username() != null && !request.username().equals(employee.getUsername())
                    && employeeRepository.existsByUsername(request.username())) {
                throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
            }
            if (request.email() != null && !request.email().equals(employee.getEmail())
                    && employeeRepository.existsByEmail(request.email())) {
                throw new DuplicateResourceException("Email '" + request.email() + "' is already taken");
            }

            EmployeeMapper.patchEntity(employee, request);
            if (request.password() != null) {
                employee.setPassword(passwordEncoder.encode(request.password()));
            }
            employeeRepository.flush();
            log.info("Partially updated employee with id: {}", id);
            return EmployeeMapper.toResponse(employee);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConcurrentModificationException(
                    "Employee with id " + id + " was modified by another request. Please retry.");
        }
    }

    /**
     * Deletes an employee by their ID.
     *
     * @param id the ID of the employee to delete
     * @throws ResourceNotFoundException if no employee exists with the given ID
     */
    @Transactional
    @Timed(value = "employee.service", extraTags = {"method", "deleteEmployee"})
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
        log.info("Deleted employee with id: {}", id);
    }
}
