package com.interview.controller;

import com.interview.model.dto.TagRequest;
import com.interview.model.dto.TagResponse;
import com.interview.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for tag management operations.
 *
 * <p>Read operations are available to all authenticated users.
 * Write operations (create, update, delete) require the {@code ADMIN} or {@code PROJECT_MANAGER} role.
 * Accessible at {@code /api/v1/tag}.</p>
 */
@RestController
@RequestMapping("/api/v1/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * Retrieves a paginated list of all tags.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @return a page of tag responses
     */
    @GetMapping
    public ResponseEntity<Page<TagResponse>> getAllTags(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(tagService.getAllTags(pageable));
    }

    /**
     * Retrieves a single tag by its ID.
     *
     * @param id the tag ID
     * @return the tag details
     */
    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> getTagById(@PathVariable Long id) {
        return ResponseEntity.ok(tagService.getTagById(id));
    }

    /**
     * Creates a new tag.
     *
     * @param request the tag creation request (validated)
     * @return the created tag with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody TagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.createTag(request));
    }

    /**
     * Updates an existing tag.
     *
     * @param id      the ID of the tag to update
     * @param request the update request (validated, supports partial updates)
     * @return the updated tag details
     */
    @PutMapping("/{id}")
    public ResponseEntity<TagResponse> updateTag(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
        return ResponseEntity.ok(tagService.updateTag(id, request));
    }

    /**
     * Deletes a tag by its ID.
     *
     * @param id the ID of the tag to delete
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}

