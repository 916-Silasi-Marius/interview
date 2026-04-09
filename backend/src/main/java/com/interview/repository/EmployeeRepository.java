package com.interview.repository;

import com.interview.model.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Employee} entities.
 *
 * <p>Provides CRUD operations and custom query methods for looking up
 * employees by username or email, and checking for duplicates.</p>
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Finds an employee by their username.
     *
     * @param username the username to search for
     * @return an optional containing the employee if found
     */
    Optional<Employee> findByUsername(String username);

    /**
     * Checks whether an employee with the given username already exists.
     *
     * @param username the username to check
     * @return {@code true} if a matching employee exists
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether an employee with the given email already exists.
     *
     * @param email the email to check
     * @return {@code true} if a matching employee exists
     */
    boolean existsByEmail(String email);
}
