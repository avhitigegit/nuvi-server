package com.nuvi.online_renting.common.ratelimit;

/**
 * Abstraction over the rate-limit counter store.
 *
 * Two implementations are provided:
 *   InMemoryRateLimiterStore — Bucket4j in-process map (dev / single-instance)
 *   RedisRateLimiterStore    — Redis INCR + EXPIRE  (prod / multi-instance)
 *
 * Switch via application.properties:
 *   app.ratelimit.provider=memory   (default)
 *   app.ratelimit.provider=redis
 */
public interface RateLimiterStore {

    /**
     * Attempt to consume one token for the given client IP and endpoint path.
     *
     * @return true  — request is within the allowed rate, let it through
     *         false — rate limit exceeded, return 429
     */
    boolean tryConsume(String ip, String path);
}
