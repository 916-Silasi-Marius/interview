package com.interview.integration;

import com.interview.model.dto.EmployeeRequest;
import com.interview.model.dto.EmployeeUpdateRequest;
import com.interview.model.enums.EmployeeRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@code /api/v1/employee} endpoints.
 *
 * <p>All endpoints require the {@code ADMIN} role.</p>
 */
class EmployeeControllerIntegrationTest extends IntegrationTestBase {

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken(ADMIN_USER);
    }

    @Nested
    @DisplayName("GET /api/v1/employee")
    class GetAll {

        @Test
        @DisplayName("Admin gets paginated list of seeded employees")
        void getAll_asAdmin_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/employee")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(SEEDED_EMPLOYEES)))
                    .andExpect(jsonPath("$.page.totalElements").value(SEEDED_EMPLOYEES))
                    .andExpect(jsonPath("$.content[0].username").isNotEmpty())
                    .andExpect(jsonPath("$.content[0].email").isNotEmpty());
        }

        @Test
        @DisplayName("Developer gets 403")
        void getAll_asDev_returns403() throws Exception {
            String devToken = obtainToken(DEV_USER);

            mockMvc.perform(get("/api/v1/employee")
                            .header("Authorization", bearer(devToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated request returns 401")
        void getAll_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/employee"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/employee/{id}")
    class GetById {

        @Test
        @DisplayName("Returns seeded employee by ID")
        void getById_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/employee/1")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value("jdoe"))
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.fullName").value("John Doe"))
                    .andExpect(jsonPath("$.role").value("ADMIN"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        @DisplayName("Non-existent ID returns 404")
        void getById_notFound_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/employee/999")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error", containsString("not found")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/employee")
    class Create {

        @Test
        @DisplayName("Admin creates a new employee successfully")
        void create_returns201() throws Exception {
            EmployeeRequest request = new EmployeeRequest(
                    "newuser", "new@example.com", "password123", "New User",
                    EmployeeRole.QA);

            mockMvc.perform(post("/api/v1/employee")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.username").value("newuser"))
                    .andExpect(jsonPath("$.email").value("new@example.com"))
                    .andExpect(jsonPath("$.fullName").value("New User"))
                    .andExpect(jsonPath("$.role").value("QA"));
        }

        @Test
        @DisplayName("Duplicate username returns 409")
        void create_duplicateUsername_returns409() throws Exception {
            EmployeeRequest request = new EmployeeRequest(
                    "jdoe", "unique@example.com", "password123", "Another Doe",
                    EmployeeRole.DEVELOPER);

            mockMvc.perform(post("/api/v1/employee")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.error", containsString("already taken")));
        }

        @Test
        @DisplayName("Duplicate email returns 409")
        void create_duplicateEmail_returns409() throws Exception {
            EmployeeRequest request = new EmployeeRequest(
                    "unique", "john.doe@example.com", "password123", "Another Doe",
                    EmployeeRole.DEVELOPER);

            mockMvc.perform(post("/api/v1/employee")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error", containsString("already taken")));
        }

        @Test
        @DisplayName("Invalid body returns 400 with field errors")
        void create_invalidBody_returns400() throws Exception {
            EmployeeRequest request = new EmployeeRequest("", "not-an-email", "short", "", null);

            mockMvc.perform(post("/api/v1/employee")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.username").exists())
                    .andExpect(jsonPath("$.fieldErrors.email").exists())
                    .andExpect(jsonPath("$.fieldErrors.password").exists())
                    .andExpect(jsonPath("$.fieldErrors.fullName").exists());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/employee/{id}")
    class Update {

        @Test
        @DisplayName("Full update succeeds with 200")
        void update_returns200() throws Exception {
            EmployeeRequest request = new EmployeeRequest(
                    "jdoe", "john.updated@example.com", "newpassword123",
                    "John Doe Updated", EmployeeRole.ADMIN);

            mockMvc.perform(put("/api/v1/employee/1")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("john.updated@example.com"))
                    .andExpect(jsonPath("$.fullName").value("John Doe Updated"));
        }

        @Test
        @DisplayName("Non-existent ID returns 404")
        void update_notFound_returns404() throws Exception {
            EmployeeRequest request = new EmployeeRequest(
                    "ghost", "ghost@example.com", "password123",
                    "Ghost User", EmployeeRole.DEVELOPER);

            mockMvc.perform(put("/api/v1/employee/999")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/employee/{id}")
    class Patch {

        @Test
        @DisplayName("Partial update changes only provided fields")
        void patch_returns200() throws Exception {
            EmployeeUpdateRequest request = new EmployeeUpdateRequest(
                    null, null, null, "John D. Patched", null);

            mockMvc.perform(patch("/api/v1/employee/1")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("John D. Patched"))
                    .andExpect(jsonPath("$.username").value("jdoe"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/employee/{id}")
    class Delete {

        @Test
        @DisplayName("Deletes employee and returns 204")
        void delete_returns204() throws Exception {
            EmployeeRequest createReq = new EmployeeRequest(
                    "disposable", "disposable@example.com", "password123",
                    "Disposable User", EmployeeRole.QA);

            String createResponse = mockMvc.perform(post("/api/v1/employee")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(createReq)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            long createdId = objectMapper.readTree(createResponse).get("id").asLong();

            mockMvc.perform(delete("/api/v1/employee/" + createdId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/employee/" + createdId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Non-existent ID returns 404")
        void delete_notFound_returns404() throws Exception {
            mockMvc.perform(delete("/api/v1/employee/999")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }
}
