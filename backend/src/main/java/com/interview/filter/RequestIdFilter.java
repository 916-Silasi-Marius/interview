package com.interview.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that assigns a unique request ID to every incoming HTTP request.
 *
 * <p>The request ID is stored in SLF4J's {@link MDC} under the key {@code requestId},
 * making it automatically available in all log statements during the request lifecycle.
 * If the client provides an {@code X-Request-Id} header (e.g., for distributed tracing),
 * that value is used; otherwise, a new UUID is generated.</p>
 *
 * <p>The request ID is also set as a response header ({@code X-Request-Id}) so
 * clients can correlate responses with their requests.</p>
 *
 * <p>Registered with {@link Ordered#HIGHEST_PRECEDENCE} to ensure it runs before
 * all other filters, including Spring Security.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}



