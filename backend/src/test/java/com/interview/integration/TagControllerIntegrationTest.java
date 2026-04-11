package com.interview.integration;

import com.interview.model.dto.TagRequest;
import com.interview.model.dto.TagUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@code /api/v1/tag} endpoints.
 *
 * <p>Read operations are open to any authenticated user.
 * Write operations require {@code ADMIN} or {@code PROJECT_MANAGER}.</p>
 */
class TagControllerIntegrationTest extends IntegrationTestBase {

    private String adminToken;
    private String pmToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken(ADMIN_USER);
        pmToken = obtainToken(PM_USER);
    }

    @Nested
    @DisplayName("GET /api/v1/tag")
    class GetAll {

        @Test
        @DisplayName("Authenticated user gets paginated list of seeded tags")
        void getAll_returns200() throws Exception {
            String devToken = obtainToken(DEV_USER);

            mockMvc.perform(get("/api/v1/tag")
                            .header("Authorization", bearer(devToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(SEEDED_TAGS)))
                    .andExpect(jsonPath("$.page.totalElements").value(SEEDED_TAGS));
        }

        @Test
        @DisplayName("Unauthenticated request returns 401")
        void getAll_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/tag"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tag/{id}")
    class GetById {

        @Test
        @DisplayName("Returns seeded tag by ID")
        void getById_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/tag/1")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("bug"))
                    .andExpect(jsonPath("$.description").value("Something is broken"));
        }

        @Test
        @DisplayName("Non-existent ID returns 404")
        void getById_notFound_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/tag/999")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tag")
    class Create {

        @Test
        @DisplayName("Admin creates tag successfully")
        void create_asAdmin_returns201() throws Exception {
            TagRequest request = new TagRequest("security", "Security related work");

            mockMvc.perform(post("/api/v1/tag")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.name").value("security"))
                    .andExpect(jsonPath("$.description").value("Security related work"));
        }

        @Test
        @DisplayName("PM creates tag successfully")
        void create_asPm_returns201() throws Exception {
            TagRequest request = new TagRequest("devops", "DevOps related work");

            mockMvc.perform(post("/api/v1/tag")
                            .header("Authorization", bearer(pmToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("devops"));
        }

        @Test
        @DisplayName("Developer cannot create tags — 403")
        void create_asDev_returns403() throws Exception {
            String devToken = obtainToken(DEV_USER);
            TagRequest request = new TagRequest("blocked", "Blocked tag");

            mockMvc.perform(post("/api/v1/tag")
                            .header("Authorization", bearer(devToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Duplicate name returns 409")
        void create_duplicateName_returns409() throws Exception {
            TagRequest request = new TagRequest("bug", "Duplicate bug tag");

            mockMvc.perform(post("/api/v1/tag")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error", containsString("already taken")));
        }

        @Test
        @DisplayName("Invalid body returns 400")
        void create_invalidBody_returns400() throws Exception {
            TagRequest request = new TagRequest("", null);

            mockMvc.perform(post("/api/v1/tag")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.name").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/tag/{id}")
    class Update {

        @Test
        @DisplayName("PM full-updates tag successfully")
        void update_asPm_returns200() throws Exception {
            TagRequest request = new TagRequest("bug", "Updated bug description");

            mockMvc.perform(put("/api/v1/tag/1")
                            .header("Authorization", bearer(pmToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("bug"))
                    .andExpect(jsonPath("$.description").value("Updated bug description"));
        }

        @Test
        @DisplayName("Duplicate name for different tag returns 409")
        void update_duplicateName_returns409() throws Exception {
            // Try to rename tag 2 (feature) to "bug" (tag 1's name)
            TagRequest request = new TagRequest("bug", "Renamed to bug");

            mockMvc.perform(put("/api/v1/tag/2")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/tag/{id}")
    class Patch {

        @Test
        @DisplayName("Partial update changes only provided fields")
        void patch_returns200() throws Exception {
            TagUpdateRequest request = new TagUpdateRequest(null, "Patched description only");

            mockMvc.perform(patch("/api/v1/tag/1")
                            .header("Authorization", bearer(pmToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("bug"))                   // unchanged
                    .andExpect(jsonPath("$.description").value("Patched description only"));
        }

        @Test
        @DisplayName("Developer cannot patch tags — 403")
        void patch_asDev_returns403() throws Exception {
            String devToken = obtainToken(DEV_USER);
            TagUpdateRequest request = new TagUpdateRequest("hacked", null);

            mockMvc.perform(patch("/api/v1/tag/1")
                            .header("Authorization", bearer(devToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/tag/{id}")
    class Delete {

        @Test
        @DisplayName("Admin deletes tag and returns 204")
        void delete_asAdmin_returns204() throws Exception {
            // Create a disposable tag first
            TagRequest createReq = new TagRequest("temp-tag", "Temporary");
            String response = mockMvc.perform(post("/api/v1/tag")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(createReq)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            long createdId = objectMapper.readTree(response).get("id").asLong();

            mockMvc.perform(delete("/api/v1/tag/" + createdId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNoContent());

            // Verify it's gone
            mockMvc.perform(get("/api/v1/tag/" + createdId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Developer cannot delete tags — 403")
        void delete_asDev_returns403() throws Exception {
            String devToken = obtainToken(DEV_USER);

            mockMvc.perform(delete("/api/v1/tag/1")
                            .header("Authorization", bearer(devToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Non-existent ID returns 404")
        void delete_notFound_returns404() throws Exception {
            mockMvc.perform(delete("/api/v1/tag/999")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }
}
