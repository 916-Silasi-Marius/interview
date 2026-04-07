package com.interview.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response returned by all exception handlers and security entry points.
 *
 * <p>Uses {@code @JsonInclude(NON_NULL)} to omit {@code fieldErrors} when not applicable.
 * Provides static factory methods for convenient construction.</p>
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final Map<String, String> fieldErrors;

    /**
     * Creates an error response without field-level errors.
     *
     * @param status the HTTP status code
     * @param error  a human-readable error message
     * @return a new {@link ErrorResponse} instance
     */
    public static ErrorResponse of(int status, String error) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .build();
    }

    /**
     * Creates an error response with field-level validation errors.
     *
     * @param status      the HTTP status code
     * @param error       a human-readable error message
     * @param fieldErrors a map of field names to their validation error messages
     * @return a new {@link ErrorResponse} instance
     */
    public static ErrorResponse of(int status, String error, Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .fieldErrors(fieldErrors)
                .build();
    }
}
