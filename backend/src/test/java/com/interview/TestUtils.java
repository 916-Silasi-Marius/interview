package com.interview;

import com.interview.model.dto.EmployeeResponse;
import com.interview.model.dto.TagResponse;
import com.interview.model.dto.TaskResponse;
import com.interview.model.entities.Employee;
import com.interview.model.entities.Tag;
import com.interview.model.entities.Task;
import com.interview.model.enums.EmployeeRole;
import com.interview.model.enums.TaskPriority;
import com.interview.model.enums.TaskStatus;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class TestUtils {

    public static final Instant FIXED_CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    public static final Instant FIXED_UPDATED_AT = Instant.parse("2026-01-02T00:00:00Z");

    private TestUtils() {}

    public static Employee buildEmployee() {
        return Employee.builder()
                .id(1L)
                .username("jdoe")
                .email("jdoe@example.com")
                .password("encoded")
                .fullName("John Doe")
                .role(EmployeeRole.DEVELOPER)
                .createdAt(FIXED_CREATED_AT)
                .updatedAt(FIXED_UPDATED_AT)
                .build();
    }

    public static Employee buildEmployee(Long id, String username) {
        return Employee.builder()
                .id(id)
                .username(username)
                .email(username + "@test.com")
                .password("encoded")
                .fullName("Full " + username)
                .role(EmployeeRole.DEVELOPER)
                .createdAt(FIXED_CREATED_AT)
                .updatedAt(FIXED_UPDATED_AT)
                .build();
    }

    public static Tag buildTag() {
        return Tag.builder()
                .id(1L)
                .name("Backend")
                .description("Work related to backend")
                .build();
    }

    public static Tag buildTag(Long id, String name, String description) {
        return Tag.builder()
                .id(id)
                .name(name)
                .description(description)
                .build();
    }

    public static Task buildTask(Employee reporter, Employee assignee) {
        return Task.builder()
                .id(1L)
                .taskKey("PROJ-1")
                .title("Test Task")
                .description("Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .storyPoints(3)
                .reporter(reporter)
                .assignee(assignee)
                .tags(new HashSet<>(Set.of(buildTag())))
                .createdAt(FIXED_CREATED_AT)
                .updatedAt(FIXED_UPDATED_AT)
                .build();
    }

    public static EmployeeResponse buildEmployeeResponse() {
        return new EmployeeResponse(1L, "jdoe", "jdoe@example.com", "John Doe",
                EmployeeRole.DEVELOPER, FIXED_CREATED_AT, FIXED_UPDATED_AT);
    }

    public static TaskResponse buildTaskResponse() {
        return new TaskResponse(1L, "PROJ-1", "Test Task", "Description",
                TaskStatus.TODO, TaskPriority.MEDIUM, 3,
                1L, "John Doe", 2L, "Full assignee",
                Set.of("Backend"), FIXED_CREATED_AT, FIXED_UPDATED_AT);
    }

    public static TagResponse buildTagResponse() {
        return new TagResponse(1L, "Backend", "Work related to backend");
    }
}
