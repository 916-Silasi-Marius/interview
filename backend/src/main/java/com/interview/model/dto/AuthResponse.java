package com.interview.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Data transfer object returned after successful authentication.
 *
 * <p>Contains the signed JWT token that the client must include
 * in the {@code Authorization} header for subsequent requests.</p>
 */
@Getter
@AllArgsConstructor
public class AuthResponse {

    private String token;
}
