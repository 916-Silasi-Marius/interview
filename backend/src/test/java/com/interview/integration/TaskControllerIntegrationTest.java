package com.interview.integration;

import com.interview.model.dto.TaskRequest;
import com.interview.model.dto.TaskStatusRequest;
import com.interview.model.dto.TaskUpdateRequest;
import com.interview.model.enums.TaskPriority;
import com.interview.model.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@code /api/v1/task} endpoints.
 *
 * <p>Read operations are open to any authenticated user.
 * Write operations require {@code ADMIN} or {@code PROJECT_MANAGER},
 * except self-assign and self-update-status which any authenticated user can use.</p>
 *
 * <p>Seeded tasks (V2__seed_data.sql):</p>
 * <ul>
 *   <li>PROJ-1 (id=1): reporter=jdoe(1), assignee=bwilson(3), status=DONE, tags=[frontend]</li>
 *   <li>PROJ-2 (id=2): reporter=asmith(2), assignee=bwilson(3), status=IN_PROGRESS, tags=[feature,frontend]</li>
 *   <li>PROJ-3 (id=3): reporter=cjones(4), assignee=bwilson(3), status=TODO, tags=[bug,backend]</li>
 *   <li>PROJ-4 (id=4): reporter=asmith(2), assignee=cjones(4), status=IN_REVIEW, tags=[]</li>
 *   <li>PROJ-5 (id=5): reporter=jdoe(1), assignee=null, status=TODO, tags=[improvement,frontend]</li>
 * </ul>
 */
class TaskControllerIntegrationTest extends IntegrationTestBase {

    private String adminToken;
    private String pmToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainToken(ADMIN_USER);
        pmToken = obtainToken(PM_USER);
    }

    @Nested
    @DisplayName("GET /api/v1/task")
    class GetAll {

        @Test
        @DisplayName("Returns paginated list of all seeded tasks with relations")
        void getAll_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/task")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(SEEDED_TASKS)))
                    .andExpect(jsonPath("$.page.totalElements").value(SEEDED_TASKS))
                    .andExpect(jsonPath("$.content[0].taskKey").isNotEmpty())
                    .andExpect(jsonPath("$.content[0].reporterName").isNotEmpty());
        }

        @Test
        @DisplayName("Developer can read tasks")
        void getAll_asDev_returns200() throws Exception {
            String devToken = obtainToken(DEV_USER);

            mockMvc.perform(get("/api/v1/task")
                            .header("Authorization", bearer(devToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(SEEDED_TASKS));
        }

        @Test
        @DisplayName("Unauthenticated request returns 401")
        void getAll_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/task"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/task/{id}")
    class GetById {

        @Test
        @DisplayName("Returns seeded task with all fields populated")
        void getById_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/task/1")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.taskKey").value("PROJ-1"))
                    .andExpect(jsonPath("$.title").value("Set up project infrastructure"))
                    .andExpect(jsonPath("$.description").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("DONE"))
                    .andExpect(jsonPath("$.priority").value("HIGH"))
                    .andExpect(jsonPath("$.storyPoints").value(3))
                    .andExpect(jsonPath("$.reporterId").value(1))
                    .andExpect(jsonPath("$.reporterName").value("John Doe"))
                    .andExpect(jsonPath("$.assigneeId").value(3))
                    .andExpect(jsonPath("$.assigneeName").value("Bob Wilson"))
                    .andExpect(jsonPath("$.tags", hasItem("frontend")))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        @DisplayName("Non-existent ID returns 404")
        void getById_notFound_returns404() throws Exception {
            mockMvc.perform(get("/api/v1/task/999")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/task/search")
    class Search {

        @Test
        @DisplayName("Matching query returns results")
        void search_found() throws Exception {
            // "design" matches PROJ-4 title "Design dashboard wireframes"
            mockMvc.perform(get("/api/v1/task/search")
                            .param("query", "design")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].taskKey").value("PROJ-4"));
        }

        @Test
        @DisplayName("Non-matching query returns empty page")
        void search_notFound() throws Exception {
            mockMvc.perform(get("/api/v1/task/search")
                            .param("query", "zzzznotfound")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(0))
                    .andExpect(jsonPath("$.content", empty()));
        }

        @Test
        @DisplayName("Multi-word query matches any word")
        void search_multiWord_returnsMatches() throws Exception {
            // "api design" — "api" matches PROJ-5, "design" matches PROJ-4
            mockMvc.perform(get("/api/v1/task/search")
                            .param("query", "api design")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(2));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/task")
    class Create {

        @Test
        @DisplayName("PM creates task — reporter auto-set from JWT")
        void create_asPm_autoSetsReporter() throws Exception {
            TaskRequest request = new TaskRequest(
                    "NEW-1", "New task", "Description", TaskStatus.TODO,
                    TaskPriority.LOW, 3, null, 3L, Set.of(1L, 2L));

            mockMvc.perform(post("/api/v1/task")
                            .header("Authorization", bearer(pmToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.taskKey").value("NEW-1"))
                    .andExpect(jsonPath("$.title").value("New task"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.reporterId").value(2))       // asmith id=2
                    .andExpect(jsonPath("$.reporterName").value("Alice Smith"))
                    .andExpect(jsonPath("$.assigneeId").value(3))
                    .andExpect(jsonPath("$.assigneeName").value("Bob Wilson"))
                    .andExpect(jsonPath("$.tags", hasSize(2)));
        }

        @Test
        @DisplayName("Admin creates task with explicit reporter")
        void create_withExplicitReporter() throws Exception {
            TaskRequest request = new TaskRequest(
                    "NEW-2", "Admin task", null, null,
                    null, null, 4L, null, null);

            mockMvc.perform(post("/api/v1/task")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.reporterId").value(4))
                    .andExpect(jsonPath("$.reporterName").value("Carol Jones"))
                    .andExpect(jsonPath("$.assigneeId").isEmpty())
                    .andExpect(jsonPath("$.tags", empty()));
        }

        @Test
        @DisplayName("Duplicate task key returns 409")
        void create_duplicateKey_returns409() throws Exception {
            TaskRequest request = new TaskRequest(
                    "PROJ-1", "Duplicate", null, null,
                    null, null, null, null, null);

            mockMvc.perform(post("/api/v1/task")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error", containsString("already taken")));
        }

        @Test
        @DisplayName("Developer cannot create tasks — 403")
        void create_asDev_returns403() throws Exception {
            String devToken = obtainToken(DEV_USER);
            TaskRequest request = new TaskRequest(
                    "DEV-1", "Dev task", null, null,
                    null, null, null, null, null);

            mockMvc.perform(post("/api/v1/task")
                            .header("Authorization", bearer(devToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Invalid body returns 400 with field errors")
        void create_invalidBody_returns400() throws Exception {
            TaskRequest request = new TaskRequest(
                    "", "", null, null, null, -1, null, null, null);

            mockMvc.perform(post("/api/v1/task")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.taskKey").exists())
                    .andExpect(jsonPath("$.fieldErrors.title").exists())
                    .andExpect(jsonPath("$.fieldErrors.storyPoints").exists());
        }

        @Test
        @DisplayName("Non-existent assigneeId returns 404")
        void create_invalidAssignee_returns404() throws Exception {
            TaskRequest request = new TaskRequest(
                    "NEW-3", "Task with ghost assignee", null, null,
                    null, null, null, 999L, null);

            mockMvc.perform(post("/api/v1/task")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error", containsString("not found")));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/task/{id}")
    class Update {

        @Test
        @DisplayName("Admin full-updates task")
        void update_returns200() throws Exception {
            TaskRequest request = new TaskRequest(
                    "PROJ-1", "Updated infrastructure", "New description",
                    TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, 5,
                    2L, 4L, Set.of(3L));

            mockMvc.perform(put("/api/v1/task/1")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated infrastructure"))
                    .andExpect(jsonPath("$.description").value("New description"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.priority").value("MEDIUM"))
                    .andExpect(jsonPath("$.storyPoints").value(5))
                    .andExpect(jsonPath("$.reporterId").value(2))
                    .andExpect(jsonPath("$.assigneeId").value(4))
                    .andExpect(jsonPath("$.tags", hasItem("improvement")));
        }

        @Test
        @DisplayName("Developer cannot update tasks — 403")
        void update_asDev_returns403() throws Exception {
            String devToken = obtainToken(DEV_USER);
            TaskRequest request = new TaskRequest(
                    "PROJ-1", "Hacked", null, null,
                    null, null, null, null, null);

            mockMvc.perform(put("/api/v1/task/1")
                            .header("Authorization", bearer(devToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/task/{id}")
    class Patch {

        @Test
        @DisplayName("PM partial-updates only provided fields")
        void patch_returns200() throws Exception {
            TaskUpdateRequest request = new TaskUpdateRequest(
                    null, "Patched title only", null, null, null, null, null, null, null);

            mockMvc.perform(patch("/api/v1/task/1")
                            .header("Authorization", bearer(pmToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Patched title only"))
                    .andExpect(jsonPath("$.taskKey").value("PROJ-1"))     // unchanged
                    .andExpect(jsonPath("$.status").value("DONE"));        // unchanged
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/task/{id}/status")
    class SelfUpdateStatus {

        @Test
        @DisplayName("Assignee can update their own task status")
        void selfUpdateStatus_asAssignee_returns200() throws Exception {
            // bwilson (DEV_USER) is assignee of PROJ-3 (id=3, status=TODO)
            String devToken = obtainToken(DEV_USER);
            TaskStatusRequest request = new TaskStatusRequest(TaskStatus.IN_PROGRESS);

            mockMvc.perform(patch("/api/v1/task/3/status")
                            .header("Authorization", bearer(devToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("Non-assignee gets 403")
        void selfUpdateStatus_notAssignee_returns403() throws Exception {
            // cjones (DEV_USER_2) is NOT the assignee of PROJ-3 (assigned to bwilson)
            String dev2Token = obtainToken(DEV_USER_2);
            TaskStatusRequest request = new TaskStatusRequest(TaskStatus.DONE);

            mockMvc.perform(patch("/api/v1/task/3/status")
                            .header("Authorization", bearer(dev2Token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/task/{id}/self-assign")
    class SelfAssign {

        @Test
        @DisplayName("Self-assign unassigned task succeeds")
        void selfAssign_unassigned_returns200() throws Exception {
            // PROJ-5 (id=5) has no assignee
            String devToken = obtainToken(DEV_USER);

            mockMvc.perform(patch("/api/v1/task/5/self-assign")
                            .header("Authorization", bearer(devToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assigneeId").value(3))
                    .andExpect(jsonPath("$.assigneeName").value("Bob Wilson"));
        }

        @Test
        @DisplayName("Self-assign task already assigned to another returns 409")
        void selfAssign_alreadyAssigned_returns409() throws Exception {
            // PROJ-4 (id=4) is assigned to cjones(4) — bwilson cannot self-assign
            String devToken = obtainToken(DEV_USER);

            mockMvc.perform(patch("/api/v1/task/4/self-assign")
                            .header("Authorization", bearer(devToken)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error", containsString("already assigned")));
        }

        @Test
        @DisplayName("Self-assign to self (already assignee) is idempotent")
        void selfAssign_alreadyAssignedToSelf_returns200() throws Exception {
            // PROJ-3 (id=3) is assigned to bwilson — bwilson self-assigns again
            String devToken = obtainToken(DEV_USER);

            mockMvc.perform(patch("/api/v1/task/3/self-assign")
                            .header("Authorization", bearer(devToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.assigneeId").value(3));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/task/{id}")
    class Delete {

        @Test
        @DisplayName("Admin deletes task and returns 204")
        void delete_asAdmin_returns204() throws Exception {
            // Create a disposable task first
            TaskRequest createReq = new TaskRequest(
                    "DEL-1", "To be deleted", null, null,
                    null, null, null, null, null);

            String response = mockMvc.perform(post("/api/v1/task")
                            .header("Authorization", bearer(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(createReq)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long createdId = objectMapper.readTree(response).get("id").asLong();

            mockMvc.perform(delete("/api/v1/task/" + createdId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNoContent());

            // Verify it's gone
            mockMvc.perform(get("/api/v1/task/" + createdId)
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Developer cannot delete tasks — 403")
        void delete_asDev_returns403() throws Exception {
            String devToken = obtainToken(DEV_USER);

            mockMvc.perform(delete("/api/v1/task/1")
                            .header("Authorization", bearer(devToken)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Non-existent ID returns 404")
        void delete_notFound_returns404() throws Exception {
            mockMvc.perform(delete("/api/v1/task/999")
                            .header("Authorization", bearer(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }
}
