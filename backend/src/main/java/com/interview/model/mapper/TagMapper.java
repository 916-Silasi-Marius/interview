package com.interview.model.mapper;

import com.interview.model.dto.TagRequest;
import com.interview.model.dto.TagResponse;
import com.interview.model.dto.TagUpdateRequest;
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
        return new TagResponse(tag.getId(), tag.getName(), tag.getDescription());
    }

    /**
     * Converts a {@link TagRequest} DTO to a new {@link Tag} entity.
     *
     * @param request the tag creation request
     * @return a new tag entity (not yet persisted)
     */
    public static Tag toEntity(TagRequest request) {
        return Tag.builder()
                .name(request.name())
                .description(request.description())
                .build();
    }

    /**
     * Applies a full update to an existing {@link Tag} entity.
     *
     * <p>All fields from the request overwrite existing values.</p>
     *
     * @param tag     the existing tag entity to update
     * @param request the full update request containing all fields
     */
    public static void fullUpdateEntity(Tag tag, TagRequest request) {
        tag.setName(request.name());
        tag.setDescription(request.description());
    }

    /**
     * Applies a partial update to an existing {@link Tag} entity.
     *
     * <p>Only non-null fields from the request are applied, allowing
     * clients to send partial updates without overwriting existing values.</p>
     *
     * @param tag     the existing tag entity to update
     * @param request the partial update request containing fields to change
     */
    public static void patchEntity(Tag tag, TagUpdateRequest request) {
        if (request.name() != null) {
            tag.setName(request.name());
        }
        if (request.description() != null) {
            tag.setDescription(request.description());
        }
    }
}
