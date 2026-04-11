package com.interview.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void generateToken_setsSubjectAndRoles() {
        when(authentication.getName()).thenReturn("jdoe");
        when(authentication.getAuthorities()).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        Jwt mockJwt = new Jwt("token-value", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"), Map.of("sub", "jdoe", "roles", "ROLE_ADMIN"));
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        String token = tokenService.generateToken(authentication);

        assertThat(token).isEqualTo("token-value");

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        var claims = captor.getValue().getClaims();
        assertThat(claims.getClaims().get("sub")).isEqualTo("jdoe");
        assertThat(claims.getClaims().get("roles")).isEqualTo("ROLE_ADMIN");
        assertThat(claims.getExpiresAt()).isAfter(Instant.now());
    }
}
