package com.gatekeeper.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    public boolean isAllowed(String apiKey, int limit) {
        String key = "rate:" + apiKey;
        String countStr = redisTemplate.opsForValue().get(key);

        if (countStr == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofHours(1));
            return true;
        }

        int currentCount = Integer.parseInt(countStr);
        if (currentCount >= limit) {
            return false;
        }

        redisTemplate.opsForValue().increment(key);
        return true;
    }

    public int getRemainingRequests(String apiKey, int limit) {
        String key = "rate:" + apiKey;
        String countStr = redisTemplate.opsForValue().get(key);

        if (countStr == null) {
            return limit;
        }

        int used = Integer.parseInt(countStr);
        return Math.max(0, limit - used);
    }

    public long getResetTimeSeconds(String apiKey) {
        String key = "rate:" + apiKey;
        Long ttl = redisTemplate.getExpire(key);
        if (ttl == null || ttl < 0) {
            return 3600;
        }
        return ttl;
    }
}
