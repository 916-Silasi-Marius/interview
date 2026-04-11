package com.interview.service;

import com.interview.exception.DuplicateResourceException;
import com.interview.exception.ResourceNotFoundException;
import com.interview.model.dto.EmployeeRequest;
import com.interview.model.dto.EmployeeResponse;
import com.interview.model.dto.EmployeeUpdateRequest;
import com.interview.model.entities.Employee;
import com.interview.model.enums.EmployeeRole;
import com.interview.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static com.interview.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;

    private void assertAllFields(EmployeeResponse response, Employee expected) {
        assertThat(response.id()).isEqualTo(expected.getId());
        assertThat(response.username()).isEqualTo(expected.getUsername());
        assertThat(response.email()).isEqualTo(expected.getEmail());
        assertThat(response.fullName()).isEqualTo(expected.getFullName());
        assertThat(response.role()).isEqualTo(expected.getRole());
        assertThat(response.isActive()).isEqualTo(expected.getIsActive());
        assertThat(response.createdAt()).isEqualTo(expected.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(expected.getUpdatedAt());
    }

    @Test
    void getAllEmployees_returnsPageOfResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Employee> page = new PageImpl<>(List.of(buildEmployee()));
        when(employeeRepository.findAll(pageable)).thenReturn(page);

        Page<EmployeeResponse> result = employeeService.getAllEmployees(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertAllFields(result.getContent().getFirst(), buildEmployee());
    }

    @Test
    void getEmployeeById_found_returnsResponse() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(buildEmployee()));

        EmployeeResponse response = employeeService.getEmployeeById(1L);

        assertAllFields(response, buildEmployee());
    }

    @Test
    void getEmployeeById_notFound_throwsResourceNotFoundException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createEmployee_success_returnsCreatedResponse() {
        EmployeeRequest request = new EmployeeRequest("newuser", "new@example.com", "password123", "New User", EmployeeRole.DEVELOPER, true);
        when(employeeRepository.existsByUsername("newuser")).thenReturn(false);
        when(employeeRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(1L);
            e.setCreatedAt(FIXED_CREATED_AT);
            e.setUpdatedAt(FIXED_UPDATED_AT);
            return e;
        });

        EmployeeResponse response = employeeService.createEmployee(request);

        Employee expected = buildEmployee(1L, "newuser");
        expected.setEmail("new@example.com");
        expected.setFullName("New User");
        assertAllFields(response, expected);
        verify(passwordEncoder).encode("password123");
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void createEmployee_duplicateUsername_throwsDuplicateResourceException() {
        EmployeeRequest request = new EmployeeRequest("jdoe", "new@example.com", "password123", "New User", null, null);
        when(employeeRepository.existsByUsername("jdoe")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("jdoe");
    }

    @Test
    void createEmployee_duplicateEmail_throwsDuplicateResourceException() {
        EmployeeRequest request = new EmployeeRequest("newuser", "existing@example.com", "password123", "New User", null, null);
        when(employeeRepository.existsByUsername("newuser")).thenReturn(false);
        when(employeeRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("existing@example.com");
    }

    @Test
    void updateEmployee_success_returnsUpdatedResponse() {
        Employee employee = buildEmployee();
        EmployeeRequest request = new EmployeeRequest("updated", "updated@example.com", "newpass", "Updated Name", EmployeeRole.ADMIN, true);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByUsername("updated")).thenReturn(false);
        when(employeeRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(passwordEncoder.encode("newpass")).thenReturn("encoded_new");

        EmployeeResponse response = employeeService.updateEmployee(1L, request);

        assertAllFields(response, employee);
    }

    @Test
    void updateEmployee_notFound_throwsResourceNotFoundException() {
        EmployeeRequest request = new EmployeeRequest("u", "e@e.com", "pass1234", "N", null, null);
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployee(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateEmployee_sameUsername_noConflict() {
        Employee employee = buildEmployee();
        EmployeeRequest request = new EmployeeRequest("jdoe", "new@example.com", "password123", "John Doe", null, null);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        EmployeeResponse response = employeeService.updateEmployee(1L, request);

        assertAllFields(response, employee);
        verify(employeeRepository, never()).existsByUsername(anyString());
    }

    @Test
    void updateEmployee_duplicateUsername_throwsDuplicateResourceException() {
        Employee employee = buildEmployee();
        EmployeeRequest request = new EmployeeRequest("taken", "jdoe@example.com", "pass1234", "John", null, null);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.updateEmployee(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("taken");
    }

    @Test
    void patchEmployee_success_appliesOnlyProvidedFields() {
        Employee employee = buildEmployee();
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(null, null, null, "New Full Name", null, null);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.patchEmployee(1L, request);

        assertAllFields(response, employee);
    }

    @Test
    void patchEmployee_withPassword_encodesNewPassword() {
        Employee employee = buildEmployee();
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(null, null, "newpass123", null, null, null);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("newpass123")).thenReturn("new_encoded");

        EmployeeResponse response = employeeService.patchEmployee(1L, request);

        assertThat(employee.getPassword()).isEqualTo("new_encoded");
        assertAllFields(response, employee);
    }

    @Test
    void patchEmployee_withoutPassword_keepsExistingPassword() {
        Employee employee = buildEmployee();
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(null, null, null, "Name", null, null);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.patchEmployee(1L, request);

        assertThat(employee.getPassword()).isEqualTo("encoded");
        verify(passwordEncoder, never()).encode(anyString());
        assertAllFields(response, employee);
    }

    @Test
    void patchEmployee_duplicateUsername_throwsDuplicateResourceException() {
        Employee employee = buildEmployee();
        EmployeeUpdateRequest request = new EmployeeUpdateRequest("taken", null, null, null, null, null);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.patchEmployee(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("taken");
    }

    @Test
    void deleteEmployee_exists_deletesSuccessfully() {
        when(employeeRepository.existsById(1L)).thenReturn(true);

        employeeService.deleteEmployee(1L);

        verify(employeeRepository).deleteById(1L);
    }

    @Test
    void deleteEmployee_notFound_throwsResourceNotFoundException() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> employeeService.deleteEmployee(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createEmployee_concurrentDuplicate_throwsDuplicateResourceException() {
        EmployeeRequest request = new EmployeeRequest("newuser", "new@example.com", "password123",
                "New User", EmployeeRole.DEVELOPER, true);

        when(employeeRepository.existsByUsername("newuser")).thenReturn(false);
        when(employeeRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(employeeRepository.save(any(Employee.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint"));

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("newuser");
    }

    @Test
    void updateEmployee_concurrentModification_throwsDuplicateResourceException() {
        Employee employee = buildEmployee();
        EmployeeRequest request = new EmployeeRequest("jdoe", "jdoe@example.com", "newpass123",
                "Updated", EmployeeRole.ADMIN, true);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded");
        doThrow(new ObjectOptimisticLockingFailureException(Employee.class, 1L))
                .when(employeeRepository).flush();

        assertThatThrownBy(() -> employeeService.updateEmployee(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Employee with id 1")
                .hasMessageContaining("Please retry");
    }

    @Test
    void patchEmployee_concurrentModification_throwsDuplicateResourceException() {
        Employee employee = buildEmployee();
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(null, null, null, "Patched", null, null);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        doThrow(new ObjectOptimisticLockingFailureException(Employee.class, 1L))
                .when(employeeRepository).flush();

        assertThatThrownBy(() -> employeeService.patchEmployee(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Employee with id 1")
                .hasMessageContaining("Please retry");
    }
}
