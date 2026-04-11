package com.interview.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.model.dto.AuthRequest;
import com.interview.model.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base class for integration tests.
 *
 * <p>Boots the full Spring context with an embedded H2 database,
 * Flyway migrations, and real security. Each test method runs within
 * a transaction that is rolled back automatically, keeping seed data intact.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class IntegrationTestBase {

    // Seeded employees (V2__seed_data.sql) — all passwords are "password"
    static final String ADMIN_USER = "jdoe";       // ADMIN
    static final String PM_USER = "asmith";         // PROJECT_MANAGER
    static final String DEV_USER = "bwilson";       // DEVELOPER (assignee of PROJ-1,2,3)
    static final String DEV_USER_2 = "cjones";      // DEVELOPER (assignee of PROJ-4)
    static final String PASSWORD = "password";

    // Seeded entity counts
    static final int SEEDED_EMPLOYEES = 4;
    static final int SEEDED_TAGS = 5;
    static final int SEEDED_TASKS = 5;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    /**
     * Logs in with the given username and default password, returning a signed JWT.
     */
    String obtainToken(String username) throws Exception {
        String body = objectMapper.writeValueAsString(new AuthRequest(username, PASSWORD));

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, AuthResponse.class).token();
    }

    /**
     * Serializes an object to JSON for use as a request body.
     */
    String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Returns the Authorization header value for the given token.
     */
    static String bearer(String token) {
        return "Bearer " + token;
    }
}
