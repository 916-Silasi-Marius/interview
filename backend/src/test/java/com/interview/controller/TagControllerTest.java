package com.interview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.model.dto.TagRequest;
import com.interview.model.dto.TagUpdateRequest;
import com.interview.config.TestSecurityConfig;
import com.interview.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.interview.TestUtils.buildTagResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
@Import(TestSecurityConfig.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getAllTags_authenticated_returns200() throws Exception {
        when(tagService.getAllTags(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildTagResponse())));

        mockMvc.perform(get("/api/v1/tag")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Backend"));
    }

    @Test
    void getTagById_authenticated_returns200() throws Exception {
        when(tagService.getTagById(1L)).thenReturn(buildTagResponse());

        mockMvc.perform(get("/api/v1/tag/1")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Backend"));
    }

    @Test
    void createTag_asAdmin_returns201() throws Exception {
        TagRequest request = new TagRequest("feature", "New feature");
        when(tagService.createTag(any(TagRequest.class))).thenReturn(buildTagResponse());

        mockMvc.perform(post("/api/v1/tag")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createTag_asDeveloper_returns403() throws Exception {
        TagRequest request = new TagRequest("feature", "New feature");

        mockMvc.perform(post("/api/v1/tag")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTag_invalidRequest_returns400() throws Exception {
        String invalidJson = """
                {"name": "", "description": ""}
                """;

        mockMvc.perform(post("/api/v1/tag")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTag_asProjectManager_returns200() throws Exception {
        TagRequest request = new TagRequest("updated", "Updated desc");
        when(tagService.updateTag(eq(1L), any(TagRequest.class))).thenReturn(buildTagResponse());

        mockMvc.perform(put("/api/v1/tag/1")
                        .with(jwt().jwt(j -> j.subject("pm"))
                                .authorities(new SimpleGrantedAuthority("ROLE_PROJECT_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void patchTag_asAdmin_returns200() throws Exception {
        TagUpdateRequest request = new TagUpdateRequest(null, "Patched desc");
        when(tagService.patchTag(eq(1L), any(TagUpdateRequest.class))).thenReturn(buildTagResponse());

        mockMvc.perform(patch("/api/v1/tag/1")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTag_asAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/tag/1")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(tagService).deleteTag(1L);
    }

    @Test
    void deleteTag_asDeveloper_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/tag/1")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isForbidden());
    }
}
