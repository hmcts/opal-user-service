package uk.gov.hmcts.reform.opal.service.rolemapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

class UserRoleMappingCacheServiceTest {

    private final StringRedisTemplate redisTemplate =
        Mockito.mock(StringRedisTemplate.class, Mockito.RETURNS_DEEP_STUBS);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UserRoleMappingCacheService cacheService =
        new UserRoleMappingCacheService(redisTemplate);

    @Test
    void putUserMappingSerializesAndStoresJsonWithTtl() {
        var payload = new LinkedHashMap<String, Set<String>>();
        payload.put("BU1", new LinkedHashSet<>(java.util.List.of("R1", "R2")));
        payload.put("BU2", new LinkedHashSet<>(java.util.List.of("R3")));

        cacheService.putUserMapping(
            "ROLE_MAPPING_USER_AS1",
            payload,
            Duration.ofMinutes(30)
        );

        verify(redisTemplate.opsForValue()).set(
            "ROLE_MAPPING_USER_AS1",
            "{\"BU1\":[\"R1\",\"R2\"],\"BU2\":[\"R3\"]}",
            Duration.ofMinutes(30)
        );
    }

    @Test
    void deleteUserKeyDelegatesToRedis() {
        cacheService.deleteUserKey("ROLE_MAPPING_USER_AS1");

        verify(redisTemplate).delete("ROLE_MAPPING_USER_AS1");
    }

    @Test
    void setAndGetLastUpdateAtDelegatesToRedis() {
        cacheService.setLastUpdateAt("2025-01-02T03:04:05.678", Duration.ofHours(1));

        verify(redisTemplate.opsForValue()).set(
            "USER_MAPPING_FILE_LAST_UPDATE_AT",
            "2025-01-02T03:04:05.678",
            Duration.ofHours(1)
        );

        when(redisTemplate.opsForValue().get("USER_MAPPING_FILE_LAST_UPDATE_AT"))
            .thenReturn("2025-01-02T03:04:05.678");

        assertEquals("2025-01-02T03:04:05.678", cacheService.getLastUpdateAt());
    }

    @Test
    void refreshAllTtlsExpiresUserKeysAndLastUpdateKey() {
        when(redisTemplate.hasKey("USER_MAPPING_FILE_LAST_UPDATE_AT")).thenReturn(true);
        stubScanKeys("ROLE_MAPPING_USER_AS1", "ROLE_MAPPING_USER_AS2");

        cacheService.refreshAllTtls(Duration.ofHours(24), Duration.ofHours(1));

        verify(redisTemplate).expire("ROLE_MAPPING_USER_AS1", Duration.ofHours(24));
        verify(redisTemplate).expire("ROLE_MAPPING_USER_AS2", Duration.ofHours(24));
        verify(redisTemplate).expire("USER_MAPPING_FILE_LAST_UPDATE_AT", Duration.ofHours(1));
    }

    @Test
    void deleteStaleUserKeysDeletesOnlyKeysNotRefreshed() {
        stubScanKeys("ROLE_MAPPING_USER_AS1", "ROLE_MAPPING_USER_AS2", "ROLE_MAPPING_USER_AS3");

        cacheService.deleteStaleUserKeys(Set.of("ROLE_MAPPING_USER_AS1", "ROLE_MAPPING_USER_AS3"));

        verify(redisTemplate).delete(argThat((Collection<String> keys) ->
                                                 keys.size() == 1 && keys.contains("ROLE_MAPPING_USER_AS2")
        ));
    }

    private void stubScanKeys(String... keys) {
        RedisConnection connection = Mockito.mock(RedisConnection.class, Mockito.RETURNS_DEEP_STUBS);
        Cursor<byte[]> cursor = new ByteArrayCursor(keys);

        when(connection.keyCommands().scan(any(ScanOptions.class))).thenReturn(cursor);

        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RedisCallback<Set<String>> callback = invocation.getArgument(0);
            return callback.doInRedis(connection);
        }).when(redisTemplate).execute(Mockito.<RedisCallback<Set<String>>>any());
    }

    private static final class ByteArrayCursor implements Cursor<byte[]> {

        private final java.util.List<byte[]> values;
        private final AtomicInteger index = new AtomicInteger(0);

        private ByteArrayCursor(String... keys) {
            this.values = java.util.Arrays.stream(keys)
                .map(key -> key.getBytes(StandardCharsets.UTF_8))
                .toList();
        }

        @Override
        public boolean hasNext() {
            return index.get() < values.size();
        }

        @Override
        public byte[] next() {
            return values.get(index.getAndIncrement());
        }

        @Override
        public void close() {
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public CursorId getId() {
            return null;
        }

        @Override
        public long getCursorId() {
            return 0;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public long getPosition() {
            return 0;
        }
    }
}
