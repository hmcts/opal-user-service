package uk.gov.hmcts.reform.opal.service.synchronise;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleMappingCacheLookupServiceTest {

    private static final String TOKEN_SUBJECT = "subject-123";
    private static final String CACHE_KEY = "ROLE_MAPPING_USER_" + TOKEN_SUBJECT;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RoleMappingCacheLookupService roleMappingCacheLookupService;

    @BeforeEach
    void setUp() {
        roleMappingCacheLookupService = new RoleMappingCacheLookupService(redisTemplate, new ObjectMapper());
    }

    @Test
    void getRoleMappingByTokenSubject_returnsNumericRoleMap() throws Exception {

        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CACHE_KEY)).thenReturn("{\"101\":[\"7\",\"8\"],\"202\":[\"9\"]}");

        // Act
        Map<Long, Set<Short>> result = roleMappingCacheLookupService.getRoleMappingByTokenSubject(TOKEN_SUBJECT);

        // Assert
        assertEquals(
            Map.of(
                101L, Set.of((short) 7, (short) 8),
                202L, Set.of((short) 9)
            ),
            result
        );
        verify(valueOperations).get(CACHE_KEY);
    }

    @Test
    void getRoleMappingByTokenSubject_throwsLegacyRefreshException_whenCachePayloadIsNotJson() {

        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CACHE_KEY)).thenReturn("invalid-json");

        // Act / Assert
        assertThrows(
            RoleMappingCacheLookupException.class,
            () -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(TOKEN_SUBJECT)
        );
    }

    @Test
    void getRoleMappingByTokenSubject_throwsLegacyRefreshException_whenRoleIdsAreNotNumeric() {

        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CACHE_KEY)).thenReturn("{\"role-1\":[\"7\"]}");

        // Act / Assert
        assertThrows(
            RoleMappingCacheLookupException.class,
            () -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(TOKEN_SUBJECT)
        );
    }

    @Test
    void getRoleMappingByTokenSubject_throwsLegacyRefreshException_whenBusinessUnitIdsAreNotNumeric() {

        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CACHE_KEY)).thenReturn("{\"101\":[\"business-unit-7\"]}");

        // Act / Assert
        assertThrows(
            RoleMappingCacheLookupException.class,
            () -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(TOKEN_SUBJECT)
        );
    }
}
