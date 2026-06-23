package com.dream.common.ratelimit;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(String key, int limit, Duration window) {
        if (limit <= 0) {
            return true;
        }
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (Long.valueOf(1).equals(count)) {
                redisTemplate.expire(key, window);
            }
            return count == null || count <= limit;
        } catch (RedisConnectionFailureException | RedisSystemException | QueryTimeoutException exception) {
            log.warn("Redis rate limiter unavailable, allowing request for key={}", key);
            return true;
        }
    }
}
