package com.interview.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom access denied handler that returns a JSON 403 response
 * when an authenticated user lacks the required role for a resource.
 *
 * <p>Replaces Spring Security's default empty response with a structured
 * {@link ErrorResponse} JSON body.</p>
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     * Writes a JSON 403 Forbidden response when access is denied at the filter chain level.
     *
     * @param request               the HTTP request that was denied
     * @param response              the HTTP response to write to
     * @param accessDeniedException the exception indicating access was denied
     * @throws IOException if an I/O error occurs while writing the response
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(HttpStatus.FORBIDDEN.value(), "Access denied — insufficient permissions"));
    }
}
