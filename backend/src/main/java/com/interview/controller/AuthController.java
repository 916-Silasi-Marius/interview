package com.interview.controller;

import com.interview.model.dto.AuthRequest;
import com.interview.model.dto.AuthResponse;
import com.interview.model.dto.ErrorResponse;
import com.interview.security.TokenService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication operations.
 *
 * <p>Provides a public login endpoint at {@code /api/v1/auth/login} that
 * authenticates credentials and returns a signed JWT token.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login endpoint for obtaining JWT tokens")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final MeterRegistry meterRegistry;

    private Counter loginSuccessCounter;
    private Counter loginFailureCounter;

    @PostConstruct
    void initCounters() {
        loginSuccessCounter = Counter.builder("auth.login")
                .tag("outcome", "success")
                .description("Successful login attempts").register(meterRegistry);
        loginFailureCounter = Counter.builder("auth.login")
                .tag("outcome", "failure")
                .description("Failed login attempts").register(meterRegistry);
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request the login credentials (username and password)
     * @return an {@link AuthResponse} containing the signed JWT token
     */
    @Operation(
            summary = "Login",
            description = "Authenticates a user with username and password, returning a signed JWT token. "
                    + "The token must be included in the Authorization header (Bearer <token>) for all subsequent requests."
    )
    @ApiResponse(responseCode = "200", description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed — missing username or password",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid username or password",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/login")
    @SecurityRequirements
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.debug("Login attempt for user: {}", request.username());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            String token = tokenService.generateToken(authentication);

            loginSuccessCounter.increment();
            log.info("User '{}' authenticated successfully", request.username());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (AuthenticationException ex) {
            loginFailureCounter.increment();
            throw ex;
        }
    }
}
