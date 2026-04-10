package com.interview.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Data transfer object for replacing the tags on a task.
 *
 * <p>Contains the complete set of tag IDs to associate with the task.
 * Pass an empty set to remove all tags.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskTagsRequest {

    @NotNull(message = "Tag IDs are required")
    private Set<Long> tagIds;
}

