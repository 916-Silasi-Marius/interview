package com.interview.model.mapper;

import com.interview.model.dto.TagRequest;
import com.interview.model.dto.TagResponse;
import com.interview.model.dto.TagUpdateRequest;
import com.interview.model.entities.Tag;
import org.junit.jupiter.api.Test;

import static com.interview.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

class TagMapperTest {

    @Test
    void toResponse_mapsAllFields() {
        Tag tag = buildTag();

        TagResponse response = TagMapper.toResponse(tag);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Backend");
        assertThat(response.description()).isEqualTo("Work related to backend");
    }

    @Test
    void toEntity_mapsNameAndDescription() {
        TagRequest request = new TagRequest("feature", "New feature");

        Tag entity = TagMapper.toEntity(request);

        assertThat(entity.getName()).isEqualTo("feature");
        assertThat(entity.getDescription()).isEqualTo("New feature");
        assertThat(entity.getId()).isNull();
    }

    @Test
    void fullUpdateEntity_overwritesAllFields() {
        Tag tag = Tag.builder().id(1L).name("old").description("Old desc").build();
        TagRequest request = new TagRequest("new", "New desc");

        TagMapper.fullUpdateEntity(tag, request);

        assertThat(tag.getName()).isEqualTo("new");
        assertThat(tag.getDescription()).isEqualTo("New desc");
    }

    @Test
    void patchEntity_nullFields_doesNotOverwrite() {
        Tag tag = buildTag();
        TagUpdateRequest request = new TagUpdateRequest(null, null);

        TagMapper.patchEntity(tag, request);

        assertThat(tag.getName()).isEqualTo("Backend");
        assertThat(tag.getDescription()).isEqualTo("Work related to backend");
    }

    @Test
    void patchEntity_allFields_overwritesAll() {
        Tag tag = Tag.builder().id(1L).name("old").description("Old").build();
        TagUpdateRequest request = new TagUpdateRequest("new", "New");

        TagMapper.patchEntity(tag, request);

        assertThat(tag.getName()).isEqualTo("new");
        assertThat(tag.getDescription()).isEqualTo("New");
    }
}
