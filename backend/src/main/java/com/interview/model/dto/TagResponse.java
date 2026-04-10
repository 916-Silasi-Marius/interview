package com.interview.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data transfer object returned to clients containing tag details.
 *
 * <p>Excludes the inverse many-to-many task relationship
 * to avoid circular references and unnecessary data exposure.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagResponse {

    private Long id;
    private String name;
    private String description;
}

