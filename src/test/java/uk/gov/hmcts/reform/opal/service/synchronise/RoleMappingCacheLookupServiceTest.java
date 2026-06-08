package uk.gov.hmcts.reform.opal.service.synchronise;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.RoleService;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleMappingCacheService;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleMappingCacheLookupServiceTest {

    private static final long USER_ID = 123L;
    private static final String TOKEN_SUBJECT = "subject-123";
    private static final Type ROLE_MAPPING_CACHE_TYPE =
        new TypeReference<Map<String, Set<String>>>() { }.getType();
    private static final String SYNC_STAGE = "parse role mapping cache";
    private static final String PAYLOAD_NULL_REASON = "payload resolved to null";
    private static final String PARSE_JSON_REASON = "could not parse JSON";
    private static final String UNEXPECTED_RUNTIME_EXCEPTION_REASON = "unexpected runtime exception";

    @Mock
    private UserRoleMappingCacheService userRoleMappingCacheService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleMappingCacheLookupService roleMappingCacheLookupService;

    @Test
    void getRoleMappingByTokenSubject_throwsUserMissingFromCacheException_whenCachePayloadIsMissing() {

        // Arrange
        when(userRoleMappingCacheService.getUserMapping(TOKEN_SUBJECT)).thenReturn(null);

        // Act / Assert
        UserMissingFromCacheException exception = assertThrows(
            UserMissingFromCacheException.class,
            () -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(user())
        );
        assertEquals("Nothing in cache for : " + TOKEN_SUBJECT, exception.getMessage());
    }

    // happy path
    @Test
    void getRoleMappingByTokenSubject_returnsNumericRoleMap() throws Exception {

        // Arrange
        String cachePayload = "{\"101\":[\"7\",\"8\"],\"202\":[\"9\"]}";
        Map<String, Set<String>> cacheMap = Map.of(
            "101", Set.of("7", "8"),
            "202", Set.of("9")
        );
        when(userRoleMappingCacheService.getUserMapping(TOKEN_SUBJECT)).thenReturn(cachePayload);
        when(objectMapper.readValue(eq(cachePayload), roleMappingCacheTypeReference())).thenReturn(cacheMap);
        when(roleService.roleExists(101L)).thenReturn(true);
        when(roleService.roleExists(202L)).thenReturn(true);

        // Act
        Map<Long, Set<Short>> result = roleMappingCacheLookupService.getRoleMappingByTokenSubject(user());

        // Assert
        assertEquals(
            Map.of(
                101L, Set.of((short) 7, (short) 8),
                202L, Set.of((short) 9)
            ),
            result
        );
        verify(userRoleMappingCacheService).getUserMapping(TOKEN_SUBJECT);
    }

    @Test
    void getRoleMappingByTokenSubject_throwsLegacyRefreshException_whenCachePayloadIsJsonNull() throws Exception {

        // Arrange
        String cachePayload = "null";
        when(userRoleMappingCacheService.getUserMapping(TOKEN_SUBJECT)).thenReturn(cachePayload);
        when(objectMapper.readValue(eq(cachePayload), roleMappingCacheTypeReference())).thenReturn(null);

        // Act / Assert
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(user())
        );
        assertEquals(errorMessage(PAYLOAD_NULL_REASON), exception.getMessage());
    }

    @Test
    void getRoleMappingByTokenSubject_throwsSynchronisePermissionsException_whenCachePayloadIsInvalidJson()
        throws Exception {

        // Arrange
        String cachePayload = "invalid-json";
        JacksonException jsonProcessingException = new JacksonException("invalid-json") {
        };
        when(userRoleMappingCacheService.getUserMapping(TOKEN_SUBJECT)).thenReturn(cachePayload);
        when(objectMapper.readValue(eq(cachePayload), roleMappingCacheTypeReference()))
            .thenThrow(jsonProcessingException);

        // Act / Assert
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(user())
        );
        assertEquals(errorMessage(PARSE_JSON_REASON), exception.getMessage());
        assertSame(jsonProcessingException, exception.getCause());
    }

    @Test
    void getRoleMappingByTokenSubject_throwsSynchronisePermissionsException_whenOMThrowsJacksonException()
        throws Exception {

        // Arrange
        String cachePayload = "{\"101\":[\"7\"]}";
        JacksonException jsonProcessingException = new JacksonException("boom") {
        };
        when(userRoleMappingCacheService.getUserMapping(TOKEN_SUBJECT)).thenReturn(cachePayload);
        when(objectMapper.readValue(eq(cachePayload), roleMappingCacheTypeReference()))
            .thenThrow(jsonProcessingException);

        // Act
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(user())
        );

        // Assert
        assertEquals(errorMessage(PARSE_JSON_REASON), exception.getMessage());
        assertSame(jsonProcessingException, exception.getCause());
    }

    @Test
    void getRoleMappingByTokenSubject_throwsUserMissingFromCacheException_whenCachePayloadIsBlank() {

        // Arrange
        when(userRoleMappingCacheService.getUserMapping(TOKEN_SUBJECT)).thenReturn("   ");

        // Act / Assert
        UserMissingFromCacheException exception = assertThrows(
            UserMissingFromCacheException.class,
            () -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(user())
        );
        assertEquals("Nothing in cache for : " + TOKEN_SUBJECT, exception.getMessage());
    }

    @Test
    void getRoleMappingByTokenSubject_returnsEmptyBusinessUnitSet_whenRoleBusinessUnitSetIsNull() throws Exception {

        // Arrange
        String cachePayload = "{\"101\":null}";
        Map<String, Set<String>> cacheMap = new java.util.HashMap<>();
        cacheMap.put("101", null);
        when(userRoleMappingCacheService.getUserMapping(TOKEN_SUBJECT)).thenReturn(cachePayload);
        when(objectMapper.readValue(eq(cachePayload), roleMappingCacheTypeReference())).thenReturn(cacheMap);
        when(roleService.roleExists(101L)).thenReturn(true);

        // Act
        Map<Long, Set<Short>> result = roleMappingCacheLookupService.getRoleMappingByTokenSubject(user());

        // Assert
        assertEquals(Map.of(101L, Set.of()), result);
    }

    @Test
    void getRoleMappingByTokenSubject_skipsInvalidRolesAndKeepsValidRoles() throws Exception {

        // Arrange
        String cachePayload = "{\"101\":[\"7\"],\"999\":[\"8\"],\"202\":[\"9\"]}";
        Map<String, Set<String>> cacheMap = Map.of(
            "101", Set.of("7"),
            "999", Set.of("8"),
            "202", Set.of("9")
        );
        when(userRoleMappingCacheService.getUserMapping(TOKEN_SUBJECT)).thenReturn(cachePayload);
        when(objectMapper.readValue(eq(cachePayload), roleMappingCacheTypeReference())).thenReturn(cacheMap);
        when(roleService.roleExists(101L)).thenReturn(true);
        when(roleService.roleExists(202L)).thenReturn(true);
        when(roleService.roleExists(999L)).thenReturn(false);

        // Act
        Map<Long, Set<Short>> result = roleMappingCacheLookupService.getRoleMappingByTokenSubject(user());

        // Assert
        assertEquals(Map.of(101L, Set.of((short) 7), 202L, Set.of((short) 9)), result);
        verify(userRoleMappingCacheService).getUserMapping(TOKEN_SUBJECT);
    }

    @Test
    void getRoleMappingByTokenSubject_throwsLegacyRefreshException_whenRoleIdsAreNotNumeric() throws Exception {

        // Arrange
        String cachePayload = "{\"role-1\":[\"7\"]}";
        Map<String, Set<String>> cacheMap = Map.of("role-1", Set.of("7"));
        when(userRoleMappingCacheService.getUserMapping(TOKEN_SUBJECT)).thenReturn(cachePayload);
        when(objectMapper.readValue(eq(cachePayload), roleMappingCacheTypeReference())).thenReturn(cacheMap);

        // Act / Assert
        assertThrows(
            SynchronisePermissionsException.class,
            () -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(user())
        );
    }

    @Test
    void getRoleMappingByTokenSubject_throwsLegacyRefreshException_whenBusinessUnitIdsAreNotNumeric()
        throws Exception {

        // Arrange
        String cachePayload = "{\"101\":[\"business-unit-7\"]}";
        Map<String, Set<String>> cacheMap = Map.of("101", Set.of("business-unit-7"));
        when(userRoleMappingCacheService.getUserMapping(TOKEN_SUBJECT)).thenReturn(cachePayload);
        when(objectMapper.readValue(eq(cachePayload), roleMappingCacheTypeReference())).thenReturn(cacheMap);
        when(roleService.roleExists(101L)).thenReturn(true);

        // Act / Assert
        assertThrows(
            SynchronisePermissionsException.class,
            () -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(user())
        );
    }

    @Test
    void getRoleMappingByTokenSubject_throwsSynchronisePermissionsException_whenUnexpectedRuntimeExceptionOccurs() {

        // Arrange
        RuntimeException runtimeException = new RuntimeException("redis boom");
        when(userRoleMappingCacheService.getUserMapping(TOKEN_SUBJECT)).thenThrow(runtimeException);

        // Act
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(user())
        );

        // Assert
        assertEquals(errorMessage(UNEXPECTED_RUNTIME_EXCEPTION_REASON), exception.getMessage());
        assertSame(runtimeException, exception.getCause());
    }

    private TypeReference<Map<String, Set<String>>> roleMappingCacheTypeReference() {
        return ArgumentMatchers.argThat(typeReference ->
            typeReference != null && ROLE_MAPPING_CACHE_TYPE.equals(typeReference.getType())
        );
    }

    private UserEntity user() {
        return UserEntity.builder()
            .userId(USER_ID)
            .tokenSubject(TOKEN_SUBJECT)
            .build();
    }

    private String errorMessage(String reason) {
        return "Could not synchronise permissions for user " + USER_ID
            + " at stage: " + SYNC_STAGE
            + ". Reason: " + reason;
    }
}
