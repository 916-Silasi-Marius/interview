package com.interview.security;

import com.interview.model.entities.Employee;
import com.interview.model.enums.EmployeeRole;
import com.interview.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static com.interview.TestUtils.buildEmployee;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeUserDetailsServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_found_returnsUserDetails() {
        Employee employee = buildEmployee();
        employee.setRole(EmployeeRole.ADMIN);
        when(employeeRepository.findByUsername("jdoe")).thenReturn(Optional.of(employee));

        UserDetails userDetails = userDetailsService.loadUserByUsername("jdoe");

        assertThat(userDetails.getUsername()).isEqualTo("jdoe");
        assertThat(userDetails.getPassword()).isEqualTo("encoded");
    }

    @Test
    void loadUserByUsername_found_mapsRoleCorrectly() {
        Employee employee = buildEmployee();
        employee.setRole(EmployeeRole.PROJECT_MANAGER);
        when(employeeRepository.findByUsername("asmith")).thenReturn(Optional.of(employee));

        UserDetails userDetails = userDetailsService.loadUserByUsername("asmith");

        assertThat(userDetails.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROJECT_MANAGER"));
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(employeeRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
