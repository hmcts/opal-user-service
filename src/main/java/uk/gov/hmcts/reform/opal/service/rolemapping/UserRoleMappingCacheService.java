package uk.gov.hmcts.reform.opal.service.rolemapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRoleMappingCacheService {

    public static final String ROLE_MAPPING_USER_PREFIX = "ROLE_MAPPING_USER_";
    public static final String USER_MAPPING_FILE_LAST_UPDATE_AT = "USER_MAPPING_FILE_LAST_UPDATE_AT";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------
    // USER MAPPING CACHE
    // -------------------------

    public void putUserMapping(String cacheKey, Object payload, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(cacheKey, json, ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize mapping for " + cacheKey, e);
        }
    }

    public void deleteUserKey(String cacheKey) {
        redisTemplate.delete(cacheKey);
    }

    public void deleteStaleUserKeys(Set<String> refreshedKeys) {
        Set<String> existingKeys = scanKeys(ROLE_MAPPING_USER_PREFIX + "*");
        Set<String> staleKeys = new LinkedHashSet<>();

        for (String key : existingKeys) {
            if (!refreshedKeys.contains(key)) {
                staleKeys.add(key);
            }
        }

        if (!staleKeys.isEmpty()) {
            redisTemplate.delete(staleKeys);
        }
    }

    // -------------------------
    // LAST UPDATE TRACKING
    // -------------------------

    public boolean hasLastUpdateAt() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(USER_MAPPING_FILE_LAST_UPDATE_AT));
    }

    public String getLastUpdateAt() {
        return redisTemplate.opsForValue().get(USER_MAPPING_FILE_LAST_UPDATE_AT);
    }

    public void setLastUpdateAt(String value, Duration ttl) {
        redisTemplate.opsForValue().set(USER_MAPPING_FILE_LAST_UPDATE_AT, value, ttl);
    }

    // -------------------------
    // TTL REFRESH
    // -------------------------

    public void refreshAllTtls(Duration userTtl, Duration lastUpdateTtl) {
        for (String key : scanKeys(ROLE_MAPPING_USER_PREFIX + "*")) {
            redisTemplate.expire(key, userTtl);
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(USER_MAPPING_FILE_LAST_UPDATE_AT))) {
            redisTemplate.expire(USER_MAPPING_FILE_LAST_UPDATE_AT, lastUpdateTtl);
        }
    }

    // -------------------------
    // INTERNAL SCAN
    // -------------------------

    private Set<String> scanKeys(String pattern) {
        RedisCallback<Set<String>> callback = connection -> {
            Set<String> keys = new LinkedHashSet<>();

            ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();

            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }

            return keys;
        };

        return redisTemplate.execute(callback);
    }
}
