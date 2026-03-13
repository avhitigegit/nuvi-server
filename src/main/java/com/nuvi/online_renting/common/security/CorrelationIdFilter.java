package com.nuvi.online_renting.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a unique correlation ID to every incoming HTTP request.
 *
 * Behaviour:
 *   1. Reads X-Request-ID from the incoming request header.
 *      If the client did not send one, a fresh UUID is generated.
 *   2. Stores it in SLF4J MDC under the key "reqId" so it appears in
 *      every log line written during that request.
 *   3. Echoes it back in the response header X-Request-ID so the
 *      frontend / API client can reference it when reporting issues.
 *   4. Clears the MDC entry in the finally block — prevents leaking
 *      the ID into the next request on the same thread (thread pool reuse).
 *
 * Usage in logs:
 *   Search for a specific request:  grep "reqId=a1b2c3d4" app.log
 *   All lines for that request appear regardless of which class logged them.
 */
@Component
@Order(1) // run before all other filters so every log line gets the ID
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_KEY           = "reqId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            // Always clear — thread pool reuse would otherwise carry the ID into the next request
            MDC.remove(MDC_KEY);
        }
    }
}
