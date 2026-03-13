package com.nuvi.online_renting.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces a maximum request body size for JSON/REST endpoints.
 *
 * Why this is needed:
 *   Without a limit, a malicious client can send a very large JSON payload
 *   (hundreds of MB) to any endpoint, consuming CPU for parsing and memory
 *   for deserialization — a low-effort denial-of-service attack.
 *
 * What is limited:
 *   All requests where Content-Type is NOT multipart/form-data.
 *   Multipart (file upload) endpoints are excluded — they are governed
 *   by spring.servlet.multipart.max-file-size (50MB) instead.
 *
 * Default limit: 512KB — generous enough for any realistic JSON payload
 *   in this API (the largest body is a seller application at ~1KB).
 *   Configure via: app.request.max-body-size-bytes
 *
 * Note on chunked encoding:
 *   If the client omits the Content-Length header (chunked transfer),
 *   this filter cannot check the size upfront. Tomcat will still reject
 *   bodies larger than server.tomcat.max-swallow-size (configured below).
 */
@Component
@Order(2) // runs after CorrelationIdFilter (Order 1) so req-id is in MDC
public class RequestBodySizeFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestBodySizeFilter.class);

    @Value("${app.request.max-body-size-bytes:524288}") // 512 KB default
    private long maxBodySizeBytes;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String contentType = request.getContentType();
        boolean isMultipart = contentType != null
                && contentType.toLowerCase().startsWith("multipart/");

        // Multipart requests have their own size limits — skip them here
        if (!isMultipart) {
            long contentLength = request.getContentLengthLong();
            if (contentLength > maxBodySizeBytes) {
                log.warn("Request body too large: {} bytes (max {} bytes) — [req-id:{}] {} {}",
                        contentLength, maxBodySizeBytes,
                        MDC.get(CorrelationIdFilter.MDC_KEY),
                        request.getMethod(), request.getRequestURI());

                response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Request body is too large. Maximum allowed size is 512KB.\"}"
                );
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
