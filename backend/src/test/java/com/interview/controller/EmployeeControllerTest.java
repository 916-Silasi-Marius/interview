package com.interview.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.model.dto.EmployeeRequest;
import com.interview.model.dto.EmployeeUpdateRequest;
import com.interview.model.enums.EmployeeRole;
import com.interview.config.TestSecurityConfig;
import com.interview.service.EmployeeService;
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

import static com.interview.TestUtils.buildEmployeeResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@Import(TestSecurityConfig.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void getAllEmployees_asAdmin_returns200() throws Exception {
        when(employeeService.getAllEmployees(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildEmployeeResponse())));

        mockMvc.perform(get("/api/v1/employee")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("jdoe"));
    }

    @Test
    void getAllEmployees_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllEmployees_nonAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/employee")
                        .with(jwt().jwt(j -> j.subject("dev"))
                                .authorities(new SimpleGrantedAuthority("ROLE_DEVELOPER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEmployeeById_asAdmin_returns200() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(buildEmployeeResponse());

        mockMvc.perform(get("/api/v1/employee/1")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createEmployee_asAdmin_validRequest_returns201() throws Exception {
        EmployeeRequest request = new EmployeeRequest("newuser", "new@test.com", "password123", "New User", EmployeeRole.DEVELOPER, true);
        when(employeeService.createEmployee(any(EmployeeRequest.class))).thenReturn(buildEmployeeResponse());

        mockMvc.perform(post("/api/v1/employee")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createEmployee_asAdmin_invalidRequest_returns400() throws Exception {
        String invalidJson = """
                {"username": "", "email": "invalid", "password": "short", "fullName": ""}
                """;

        mockMvc.perform(post("/api/v1/employee")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEmployee_asAdmin_returns200() throws Exception {
        EmployeeRequest request = new EmployeeRequest("updated", "up@test.com", "password123", "Updated", EmployeeRole.ADMIN, true);
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class))).thenReturn(buildEmployeeResponse());

        mockMvc.perform(put("/api/v1/employee/1")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void patchEmployee_asAdmin_returns200() throws Exception {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(null, null, null, "Patched Name", null, null);
        when(employeeService.patchEmployee(eq(1L), any(EmployeeUpdateRequest.class))).thenReturn(buildEmployeeResponse());

        mockMvc.perform(patch("/api/v1/employee/1")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteEmployee_asAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/employee/1")
                        .with(jwt().jwt(j -> j.subject("admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(employeeService).deleteEmployee(1L);
    }
}
