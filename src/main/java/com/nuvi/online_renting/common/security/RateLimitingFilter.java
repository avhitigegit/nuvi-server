package com.nuvi.online_renting.common.security;

import com.nuvi.online_renting.common.ratelimit.RateLimiterStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Applies per-IP rate limiting to sensitive authentication endpoints.
 * Limit: 5 requests per minute per IP (configurable in RateLimiterStore implementations).
 *
 * The actual counter storage is delegated to RateLimiterStore:
 *   - InMemoryRateLimiterStore (app.ratelimit.provider=memory) — dev / single-instance
 *   - RedisRateLimiterStore    (app.ratelimit.provider=redis)  — prod / multi-instance
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final List<String> RATE_LIMITED_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/refresh"
    );

    private final RateLimiterStore rateLimiterStore;

    public RateLimitingFilter(RateLimiterStore rateLimiterStore) {
        this.rateLimiterStore = rateLimiterStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        boolean isRateLimited = RATE_LIMITED_PATHS.stream().anyMatch(path::equals);
        if (!isRateLimited) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);

        if (rateLimiterStore.tryConsume(ip, path)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP {} on {}", ip, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please wait a minute and try again.\"}"
            );
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may contain a chain of IPs — take the first (original client)
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
