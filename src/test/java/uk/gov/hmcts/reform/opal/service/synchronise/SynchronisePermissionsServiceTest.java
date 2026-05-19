package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynchronisePermissionsServiceTest {

    private static final long USER_ID = 123L;
    private static final long ROLE_ID = 101L;
    private static final String SYNC_STAGE = "synchronise user permissions";
    private static final String UNEXPECTED_RUNTIME_EXCEPTION_REASON = "unexpected runtime exception";
    private static final UserEntity DETACHED_USER = user(USER_ID, null);
    private static final UserEntity MANAGED_USER_WITH_NO_ACTIVATION = user(USER_ID, null);
    private static final UserEntity MANAGED_USER_WITH_EXISTING_ACTIVATION_DATE =
        user(USER_ID, LocalDateTime.parse("2025-01-02T03:04:05.678"));

    @Mock
    private LegacyWrapperService legacyWrapperService;

    @Mock
    private SynchroniseBusinessUnitUsersService synchroniseBusinessUnitUsersService;

    @Mock
    private SynchroniseRolesService synchroniseRolesService;

    @Mock
    private UserService userService;

    @InjectMocks
    private SynchronisePermissionsService synchronisePermissionsService;

    // happy path
    @Test
    void synchronise_activatesUser_whenValidatedRolesExistAndActivationDateIsMissing() {

        // Arrange
        List<LegacyBusinessUnitUserId> legacyBusinessUnitUsers = List.of(
            legacyBusinessUnitUserId("66", "L066JG")
        );
        when(userService.getUser(USER_ID)).thenReturn(MANAGED_USER_WITH_NO_ACTIVATION);
        when(legacyWrapperService.getBusinessUnitUserIds(MANAGED_USER_WITH_NO_ACTIVATION))
            .thenReturn(legacyBusinessUnitUsers);
        when(synchroniseRolesService.synchroniseRoles(MANAGED_USER_WITH_NO_ACTIVATION, legacyBusinessUnitUsers))
            .thenReturn(Set.of(ROLE_ID));

        // Act
        synchronisePermissionsService.synchronise(DETACHED_USER);

        // Assert
        verify(userService).activateUser(MANAGED_USER_WITH_NO_ACTIVATION);
    }

    @Test
    void synchronise_callsLegacyAndSynchronisationServices_doesNotActivateUser_whenNoValidatedRolesReturned() {

        // Arrange
        List<LegacyBusinessUnitUserId> legacyBusinessUnitUsers = List.of(
            legacyBusinessUnitUserId("66", "L066JG")
        );
        when(userService.getUser(USER_ID)).thenReturn(MANAGED_USER_WITH_NO_ACTIVATION);
        when(legacyWrapperService.getBusinessUnitUserIds(MANAGED_USER_WITH_NO_ACTIVATION))
            .thenReturn(legacyBusinessUnitUsers);
        when(synchroniseRolesService.synchroniseRoles(MANAGED_USER_WITH_NO_ACTIVATION, legacyBusinessUnitUsers))
            .thenReturn(Set.of());

        // Act
        synchronisePermissionsService.synchronise(DETACHED_USER);

        // Assert
        verify(userService).getUser(USER_ID);
        verify(legacyWrapperService).getBusinessUnitUserIds(MANAGED_USER_WITH_NO_ACTIVATION);
        verify(synchroniseBusinessUnitUsersService).synchroniseBusinessUnitsUsers(
            MANAGED_USER_WITH_NO_ACTIVATION,
            legacyBusinessUnitUsers
        );
        verify(synchroniseRolesService).synchroniseRoles(MANAGED_USER_WITH_NO_ACTIVATION, legacyBusinessUnitUsers);
        verify(userService, never()).activateUser(MANAGED_USER_WITH_NO_ACTIVATION);
    }

    @Test
    void synchronise_doesNotActivateUser_whenActivationDateAlreadyExists() {

        // Arrange
        List<LegacyBusinessUnitUserId> legacyBusinessUnitUsers = List.of(
            legacyBusinessUnitUserId("66", "L066JG")
        );
        when(userService.getUser(USER_ID)).thenReturn(MANAGED_USER_WITH_EXISTING_ACTIVATION_DATE);
        when(legacyWrapperService.getBusinessUnitUserIds(MANAGED_USER_WITH_EXISTING_ACTIVATION_DATE))
            .thenReturn(legacyBusinessUnitUsers);
        when(synchroniseRolesService.synchroniseRoles(
            MANAGED_USER_WITH_EXISTING_ACTIVATION_DATE,
            legacyBusinessUnitUsers
        )).thenReturn(Set.of(ROLE_ID));

        // Act
        synchronisePermissionsService.synchronise(DETACHED_USER);

        // Assert
        verify(userService, never()).activateUser(MANAGED_USER_WITH_EXISTING_ACTIVATION_DATE);
    }

    @Test
    void synchronise_throwsSynchronisePermissionsException_whenUnexpectedRuntimeExceptionOccurs() {

        // Arrange
        List<LegacyBusinessUnitUserId> legacyBusinessUnitUsers = List.of(
            legacyBusinessUnitUserId("66", "L066JG")
        );
        RuntimeException runtimeException = new RuntimeException("db boom");
        when(userService.getUser(USER_ID)).thenReturn(MANAGED_USER_WITH_NO_ACTIVATION);
        when(legacyWrapperService.getBusinessUnitUserIds(MANAGED_USER_WITH_NO_ACTIVATION))
            .thenReturn(legacyBusinessUnitUsers);
        doThrow(runtimeException).when(synchroniseBusinessUnitUsersService)
            .synchroniseBusinessUnitsUsers(MANAGED_USER_WITH_NO_ACTIVATION, legacyBusinessUnitUsers);

        // Act
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> synchronisePermissionsService.synchronise(DETACHED_USER)
        );

        // Assert
        assertEquals(
            "Could not synchronise permissions for user " + USER_ID
                + " at stage: " + SYNC_STAGE
                + ". Reason: " + UNEXPECTED_RUNTIME_EXCEPTION_REASON,
            exception.getMessage()
        );
        verify(userService, never()).activateUser(MANAGED_USER_WITH_NO_ACTIVATION);
    }

    private static UserEntity user(long userId, LocalDateTime activationDate) {
        return UserEntity.builder()
            .userId(userId)
            .activationDate(activationDate)
            .build();
    }

    private LegacyBusinessUnitUserId legacyBusinessUnitUserId(String businessUnitId, String businessUnitUserId) {
        return LegacyBusinessUnitUserId.builder()
            .businessUnitId(businessUnitId)
            .businessUnitUserId(businessUnitUserId)
            .build();
    }
}
