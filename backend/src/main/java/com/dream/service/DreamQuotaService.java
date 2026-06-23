package com.dream.service;

import com.dream.common.exception.BusinessException;
import com.dream.common.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DreamQuotaService {

    private static final Logger log = LoggerFactory.getLogger(DreamQuotaService.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final StringRedisTemplate redisTemplate;
    private final int dailyFreeLimit;

    public DreamQuotaService(
            StringRedisTemplate redisTemplate,
            @Value("${dream.interpret.daily-free-limit:5}") int dailyFreeLimit) {
        this.redisTemplate = redisTemplate;
        this.dailyFreeLimit = dailyFreeLimit;
    }

    public void consumeDailyFreeQuota(Long userId) {
        if (dailyFreeLimit <= 0) {
            return;
        }
        String key = "quota:dream:interpret:daily:" + LocalDate.now(BUSINESS_ZONE) + ":user:" + userId;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (Long.valueOf(1).equals(count)) {
                redisTemplate.expire(key, ttlUntilTomorrow());
            }
            if (count != null && count > dailyFreeLimit) {
                throw new BusinessException(ErrorCode.RATE_LIMITED, "daily free quota exceeded");
            }
        } catch (RedisConnectionFailureException | RedisSystemException | QueryTimeoutException exception) {
            log.warn("Redis daily quota unavailable, allowing request for userId={}", userId);
        }
    }

    private Duration ttlUntilTomorrow() {
        ZonedDateTime now = ZonedDateTime.now(BUSINESS_ZONE);
        ZonedDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay(BUSINESS_ZONE);
        return Duration.between(now, tomorrow);
    }
}
