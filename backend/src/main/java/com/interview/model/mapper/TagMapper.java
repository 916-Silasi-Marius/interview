package com.interview.model.mapper;

import com.interview.model.dto.TagRequest;
import com.interview.model.dto.TagResponse;
import com.interview.model.entities.Tag;

/**
 * Utility class for mapping between {@link Tag} entities and DTOs.
 *
 * <p>Provides static methods for converting entities to response DTOs,
 * request DTOs to entities, and partial updates on existing entities.</p>
 */
public class TagMapper {

    private TagMapper() {
    }

    /**
     * Converts a {@link Tag} entity to a {@link TagResponse} DTO.
     *
     * @param tag the tag entity to convert
     * @return the corresponding response DTO
     */
    public static TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .description(tag.getDescription())
                .build();
    }

    /**
     * Converts a {@link TagRequest} DTO to a new {@link Tag} entity.
     *
     * @param request the tag creation request
     * @return a new tag entity (not yet persisted)
     */
    public static Tag toEntity(TagRequest request) {
        return Tag.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /**
     * Applies a partial update to an existing {@link Tag} entity.
     *
     * <p>Only non-null fields from the request are applied, allowing
     * clients to send partial updates without overwriting existing values.</p>
     *
     * @param tag     the existing tag entity to update
     * @param request the update request containing fields to change
     */
    public static void updateEntity(Tag tag, TagRequest request) {
        if (request.getName() != null) {
            tag.setName(request.getName());
        }
        if (request.getDescription() != null) {
            tag.setDescription(request.getDescription());
        }
    }
}

