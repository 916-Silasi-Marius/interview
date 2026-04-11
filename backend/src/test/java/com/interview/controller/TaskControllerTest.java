package com.interview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.model.dto.TaskRequest;
import com.interview.model.dto.TaskStatusRequest;
import com.interview.model.dto.TaskUpdateRequest;
import com.interview.model.enums.TaskStatus;
import com.interview.config.TestSecurityConfig;
import com.interview.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.interview.TestUtils.buildTaskResponse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(TestSecurityConfig.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getAllTasks_authenticated_returns200() throws Exception {
        when(taskService.getAllTasks(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildTaskResponse())));

        mockMvc.perform(get("/api/v1/task")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].taskKey").value("PROJ-1"));
    }

    @Test
    void getTaskById_authenticated_returns200() throws Exception {
        when(taskService.getTaskById(1L)).thenReturn(buildTaskResponse());

        mockMvc.perform(get("/api/v1/task/1")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void searchTasks_authenticated_validQuery_returns200() throws Exception {
        when(taskService.searchTasks(eq("login"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildTaskResponse())));

        mockMvc.perform(get("/api/v1/task/search")
                        .param("query", "login")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isOk());
    }

    @Test
    void searchTasks_blankQuery_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/task/search")
                        .param("query", "")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchTasks_tooShortQuery_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/task/search")
                        .param("query", "a")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_asAdmin_returns201() throws Exception {
        TaskRequest request = new TaskRequest("NEW-1", "New Task", "Desc", null, null, null, null, null, null);
        when(taskService.createTask(any(TaskRequest.class), anyString())).thenReturn(buildTaskResponse());

        mockMvc.perform(post("/api/v1/task")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createTask_asProjectManager_returns201() throws Exception {
        TaskRequest request = new TaskRequest("NEW-1", "New Task", null, null, null, null, null, null, null);
        when(taskService.createTask(any(TaskRequest.class), anyString())).thenReturn(buildTaskResponse());

        mockMvc.perform(post("/api/v1/task")
                        .with(jwt().jwt(j -> j.subject("pm"))
                                .authorities(new SimpleGrantedAuthority("ROLE_PROJECT_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createTask_asDeveloper_returns403() throws Exception {
        TaskRequest request = new TaskRequest("NEW-1", "New Task", null, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/task")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTask_invalidRequest_returns400() throws Exception {
        String invalidJson = """
                {"taskKey": "", "title": ""}
                """;

        mockMvc.perform(post("/api/v1/task")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateTask_asAdmin_returns200() throws Exception {
        TaskRequest request = new TaskRequest("PROJ-1", "Updated", "Desc", null, null, null, null, null, null);
        when(taskService.updateTask(eq(1L), any(TaskRequest.class))).thenReturn(buildTaskResponse());

        mockMvc.perform(put("/api/v1/task/1")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateTask_asDeveloper_returns403() throws Exception {
        TaskRequest request = new TaskRequest("PROJ-1", "Updated", null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/v1/task/1")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void patchTask_asProjectManager_returns200() throws Exception {
        TaskUpdateRequest request = new TaskUpdateRequest(null, "Patched", null, null, null, null, null, null, null);
        when(taskService.patchTask(eq(1L), any(TaskUpdateRequest.class))).thenReturn(buildTaskResponse());

        mockMvc.perform(patch("/api/v1/task/1")
                        .with(jwt().jwt(j -> j.subject("pm"))
                                .authorities(new SimpleGrantedAuthority("ROLE_PROJECT_MANAGER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void selfUpdateTaskStatus_authenticated_returns200() throws Exception {
        TaskStatusRequest request = new TaskStatusRequest(TaskStatus.IN_PROGRESS);
        when(taskService.selfUpdateTaskStatus(eq(1L), any(TaskStatusRequest.class), anyString())).thenReturn(buildTaskResponse());

        mockMvc.perform(patch("/api/v1/task/1/status")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void selfAssignTask_authenticated_returns200() throws Exception {
        when(taskService.selfAssignTask(eq(1L), anyString())).thenReturn(buildTaskResponse());

        mockMvc.perform(patch("/api/v1/task/1/self-assign")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isOk());
    }

    @Test
    void deleteTask_asAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/task/1")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(1L);
    }

    @Test
    void deleteTask_asDeveloper_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/task/1")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isForbidden());
    }
}
