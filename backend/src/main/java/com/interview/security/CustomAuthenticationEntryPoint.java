package com.interview.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom authentication entry point that differentiates between
 * missing tokens and invalid/expired tokens.
 *
 * <p>Returns a specific error message when the provided Bearer token
 * is malformed, expired, or otherwise invalid, versus when no token
 * is provided at all.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        if (authException instanceof InvalidBearerTokenException) {
            log.warn("Invalid token: {}", authException.getMessage());
            objectMapper.writeValue(response.getOutputStream(),
                    ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired token"));
        } else {
            objectMapper.writeValue(response.getOutputStream(),
                    ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), "Authentication required"));
        }
    }
}
