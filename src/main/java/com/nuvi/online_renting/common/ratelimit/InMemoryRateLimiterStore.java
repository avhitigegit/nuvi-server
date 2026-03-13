package com.nuvi.online_renting.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter backed by Bucket4j.
 * Active when app.ratelimit.provider=memory (default).
 *
 * Suitable for development and single-instance deployments.
 * In a multi-instance deployment, each server maintains its own counters,
 * so a determined user could bypass limits by hitting different instances.
 * Use RedisRateLimiterStore in production to share counters across all instances.
 */
@Component
@ConditionalOnProperty(name = "app.ratelimit.provider", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimiterStore implements RateLimiterStore {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW    = Duration.ofMinutes(1);

    // Key: "ip:path" — one bucket per unique IP + endpoint combination
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String ip, String path) {
        String key = ip + ":" + path;
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());
        return bucket.tryConsume(1);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_REQUESTS)
                .refillIntervally(MAX_REQUESTS, WINDOW)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
