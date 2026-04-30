package uk.gov.hmcts.reform.opal.service.rolemapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

class UserRoleMappingCacheServiceTest {

    private static final String PREFIX = "ROLE_MAPPING_USER_";

    private final StringRedisTemplate redisTemplate =
        Mockito.mock(StringRedisTemplate.class, Mockito.RETURNS_DEEP_STUBS);

    private final RoleMappingCacheProperties properties =
        Mockito.mock(RoleMappingCacheProperties.class);

    private final UserRoleMappingCacheService cacheService =
        new UserRoleMappingCacheService(redisTemplate, properties, new ObjectMapper());

    @Test
    void putUserMappingSerializesAndStoresJsonWithTtl() {

        // ARRANGE
        when(properties.getUserTtl()).thenReturn(Duration.ofMinutes(30));

        var payload = new LinkedHashMap<String, Set<String>>();
        payload.put("BU1", new LinkedHashSet<>(java.util.List.of("R1", "R2")));
        payload.put("BU2", new LinkedHashSet<>(java.util.List.of("R3")));

        // ACT
        cacheService.putUserMapping("AS1", payload);

        // ASSERT
        verify(redisTemplate.opsForValue()).set(
            PREFIX + "AS1",
            "{\"BU1\":[\"R1\",\"R2\"],\"BU2\":[\"R3\"]}",
            Duration.ofMinutes(30)
        );
    }

    @Test
    void deleteUserMappingDelegatesToRedis() {

        // ARRANGE
        String tokenSubject = "AS1";

        // ACT
        cacheService.deleteUserMapping(tokenSubject);

        // ASSERT
        verify(redisTemplate).delete(PREFIX + "AS1");
    }

    @Test
    void setAndGetLastUpdateAtDelegatesToRedis() {

        // ARRANGE
        when(properties.getLastUpdateTtl()).thenReturn(Duration.ofHours(1));

        String timestamp = "2025-01-02T03:04:05.678";

        // ACT
        cacheService.setLastUpdateAt(timestamp);

        // ASSERT
        verify(redisTemplate.opsForValue()).set(
            "USER_MAPPING_FILE_LAST_UPDATE_AT",
            timestamp,
            Duration.ofHours(1)
        );

        when(redisTemplate.opsForValue().get("USER_MAPPING_FILE_LAST_UPDATE_AT"))
            .thenReturn(timestamp);

        assertEquals(timestamp, cacheService.getLastUpdateAt());
    }

    @Test
    void refreshAllTtlsExpiresUserKeysAndLastUpdateKey() {

        // ARRANGE
        when(properties.getUserTtl()).thenReturn(Duration.ofHours(24));
        when(properties.getLastUpdateTtl()).thenReturn(Duration.ofHours(1));
        when(redisTemplate.hasKey("USER_MAPPING_FILE_LAST_UPDATE_AT")).thenReturn(true);

        stubScanKeys(PREFIX + "AS1", PREFIX + "AS2");

        // ACT
        cacheService.refreshAllTtls();

        // ASSERT
        verify(redisTemplate).expire(PREFIX + "AS1", Duration.ofHours(24));
        verify(redisTemplate).expire(PREFIX + "AS2", Duration.ofHours(24));
        verify(redisTemplate).expire("USER_MAPPING_FILE_LAST_UPDATE_AT", Duration.ofHours(1));
    }

    @Test
    void deleteStaleUserMappingsDeletesOnlyKeysNotRefreshed() {

        // ARRANGE
        stubScanKeys(
            PREFIX + "AS1",
            PREFIX + "AS2",
            PREFIX + "AS3"
        );

        // ACT
        cacheService.deleteStaleUserMappings(Set.of("AS1", "AS3"));

        // ASSERT
        verify(redisTemplate).delete(argThat((Collection<String> keys) ->
                                                 keys.size() == 1 && keys.contains(PREFIX + "AS2")
        ));
    }

    @Test
    void putUserMappingThrowsWhenSerializationFails() throws Exception {

        // ARRANGE
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        RoleMappingCacheProperties properties = Mockito.mock(RoleMappingCacheProperties.class);

        when(properties.getUserTtl()).thenReturn(Duration.ofMinutes(30));

        StringRedisTemplate redisTemplate =
            Mockito.mock(StringRedisTemplate.class, Mockito.RETURNS_DEEP_STUBS);

        UserRoleMappingCacheService cacheService =
            new UserRoleMappingCacheService(redisTemplate, properties, objectMapper);

        when(objectMapper.writeValueAsString(any()))
            .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});

        // ACT
        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> cacheService.putUserMapping("AS1", Map.of())
        );

        // ASSERT
        assertEquals(
            "Failed to serialize mapping for key ROLE_MAPPING_USER_AS1",
            exception.getMessage()
        );

        assertTrue(exception.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException);

        verify(redisTemplate.opsForValue(), never()).set(any(), any(), any());
    }

    // ---------------------------------------
    // Redis SCAN mocking
    // ---------------------------------------

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
