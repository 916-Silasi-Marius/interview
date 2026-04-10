package com.interview.service;

import com.interview.exception.DuplicateResourceException;
import com.interview.exception.ResourceNotFoundException;
import com.interview.model.dto.TagRequest;
import com.interview.model.dto.TagResponse;
import com.interview.model.dto.TagUpdateRequest;
import com.interview.model.entities.Tag;
import com.interview.model.mapper.TagMapper;
import com.interview.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for tag management operations.
 *
 * <p>Handles business logic including duplicate tag name validation,
 * entity mapping, and transactional boundaries.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    /**
     * Retrieves a paginated list of all tags.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of {@link TagResponse} DTOs
     */
    @Transactional(readOnly = true)
    public Page<TagResponse> getAllTags(Pageable pageable) {
        log.debug("Fetching tags page: {}", pageable);
        return tagRepository.findAll(pageable)
                .map(TagMapper::toResponse);
    }

    /**
     * Retrieves a single tag by its ID.
     *
     * @param id the tag ID
     * @return the tag as a response DTO
     * @throws ResourceNotFoundException if no tag exists with the given ID
     */
    @Transactional(readOnly = true)
    public TagResponse getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));
        return TagMapper.toResponse(tag);
    }

    /**
     * Creates a new tag after validating uniqueness of the tag name.
     *
     * @param request the tag creation request
     * @return the created tag as a response DTO
     * @throws DuplicateResourceException if the tag name is already taken
     */
    @Transactional
    public TagResponse createTag(TagRequest request) {
        if (tagRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Tag name '" + request.name() + "' is already taken");
        }

        Tag tag = TagMapper.toEntity(request);
        Tag saved = tagRepository.save(tag);
        log.info("Created tag with id: {}", saved.getId());
        return TagMapper.toResponse(saved);
    }

    /**
     * Fully updates an existing tag with all provided fields.
     *
     * <p>All fields are overwritten. Validates that the tag name
     * does not conflict with existing records.</p>
     *
     * @param id      the ID of the tag to update
     * @param request the full update request containing all fields
     * @return the updated tag as a response DTO
     * @throws ResourceNotFoundException  if no tag exists with the given ID
     * @throws DuplicateResourceException if the new tag name is already taken
     */
    @Transactional
    public TagResponse updateTag(Long id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));

        if (!request.name().equals(tag.getName())
                && tagRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Tag name '" + request.name() + "' is already taken");
        }

        TagMapper.fullUpdateEntity(tag, request);
        log.info("Fully updated tag with id: {}", id);
        return TagMapper.toResponse(tag);
    }

    /**
     * Partially updates an existing tag with the provided fields.
     *
     * <p>Only non-null fields in the request are applied (partial update).
     * Validates that any changed tag name does not conflict with existing records.</p>
     *
     * @param id      the ID of the tag to patch
     * @param request the partial update request containing fields to change
     * @return the updated tag as a response DTO
     * @throws ResourceNotFoundException  if no tag exists with the given ID
     * @throws DuplicateResourceException if the new tag name is already taken
     */
    @Transactional
    public TagResponse patchTag(Long id, TagUpdateRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + id));

        if (request.name() != null && !request.name().equals(tag.getName())
                && tagRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Tag name '" + request.name() + "' is already taken");
        }

        TagMapper.patchEntity(tag, request);
        log.info("Partially updated tag with id: {}", id);
        return TagMapper.toResponse(tag);
    }

    /**
     * Deletes a tag by its ID.
     *
     * @param id the ID of the tag to delete
     * @throws ResourceNotFoundException if no tag exists with the given ID
     */
    @Transactional
    public void deleteTag(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new ResourceNotFoundException("Tag not found with id: " + id);
        }
        tagRepository.deleteById(id);
        log.info("Deleted tag with id: {}", id);
    }
}

