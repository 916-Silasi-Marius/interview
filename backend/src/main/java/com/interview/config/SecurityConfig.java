package com.interview.config;

import com.interview.security.CustomAccessDeniedHandler;
import com.interview.security.CustomAuthenticationEntryPoint;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security configuration for the application.
 *
 * <p>Configures stateless JWT-based authentication using Spring's built-in
 * OAuth2 Resource Server support. User credentials and roles are loaded from
 * the {@code employee} table via {@link com.interview.security.EmployeeUserDetailsService}.</p>
 *
 * <p>URL-level access rules:</p>
 * <ul>
 *   <li>{@code /api/v1/auth/**} — public (login endpoint)</li>
 *   <li>{@code /h2-console/**} — public (development only)</li>
 *   <li>{@code /actuator/**} — public (health checks)</li>
 *   <li>{@code /swagger-ui/**, /v3/api-docs/**} — public (API documentation)</li>
 *   <li>{@code /api/v1/task/**} — authenticated</li>
 *   <li>{@code /api/v1/tag/**} — authenticated</li>
 *   <li>{@code /api/v1/employee/**} — authenticated (ADMIN enforced at method level via {@code @PreAuthorize})</li>
 *   <li>All other endpoints — require authentication</li>
 * </ul>
 *
 * <p>Fine-grained role checks (ADMIN, PROJECT_MANAGER) are enforced at the method level
 * via {@code @PreAuthorize} annotations on individual controller methods.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String JWT_ALGORITHM = "HmacSHA256";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomAuthenticationEntryPoint authEntryPoint,
                                                   CustomAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        .requestMatchers("/api/v1/task/**").authenticated()
                        .requestMatchers("/api/v1/tag/**").authenticated()
                        .requestMatchers("/api/v1/employee/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .build();
    }

    /**
     * Configures how JWT claims are mapped to Spring Security authorities.
     *
     * <p>Reads from the {@code roles} claim (instead of the default {@code scope})
     * and removes the default {@code SCOPE_} prefix so authorities match
     * the {@code ROLE_ADMIN} format expected by {@code hasRole("ADMIN")}.</p>
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), JWT_ALGORITHM);
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), JWT_ALGORITHM);
        ImmutableSecret<SecurityContext> secret = new ImmutableSecret<>(key);
        return new NimbusJwtEncoder(secret);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
