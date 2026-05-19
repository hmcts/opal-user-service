package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.BusinessUnitUserService;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynchroniseRolesServiceTest {

    private static final String TOKEN_SUBJECT = "subject-123";
    private static final long USER_ID = 123L;
    private static final String SYNC_STAGE = "synchronise roles";
    private static final String PARSE_LEGACY_BUSINESS_UNIT_ID_REASON = "parse failed for legacy business unit id";
    private static final String UNEXPECTED_RUNTIME_EXCEPTION_REASON = "unexpected runtime exception";

    @Mock
    private UserService userService;

    @Mock
    private BusinessUnitUserService businessUnitUserService;

    @Mock
    private RoleMappingCacheLookupService roleMappingCacheLookupService;

    @InjectMocks
    private SynchroniseRolesService synchroniseRolesService;

    // happy path
    @Test
    void synchroniseRoles_deletesOnlyRolesMissingFromCachedRoleIds() throws Exception {

        // Arrange
        UserEntity user = UserEntity.builder().userId(USER_ID).tokenSubject(TOKEN_SUBJECT).build();
        when(roleMappingCacheLookupService.getRoleMappingByTokenSubject(user))
            .thenReturn(java.util.Map.of(101L, Set.of((short) 7)));

        RoleEntity roleStillInCache = RoleEntity.builder().roleId(101L).name("name-not-id").build();
        RoleEntity roleMissingFromCache = RoleEntity.builder().roleId(202L).name("101").build();
        when(businessUnitUserService.findAllRolesOfUser(user)).thenReturn(Set.of(roleStillInCache,
                                                                                 roleMissingFromCache));

        LegacyBusinessUnitUserId legacyBusinessUnitUser = LegacyBusinessUnitUserId.builder()
            .businessUnitId("7")
            .build();

        // Act
        synchroniseRolesService.synchroniseRoles(user, List.of(legacyBusinessUnitUser));

        // Assert
        verify(userService, never()).deleteRoleFromUser(user, 101L);
        verify(userService).deleteRoleFromUser(user, 202L);
    }

    @Test
    void synchroniseRoles_returnsEmptyValidatedRoleIdsAndDeletesExistingRoles_whenUserMissingFromCache()
        throws Exception {

        // Arrange
        UserEntity user = UserEntity.builder().userId(USER_ID).tokenSubject(TOKEN_SUBJECT).build();
        when(roleMappingCacheLookupService.getRoleMappingByTokenSubject(user))
            .thenThrow(new UserMissingFromCacheException("Nothing in cache for : " + TOKEN_SUBJECT));

        RoleEntity firstExistingRole = RoleEntity.builder().roleId(101L).name("role-101").build();
        RoleEntity secondExistingRole = RoleEntity.builder().roleId(202L).name("role-202").build();
        when(businessUnitUserService.findAllRolesOfUser(user)).thenReturn(Set.of(firstExistingRole,
                                                                                 secondExistingRole));

        // Act
        Set<Long> validatedRoleIds = synchroniseRolesService.synchroniseRoles(user, List.of());

        // Assert
        assertEquals(Set.of(), validatedRoleIds);
        verify(userService, never()).addOrReplaceRoleInformationOnUser(any(), anyLong(), anySet());
        verify(userService).deleteRoleFromUser(user, 101L);
        verify(userService).deleteRoleFromUser(user, 202L);
    }

    @Test
    void synchroniseRoles_throwsSynchronisePermissionsException_whenLegacyBusinessUnitIdIsNotNumeric()
        throws Exception {

        // Arrange
        UserEntity user = UserEntity.builder().userId(USER_ID).tokenSubject(TOKEN_SUBJECT).build();
        when(roleMappingCacheLookupService.getRoleMappingByTokenSubject(user))
            .thenReturn(java.util.Map.of(101L, Set.of((short) 7)));

        LegacyBusinessUnitUserId invalidLegacyBusinessUnitUser = LegacyBusinessUnitUserId.builder()
            .businessUnitId("not-a-number")
            .build();

        // Act / Assert
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> synchroniseRolesService.synchroniseRoles(user, List.of(invalidLegacyBusinessUnitUser))
        );
        assertEquals(
            errorMessage(SYNC_STAGE, PARSE_LEGACY_BUSINESS_UNIT_ID_REASON),
            exception.getMessage()
        );
        verify(userService, never()).addOrReplaceRoleInformationOnUser(any(), anyLong(), anySet());
        verify(businessUnitUserService, never()).findAllRolesOfUser(any());
        verify(userService, never()).deleteRoleFromUser(any(), anyLong());
    }

    @Test
    void synchroniseRoles_throwsSynchronisePermissionsException_whenUnexpectedRuntimeExceptionOccurs()
        throws Exception {

        // Arrange
        UserEntity user = UserEntity.builder().userId(USER_ID).tokenSubject(TOKEN_SUBJECT).build();
        RuntimeException runtimeException = new RuntimeException("db boom");
        when(roleMappingCacheLookupService.getRoleMappingByTokenSubject(user))
            .thenReturn(java.util.Map.of(101L, Set.of((short) 7)));
        when(businessUnitUserService.findAllRolesOfUser(user)).thenThrow(runtimeException);

        LegacyBusinessUnitUserId legacyBusinessUnitUser = LegacyBusinessUnitUserId.builder()
            .businessUnitId("7")
            .build();

        // Act
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> synchroniseRolesService.synchroniseRoles(user, List.of(legacyBusinessUnitUser))
        );

        // Assert
        assertEquals(
            errorMessage(SYNC_STAGE, UNEXPECTED_RUNTIME_EXCEPTION_REASON),
            exception.getMessage()
        );
        assertEquals(runtimeException, exception.getCause());
    }

    private String errorMessage(String stage, String reason) {
        return "Could not synchronise permissions for user " + USER_ID
            + " at stage: " + stage + ". Reason: " + reason;
    }

}
