package com.dream.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DreamResultCacheService {

    private static final Logger log = LoggerFactory.getLogger(DreamResultCacheService.class);
    private static final String KEY_PREFIX = "cache:dream:interpret:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration cacheTtl;

    public DreamResultCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${dream.interpret.cache-ttl-seconds:86400}") long cacheTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
    }

    public Optional<JsonNode> get(String dreamText, List<String> tags, String school) {
        try {
            String value = redisTemplate.opsForValue().get(key(dreamText, tags, school));
            if (!StringUtils.hasText(value)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(value));
        } catch (RedisConnectionFailureException | RedisSystemException | QueryTimeoutException exception) {
            log.warn("Redis dream result cache get failed");
            return Optional.empty();
        } catch (JsonProcessingException exception) {
            log.warn("Cached dream result JSON is invalid");
            return Optional.empty();
        }
    }

    public void put(String dreamText, List<String> tags, String school, JsonNode result) {
        if (cacheTtl.isZero() || cacheTtl.isNegative() || result == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    key(dreamText, tags, school),
                    objectMapper.writeValueAsString(result),
                    cacheTtl);
        } catch (RedisConnectionFailureException | RedisSystemException | QueryTimeoutException exception) {
            log.warn("Redis dream result cache put failed");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize dream result cache value", exception);
        }
    }

    private String key(String dreamText, List<String> tags, String school) {
        CacheInput input = new CacheInput(
                normalize(dreamText),
                tags == null ? List.of() : tags,
                normalize(school));
        try {
            String canonical = objectMapper.writeValueAsString(input);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return KEY_PREFIX + HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("failed to build dream result cache key", exception);
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private record CacheInput(String dreamText, List<String> tags, String school) {
    }
}
