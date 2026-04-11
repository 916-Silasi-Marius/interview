package com.interview.integration;

import com.interview.model.dto.AuthRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@code /api/v1/auth} endpoints.
 */
class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("Admin login returns 200 with JWT token")
        void login_admin_returns200() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest(ADMIN_USER, PASSWORD))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("Project manager login returns 200 with JWT token")
        void login_pm_returns200() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest(PM_USER, PASSWORD))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("Developer login returns 200 with JWT token")
        void login_dev_returns200() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest(DEV_USER, PASSWORD))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty());
        }

        @Test
        @DisplayName("Wrong password returns 401")
        void login_wrongPassword_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest(ADMIN_USER, "wrongpassword"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Invalid username or password"));
        }

        @Test
        @DisplayName("Non-existent user returns 401")
        void login_unknownUser_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest("nonexistent", PASSWORD))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Blank fields return 400 with validation errors")
        void login_blankFields_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(new AuthRequest("", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors.username").exists())
                    .andExpect(jsonPath("$.fieldErrors.password").exists());
        }
    }

    @Test
    @DisplayName("Obtained token grants access to a protected endpoint")
    void obtainedToken_isUsableForProtectedEndpoint() throws Exception {
        String token = obtainToken(DEV_USER);

        mockMvc.perform(get("/api/v1/task")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
