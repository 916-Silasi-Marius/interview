package com.interview.model.dto;

/**
 * Data transfer object returned after successful authentication.
 *
 * <p>Contains the signed JWT token that the client must include
 * in the {@code Authorization} header for subsequent requests.</p>
 */
public record AuthResponse(String token) {}
