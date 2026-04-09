package com.interview.model.dto;

import com.interview.model.enums.TaskPriority;
import com.interview.model.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data transfer object returned to clients containing task details.
 *
 * <p>Includes flattened references to reporter and assignee (ID + name)
 * and a set of tag names to avoid exposing full entity graphs.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {

    private Long id;
    private String taskKey;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Integer storyPoints;
    private Long reporterId;
    private String reporterName;
    private Long assigneeId;
    private String assigneeName;
    private Set<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

