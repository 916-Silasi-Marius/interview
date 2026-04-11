package com.interview.model.mapper;

import com.interview.model.dto.EmployeeRequest;
import com.interview.model.dto.EmployeeResponse;
import com.interview.model.dto.EmployeeUpdateRequest;
import com.interview.model.entities.Employee;
import com.interview.model.enums.EmployeeRole;
import org.junit.jupiter.api.Test;

import static com.interview.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

class EmployeeMapperTest {

    @Test
    void toResponse_mapsAllFields() {
        EmployeeResponse response = EmployeeMapper.toResponse(buildEmployee());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("jdoe");
        assertThat(response.email()).isEqualTo("jdoe@example.com");
        assertThat(response.fullName()).isEqualTo("John Doe");
        assertThat(response.role()).isEqualTo(EmployeeRole.DEVELOPER);
        assertThat(response.isActive()).isTrue();
        assertThat(response.createdAt()).isEqualTo(FIXED_CREATED_AT);
        assertThat(response.updatedAt()).isEqualTo(FIXED_UPDATED_AT);
    }

    @Test
    void toEntity_withRole_setsProvidedRole() {
        EmployeeRequest request = new EmployeeRequest("user", "u@e.com", "pass", "User", EmployeeRole.QA, false);

        Employee entity = EmployeeMapper.toEntity(request, "encoded");

        assertThat(entity.getRole()).isEqualTo(EmployeeRole.QA);
        assertThat(entity.getIsActive()).isFalse();
        assertThat(entity.getPassword()).isEqualTo("encoded");
    }

    @Test
    void toEntity_nullRole_defaultsToDeveloper() {
        EmployeeRequest request = new EmployeeRequest("user", "u@e.com", "pass", "User", null, null);

        Employee entity = EmployeeMapper.toEntity(request, "encoded");

        assertThat(entity.getRole()).isEqualTo(EmployeeRole.DEVELOPER);
        assertThat(entity.getIsActive()).isTrue();
    }

    @Test
    void fullUpdateEntity_overwritesAllFields() {
        Employee employee = buildEmployee();
        EmployeeRequest request = new EmployeeRequest("updated", "up@e.com", "pass", "Updated", EmployeeRole.PROJECT_MANAGER, false);

        EmployeeMapper.fullUpdateEntity(employee, request);

        assertThat(employee.getUsername()).isEqualTo("updated");
        assertThat(employee.getEmail()).isEqualTo("up@e.com");
        assertThat(employee.getFullName()).isEqualTo("Updated");
        assertThat(employee.getRole()).isEqualTo(EmployeeRole.PROJECT_MANAGER);
        assertThat(employee.getIsActive()).isFalse();
    }

    @Test
    void patchEntity_nullFields_doesNotOverwrite() {
        Employee employee = buildEmployee();
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(null, null, null, null, null, null);

        EmployeeMapper.patchEntity(employee, request);

        assertThat(employee.getUsername()).isEqualTo("jdoe");
        assertThat(employee.getEmail()).isEqualTo("jdoe@example.com");
        assertThat(employee.getFullName()).isEqualTo("John Doe");
        assertThat(employee.getRole()).isEqualTo(EmployeeRole.DEVELOPER);
        assertThat(employee.getIsActive()).isTrue();
    }

    @Test
    void patchEntity_allFields_overwritesAll() {
        Employee employee = buildEmployee();
        EmployeeUpdateRequest request = new EmployeeUpdateRequest("new", "new@e.com", "pass", "New Name", EmployeeRole.QA, false);

        EmployeeMapper.patchEntity(employee, request);

        assertThat(employee.getUsername()).isEqualTo("new");
        assertThat(employee.getEmail()).isEqualTo("new@e.com");
        assertThat(employee.getFullName()).isEqualTo("New Name");
        assertThat(employee.getRole()).isEqualTo(EmployeeRole.QA);
        assertThat(employee.getIsActive()).isFalse();
    }
}
