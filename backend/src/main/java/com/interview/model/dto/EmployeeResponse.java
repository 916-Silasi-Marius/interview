package com.interview.model.dto;

import com.interview.model.enums.EmployeeRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Data transfer object returned to clients containing employee details.
 *
 * <p>Excludes internal entity relationships (reported/assigned tasks)
 * to avoid circular references and unnecessary data exposure.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeResponse {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private EmployeeRole role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
