package com.nuvi.online_renting.common.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Distributed rate limiter backed by Redis.
 * Active when app.ratelimit.provider=redis.
 *
 * Uses the Redis INCR + EXPIRE pattern:
 *   1. INCR rate_limit:{ip}:{path}   — atomically increment the counter
 *   2. If count == 1 (first request in window): set TTL to 60 seconds
 *   3. If count > MAX_REQUESTS: reject with 429
 *
 * Because INCR is atomic in Redis, this is safe under concurrent load and
 * works correctly across multiple application instances sharing the same Redis.
 *
 * Requires: app.ratelimit.provider=redis + Redis connection configured
 *   spring.data.redis.host=${REDIS_HOST}
 *   spring.data.redis.port=${REDIS_PORT:6379}
 *   spring.data.redis.password=${REDIS_PASSWORD:}
 *
 * AWS setup: use ElastiCache (Redis OSS) in the same VPC as your EC2 instance.
 */
@Component
@ConditionalOnProperty(name = "app.ratelimit.provider", havingValue = "redis")
public class RedisRateLimiterStore implements RateLimiterStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiterStore.class);

    private static final int      MAX_REQUESTS  = 5;
    private static final Duration WINDOW        = Duration.ofMinutes(1);
    private static final String   KEY_PREFIX    = "rate_limit:";

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiterStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryConsume(String ip, String path) {
        String key = KEY_PREFIX + ip + ":" + path;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                // Redis returned unexpected null — fail open to avoid blocking all traffic
                log.error("Redis returned null for INCR on key {}. Failing open.", key);
                return true;
            }
            if (count == 1) {
                // First request in this window — set the expiry
                redisTemplate.expire(key, WINDOW);
            }
            return count <= MAX_REQUESTS;
        } catch (Exception e) {
            // Redis unavailable — fail open so a Redis outage doesn't take down the API
            log.error("Redis rate-limit check failed for key {}: {}. Failing open.", key, e.getMessage());
            return true;
        }
    }
}
