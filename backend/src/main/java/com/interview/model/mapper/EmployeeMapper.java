package com.interview.model.mapper;

import com.interview.model.dto.EmployeeRequest;
import com.interview.model.dto.EmployeeResponse;
import com.interview.model.entities.Employee;
import com.interview.model.enums.EmployeeRole;

/**
 * Utility class for mapping between {@link Employee} entities and DTOs.
 *
 * <p>Provides static methods for converting entities to response DTOs,
 * request DTOs to entities, and partial updates on existing entities.</p>
 */
public class EmployeeMapper {

    private EmployeeMapper() {
    }

    /**
     * Converts an {@link Employee} entity to an {@link EmployeeResponse} DTO.
     *
     * @param employee the employee entity to convert
     * @return the corresponding response DTO
     */
    public static EmployeeResponse toResponse(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .username(employee.getUsername())
                .email(employee.getEmail())
                .fullName(employee.getFullName())
                .role(employee.getRole())
                .isActive(employee.getIsActive())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    /**
     * Converts an {@link EmployeeRequest} DTO to a new {@link Employee} entity.
     *
     * <p>Applies default values for {@code role} (DEVELOPER) and
     * {@code isActive} (true) when not provided in the request.
     * The {@code encodedPassword} parameter must be a pre-hashed password
     * (BCrypt) — the mapper does not perform hashing.</p>
     *
     * @param request         the employee creation request
     * @param encodedPassword the BCrypt-hashed password
     * @return a new employee entity (not yet persisted)
     */
    public static Employee toEntity(EmployeeRequest request, String encodedPassword) {
        return Employee.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(encodedPassword)
                .fullName(request.getFullName())
                .role(request.getRole() != null ? request.getRole() : EmployeeRole.DEVELOPER)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
    }

    /**
     * Applies a partial update to an existing {@link Employee} entity.
     *
     * <p>Only non-null fields from the request are applied, allowing
     * clients to send partial updates without overwriting existing values.</p>
     *
     * @param employee the existing employee entity to update
     * @param request  the update request containing fields to change
     */
    public static void updateEntity(Employee employee, EmployeeRequest request) {
        if (request.getUsername() != null) {
            employee.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            employee.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) {
            employee.setFullName(request.getFullName());
        }
        if (request.getRole() != null) {
            employee.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            employee.setIsActive(request.getIsActive());
        }
    }
}
