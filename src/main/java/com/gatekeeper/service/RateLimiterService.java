package com.gatekeeper.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Token bucket rate limiter. Each API key gets a bucket that holds up to
 * {@code limit} tokens and refills continuously over one hour. The check and
 * the update run in a single Lua script, so concurrent requests on the same
 * key can never overspend the bucket.
 */
@Service
public class RateLimiterService {

    static final Duration REFILL_WINDOW = Duration.ofHours(1);

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> TOKEN_BUCKET = new DefaultRedisScript<>();

    static {
        TOKEN_BUCKET.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
        TOKEN_BUCKET.setResultType(List.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    @Autowired
    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this(redisTemplate, Clock.systemUTC());
    }

    RateLimiterService(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    public RateLimitDecision tryConsume(String apiKey, int limit) {
        List<?> result = redisTemplate.execute(
                TOKEN_BUCKET,
                List.of("rate:" + apiKey),
                String.valueOf(limit),
                String.valueOf(REFILL_WINDOW.toMillis()),
                String.valueOf(clock.millis()));

        boolean allowed = ((Number) result.get(0)).longValue() == 1;
        int remaining = ((Number) result.get(1)).intValue();
        long retryAfterMs = ((Number) result.get(2)).longValue();
        long resetMs = ((Number) result.get(3)).longValue();

        return new RateLimitDecision(allowed, limit, remaining, toSeconds(retryAfterMs), toSeconds(resetMs));
    }

    private static long toSeconds(long millis) {
        return (millis + 999) / 1000;
    }
}
