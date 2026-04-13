package com.interview.model.mapper;

import com.interview.model.dto.EmployeeRequest;
import com.interview.model.dto.EmployeeResponse;
import com.interview.model.dto.EmployeeUpdateRequest;
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
        return new EmployeeResponse(
                employee.getId(),
                employee.getUsername(),
                employee.getEmail(),
                employee.getFullName(),
                employee.getRole(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    /**
     * Converts an {@link EmployeeRequest} DTO to a new {@link Employee} entity.
     *
     * <p>Applies default value for {@code role} (DEVELOPER)
     * when not provided in the request.
     * The {@code encodedPassword} parameter must be a pre-hashed password
     * (BCrypt) — the mapper does not perform hashing.</p>
     *
     * @param request         the employee creation request
     * @param encodedPassword the BCrypt-hashed password
     * @return a new employee entity (not yet persisted)
     */
    public static Employee toEntity(EmployeeRequest request, String encodedPassword) {
        return Employee.builder()
                .username(request.username())
                .email(request.email())
                .password(encodedPassword)
                .fullName(request.fullName())
                .role(request.role() != null ? request.role() : EmployeeRole.DEVELOPER)
                .build();
    }

    /**
     * Applies a full update to an existing {@link Employee} entity.
     *
     * <p>All fields from the request overwrite existing values.
     * Password is not set here — it must be handled separately by the service layer.</p>
     *
     * @param employee the existing employee entity to update
     * @param request  the full update request containing all fields
     */
    public static void fullUpdateEntity(Employee employee, EmployeeRequest request) {
        employee.setUsername(request.username());
        employee.setEmail(request.email());
        employee.setFullName(request.fullName());
        employee.setRole(request.role() != null ? request.role() : EmployeeRole.DEVELOPER);
    }

    /**
     * Applies a partial update to an existing {@link Employee} entity.
     *
     * <p>Only non-null fields from the request are applied, allowing
     * clients to send partial updates without overwriting existing values.
     * Password is not set here — it must be handled separately by the service layer.</p>
     *
     * @param employee the existing employee entity to update
     * @param request  the partial update request containing fields to change
     */
    public static void patchEntity(Employee employee, EmployeeUpdateRequest request) {
        if (request.username() != null) {
            employee.setUsername(request.username());
        }
        if (request.email() != null) {
            employee.setEmail(request.email());
        }
        if (request.fullName() != null) {
            employee.setFullName(request.fullName());
        }
        if (request.role() != null) {
            employee.setRole(request.role());
        }
    }
}
