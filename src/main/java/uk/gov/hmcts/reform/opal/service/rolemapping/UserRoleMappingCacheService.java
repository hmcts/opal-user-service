package uk.gov.hmcts.reform.opal.service.rolemapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRoleMappingCacheService {

    private static final String ROLE_MAPPING_USER_PREFIX = "ROLE_MAPPING_USER_";
    private static final String USER_MAPPING_FILE_LAST_UPDATE_AT = "USER_MAPPING_FILE_LAST_UPDATE_AT";

    private final StringRedisTemplate redisTemplate;
    private final RoleMappingCacheProperties properties;
    private final ObjectMapper objectMapper;


    // -------------------------
    // USER MAPPING CACHE
    // -------------------------

    public void putUserMapping(String tokenSubject, Object payload) {
        write(buildUserKey(tokenSubject), payload, properties.getUserTtl());
    }

    public void deleteUserMapping(String tokenSubject) {
        redisTemplate.delete(buildUserKey(tokenSubject));
    }

    public void deleteStaleUserMappings(Set<String> refreshedSubjects) {
        Set<String> existingKeys = scanKeys(ROLE_MAPPING_USER_PREFIX + "*");

        if (existingKeys.isEmpty()) {
            return;
        }

        Set<String> refreshedKeys = new LinkedHashSet<>();
        for (String subject : refreshedSubjects) {
            refreshedKeys.add(buildUserKey(subject));
        }

        Set<String> staleKeys = new LinkedHashSet<>(existingKeys);
        staleKeys.removeAll(refreshedKeys);

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

    public void setLastUpdateAt(String value) {
        redisTemplate.opsForValue().set(
            USER_MAPPING_FILE_LAST_UPDATE_AT,
            value,
            properties.getLastUpdateTtl()
        );
    }

    // -------------------------
    // TTL REFRESH
    // -------------------------

    public void refreshAllTtls() {
        for (String key : scanKeys(ROLE_MAPPING_USER_PREFIX + "*")) {
            redisTemplate.expire(key, properties.getUserTtl());
        }

        if (hasLastUpdateAt()) {
            redisTemplate.expire(
                USER_MAPPING_FILE_LAST_UPDATE_AT,
                properties.getLastUpdateTtl()
            );
        }
    }

    // -------------------------
    // INTERNALS
    // -------------------------

    private String buildUserKey(String tokenSubject) {
        return ROLE_MAPPING_USER_PREFIX + tokenSubject;
    }

    private void write(String cacheKey, Object payload, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(cacheKey, json, ttl);

        } catch (DataAccessException e) {
            throw new IllegalStateException(
                "Redis write failed for key " + cacheKey,
                e
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                "Failed to serialize mapping for key " + cacheKey,
                e
            );
        }
    }

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
