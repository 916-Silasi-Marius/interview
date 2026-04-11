package com.interview.service;

import com.interview.exception.DuplicateResourceException;
import com.interview.exception.ResourceNotFoundException;
import com.interview.model.dto.TagRequest;
import com.interview.model.dto.TagResponse;
import com.interview.model.dto.TagUpdateRequest;
import com.interview.model.entities.Tag;
import com.interview.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;

import static com.interview.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private void assertAllFields(TagResponse response, Tag expected) {
        assertThat(response.id()).isEqualTo(expected.getId());
        assertThat(response.name()).isEqualTo(expected.getName());
        assertThat(response.description()).isEqualTo(expected.getDescription());
    }

    @Test
    void getAllTags_returnsPageOfResponses() {
        Pageable pageable = PageRequest.of(0, 20);
        when(tagRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(buildTag())));

        Page<TagResponse> result = tagService.getAllTags(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertAllFields(result.getContent().getFirst(), buildTag());
    }

    @Test
    void getTagById_found_returnsResponse() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(buildTag()));

        TagResponse response = tagService.getTagById(1L);

        assertAllFields(response, buildTag());
    }

    @Test
    void getTagById_notFound_throwsResourceNotFoundException() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.getTagById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createTag_success_returnsCreatedResponse() {
        TagRequest request = new TagRequest("feature", "New feature request");
        when(tagRepository.existsByName("feature")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            t.setId(2L);
            return t;
        });

        TagResponse response = tagService.createTag(request);

        Tag expected = buildTag(2L, "feature", "New feature request");
        assertAllFields(response, expected);
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void createTag_duplicateName_throwsDuplicateResourceException() {
        TagRequest request = new TagRequest("Backend", "Duplicate");
        when(tagRepository.existsByName("Backend")).thenReturn(true);

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Backend");
    }

    @Test
    void updateTag_success_returnsUpdatedResponse() {
        Tag tag = buildTag();
        TagRequest request = new TagRequest("enhancement", "Updated description");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByName("enhancement")).thenReturn(false);

        TagResponse response = tagService.updateTag(1L, request);

        assertAllFields(response, tag);
    }

    @Test
    void updateTag_notFound_throwsResourceNotFoundException() {
        TagRequest request = new TagRequest("name", "desc");
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.updateTag(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateTag_sameName_noConflict() {
        Tag tag = buildTag();
        TagRequest request = new TagRequest("Backend", "Updated desc");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        TagResponse response = tagService.updateTag(1L, request);

        assertAllFields(response, tag);
        verify(tagRepository, never()).existsByName(anyString());
    }

    @Test
    void updateTag_duplicateName_throwsDuplicateResourceException() {
        Tag tag = buildTag();
        TagRequest request = new TagRequest("taken", "desc");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByName("taken")).thenReturn(true);

        assertThatThrownBy(() -> tagService.updateTag(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("taken");
    }

    @Test
    void patchTag_success_appliesOnlyProvidedFields() {
        Tag tag = buildTag();
        TagUpdateRequest request = new TagUpdateRequest(null, "New description");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        TagResponse response = tagService.patchTag(1L, request);

        assertAllFields(response, tag);
    }

    @Test
    void patchTag_duplicateName_throwsDuplicateResourceException() {
        Tag tag = buildTag();
        TagUpdateRequest request = new TagUpdateRequest("taken", null);
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByName("taken")).thenReturn(true);

        assertThatThrownBy(() -> tagService.patchTag(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("taken");
    }

    @Test
    void deleteTag_exists_deletesSuccessfully() {
        when(tagRepository.existsById(1L)).thenReturn(true);

        tagService.deleteTag(1L);

        verify(tagRepository).deleteById(1L);
    }

    @Test
    void deleteTag_notFound_throwsResourceNotFoundException() {
        when(tagRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> tagService.deleteTag(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createTag_concurrentDuplicateName_throwsDuplicateResourceException() {
        TagRequest request = new TagRequest("security", "Security work");

        when(tagRepository.existsByName("security")).thenReturn(false);
        when(tagRepository.save(any(Tag.class)))
                .thenThrow(new DataIntegrityViolationException("Unique constraint"));

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("security");
    }

    @Test
    void updateTag_concurrentModification_throwsDuplicateResourceException() {
        Tag tag = buildTag();
        TagRequest request = new TagRequest("bug", "Updated description");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        doThrow(new ObjectOptimisticLockingFailureException(Tag.class, 1L))
                .when(tagRepository).flush();

        assertThatThrownBy(() -> tagService.updateTag(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Tag with id 1")
                .hasMessageContaining("Please retry");
    }

    @Test
    void patchTag_concurrentModification_throwsDuplicateResourceException() {
        Tag tag = buildTag();
        TagUpdateRequest request = new TagUpdateRequest(null, "Patched desc");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        doThrow(new ObjectOptimisticLockingFailureException(Tag.class, 1L))
                .when(tagRepository).flush();

        assertThatThrownBy(() -> tagService.patchTag(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Tag with id 1")
                .hasMessageContaining("Please retry");
    }
}
