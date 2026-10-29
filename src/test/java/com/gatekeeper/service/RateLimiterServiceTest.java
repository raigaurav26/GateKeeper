package com.gatekeeper.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the Lua token bucket against a real Redis (localhost:6379 by default,
 * override with REDIS_HOST / REDIS_PORT). CI starts one as a service container.
 */
class RateLimiterServiceTest {

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    private MutableClock clock;
    private RateLimiterService rateLimiter;
    private String apiKey;

    @BeforeAll
    static void connect() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);

        boolean redisUp;
        try {
            redisUp = "PONG".equals(redisTemplate.execute(c -> c.ping(), true));
        } catch (Exception e) {
            redisUp = false;
        }
        assumeTrue(redisUp, "Redis is not reachable at " + host + ":" + port);
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        rateLimiter = new RateLimiterService(redisTemplate, clock);
        apiKey = "test-" + UUID.randomUUID();
    }

    @Test
    void allowsRequestsUpToTheLimitThenRejects() {
        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = rateLimiter.tryConsume(apiKey, 5);
            assertThat(decision.allowed()).isTrue();
            assertThat(decision.remaining()).isEqualTo(4 - i);
        }

        RateLimitDecision rejected = rateLimiter.tryConsume(apiKey, 5);
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.remaining()).isZero();
    }

    @Test
    void retryAfterIsTimeUntilNextToken() {
        drain(apiKey, 60);

        // 60 tokens per hour refills one token every 60 seconds
        RateLimitDecision rejected = rateLimiter.tryConsume(apiKey, 60);
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isEqualTo(60);
        assertThat(rejected.resetSeconds()).isEqualTo(3600);
    }

    @Test
    void handlesLargeTierLimits() {
        int enterprise = 10_000;
        RateLimitDecision first = rateLimiter.tryConsume(apiKey, enterprise);

        assertThat(first.allowed()).isTrue();
        assertThat(first.remaining()).isEqualTo(enterprise - 1);
        assertThat(first.resetSeconds()).isEqualTo(1);
    }

    @Test
    void refillsGraduallyInsteadOfResettingAtOnce() {
        drain(apiKey, 60);

        clock.advance(Duration.ofSeconds(59));
        assertThat(rateLimiter.tryConsume(apiKey, 60).allowed()).isFalse();

        clock.advance(Duration.ofSeconds(1));
        assertThat(rateLimiter.tryConsume(apiKey, 60).allowed()).isTrue();
        assertThat(rateLimiter.tryConsume(apiKey, 60).allowed()).isFalse();
    }

    @Test
    void neverRefillsAboveCapacity() {
        rateLimiter.tryConsume(apiKey, 10);

        clock.advance(Duration.ofDays(1));

        assertThat(rateLimiter.tryConsume(apiKey, 10).remaining()).isEqualTo(9);
    }

    @Test
    void keysHaveSeparateBuckets() {
        drain(apiKey, 3);

        assertThat(rateLimiter.tryConsume(apiKey, 3).allowed()).isFalse();
        assertThat(rateLimiter.tryConsume(apiKey + "-other", 3).allowed()).isTrue();
    }

    @Test
    void concurrentRequestsCannotOverspendTheBucket() throws Exception {
        int limit = 50;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Callable<Boolean>> calls = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                calls.add(() -> rateLimiter.tryConsume(apiKey, limit).allowed());
            }
            int allowed = 0;
            for (Future<Boolean> f : pool.invokeAll(calls)) {
                if (f.get()) {
                    allowed++;
                }
            }
            assertThat(allowed).isEqualTo(limit);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void bucketKeyExpiresAfterTheRefillWindow() {
        rateLimiter.tryConsume(apiKey, 10);

        Long ttl = redisTemplate.getExpire("rate:" + apiKey);
        assertThat(ttl).isPositive().isLessThanOrEqualTo(RateLimiterService.REFILL_WINDOW.toSeconds());
    }

    private void drain(String key, int limit) {
        for (int i = 0; i < limit; i++) {
            assertThat(rateLimiter.tryConsume(key, limit).allowed()).isTrue();
        }
    }

    private static final class MutableClock extends Clock {
        private volatile Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
