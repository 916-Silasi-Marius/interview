package com.interview.security;

import com.interview.model.entities.Employee;
import com.interview.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-backed implementation of {@link UserDetailsService}.
 *
 * <p>Loads user credentials and roles from the {@code employee} table.
 * The employee's {@code role} is mapped to a Spring Security authority
 * (e.g., {@code ADMIN} → {@code ROLE_ADMIN}).</p>
 */
@Service
@RequiredArgsConstructor
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    /**
     * Loads an employee by username and converts it to a Spring Security {@link UserDetails}.
     *
     * @param username the username to look up
     * @return the user details for authentication
     * @throws UsernameNotFoundException if no employee exists with the given username
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Employee not found with username: " + username));

        return User.builder()
                .username(employee.getUsername())
                .password(employee.getPassword())
                .roles(employee.getRole().name())
                .build();
    }
}

