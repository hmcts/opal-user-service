package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRoleRepository;
import uk.gov.hmcts.reform.opal.repository.UserEntitlementRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynchroniseBusinessUnitUsersServiceTest {

    private static final long USER_ID = 500000001L;
    private static final long DIFFERENT_USER_ID = 500000006L;
    private static final String BUSINESS_UNIT_USER_ID = "L081JG";
    private static final String STALE_BUSINESS_UNIT_USER_ID = "L082JG";
    private static final short BUSINESS_UNIT_ID = 70;
    private static final short DIFFERENT_BUSINESS_UNIT_ID = 67;
    private static final String LEGACY_BUU_PAYLOAD_MISSING_REASON = "legacy business unit user payload is missing";
    private static final String LEGACY_BUU_ENTRY_MISSING_REASON = "legacy business unit user entry is missing";
    private static final String SYNC_STAGE = "synchronise business unit users";
    private static final String UNEXPECTED_RUNTIME_EXCEPTION_REASON = "unexpected runtime exception";

    @Mock
    private BusinessUnitUserRepository businessUnitUserRepository;

    @Mock
    private BusinessUnitRepository businessUnitRepository;

    @Mock
    private BusinessUnitUserRoleRepository businessUnitUserRoleRepository;

    @Mock
    private UserEntitlementRepository userEntitlementRepository;

    @InjectMocks
    private SynchroniseBusinessUnitUsersService synchroniseBusinessUnitUsersService;

    @Test
    void synchroniseBusinessUnitUsers_throwsWhenLegacyPayloadIsNull() {

        // Arrange
        UserEntity user = user(USER_ID);

        // Act
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> synchroniseBusinessUnitUsersService.synchroniseBusinessUnitsUsers(user, null)
        );

        // Assert
        assertEquals(errorMessage(USER_ID, LEGACY_BUU_PAYLOAD_MISSING_REASON), exception.getMessage());
        verifyNoInteractions(
            businessUnitUserRepository,
            businessUnitRepository,
            businessUnitUserRoleRepository,
            userEntitlementRepository
        );
    }

    @Test
    void synchroniseBusinessUnitUsers_throwsWhenLegacyPayloadContainsNullEntry() {

        // Arrange
        UserEntity user = user(USER_ID);

        // Act
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> synchroniseBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
                user,
                java.util.Collections.singletonList(null)
            )
        );

        // Assert
        assertEquals(errorMessage(USER_ID, LEGACY_BUU_ENTRY_MISSING_REASON), exception.getMessage());
        verifyNoInteractions(
            businessUnitUserRepository,
            businessUnitRepository,
            businessUnitUserRoleRepository,
            userEntitlementRepository
        );
    }

    @Test
    void synchroniseBusinessUnitUsers_insertsMissingBusinessUnitUser() {

        // Arrange
        BusinessUnitEntity businessUnit = businessUnit(BUSINESS_UNIT_ID);
        when(businessUnitRepository.findById(BUSINESS_UNIT_ID)).thenReturn(Optional.of(businessUnit));
        when(businessUnitUserRepository.findById(BUSINESS_UNIT_USER_ID)).thenReturn(Optional.empty());
        when(businessUnitUserRepository.findAllByUser_UserIdAndBusinessUnitUserIdNotIn(
            USER_ID, java.util.Set.of(BUSINESS_UNIT_USER_ID)
        )).thenReturn(List.of());
        UserEntity user = user(USER_ID);
        LegacyBusinessUnitUserId legacyBusinessUnitUser = legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID,
                                                                                 BUSINESS_UNIT_ID);

        // Act
        synchroniseBusinessUnitUsersService.synchroniseBusinessUnitsUsers(user, List.of(legacyBusinessUnitUser));

        // Assert
        ArgumentCaptor<BusinessUnitUserEntity> captor = ArgumentCaptor.forClass(BusinessUnitUserEntity.class);
        verify(businessUnitUserRepository).save(captor.capture());

        BusinessUnitUserEntity savedBusinessUnitUser = captor.getValue();
        assertEquals(BUSINESS_UNIT_USER_ID, savedBusinessUnitUser.getBusinessUnitUserId());
        assertEquals(BUSINESS_UNIT_ID, savedBusinessUnitUser.getBusinessUnitId());
        assertEquals(USER_ID, savedBusinessUnitUser.getUser().getUserId());
        verify(userEntitlementRepository, never()).deleteAllByBusinessUnitUser_BusinessUnitUserIdIn(any());
        verify(businessUnitUserRoleRepository, never()).deleteAllByBusinessUnitUser_BusinessUnitUserIdIn(any());
    }

    @Test
    void synchroniseBusinessUnitUsers_updatesExistingBusinessUnitUserWhenValuesDiffer() {

        // Arrange
        BusinessUnitEntity businessUnit = businessUnit(BUSINESS_UNIT_ID);
        BusinessUnitUserEntity existingBusinessUnitUser =
            businessUnitUser(BUSINESS_UNIT_USER_ID, DIFFERENT_BUSINESS_UNIT_ID, DIFFERENT_USER_ID);
        when(businessUnitRepository.findById(BUSINESS_UNIT_ID)).thenReturn(Optional.of(businessUnit));
        when(businessUnitUserRepository.findById(BUSINESS_UNIT_USER_ID))
            .thenReturn(Optional.of(existingBusinessUnitUser));
        when(businessUnitUserRepository.findAllByUser_UserIdAndBusinessUnitUserIdNotIn(
            USER_ID, java.util.Set.of(BUSINESS_UNIT_USER_ID)
        )).thenReturn(List.of());
        UserEntity user = user(USER_ID);
        LegacyBusinessUnitUserId legacyBusinessUnitUser = legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID,
                                                                                 BUSINESS_UNIT_ID);

        // Act
        synchroniseBusinessUnitUsersService.synchroniseBusinessUnitsUsers(user, List.of(legacyBusinessUnitUser));

        // Assert
        assertEquals(BUSINESS_UNIT_ID, existingBusinessUnitUser.getBusinessUnitId());
        assertEquals(USER_ID, existingBusinessUnitUser.getUser().getUserId());
        verify(businessUnitUserRepository, never()).save(any(BusinessUnitUserEntity.class));
        verify(userEntitlementRepository, never()).deleteAllByBusinessUnitUser_BusinessUnitUserIdIn(any());
        verify(businessUnitUserRoleRepository, never()).deleteAllByBusinessUnitUser_BusinessUnitUserIdIn(any());
    }

    @Test
    void synchroniseBusinessUnitUsers_throwsWhenBusinessUnitDoesNotExist() {

        // Arrange
        UserEntity user = user(USER_ID);
        LegacyBusinessUnitUserId legacyBusinessUnitUser = legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID,
                                                                                 BUSINESS_UNIT_ID);
        when(businessUnitRepository.findById(BUSINESS_UNIT_ID)).thenReturn(Optional.empty());

        // Act
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> synchroniseBusinessUnitUsersService.synchroniseBusinessUnitsUsers(user,
                                                                                    List.of(legacyBusinessUnitUser))
        );

        // Assert
        assertEquals(errorMessage(USER_ID, "legacy business unit not found: " + BUSINESS_UNIT_ID),
                     exception.getMessage());
        verify(businessUnitUserRepository, never()).save(any(BusinessUnitUserEntity.class));
        verify(userEntitlementRepository, never()).deleteAllByBusinessUnitUser_BusinessUnitUserIdIn(any());
        verify(businessUnitUserRoleRepository, never()).deleteAllByBusinessUnitUser_BusinessUnitUserIdIn(any());
    }

    @Test
    void synchroniseBusinessUnitUsers_throwsWhenBusinessUnitUserIdIsInvalid() {

        // Arrange
        UserEntity user = user(USER_ID);
        LegacyBusinessUnitUserId legacyBusinessUnitUser = legacyBusinessUnitUser("", BUSINESS_UNIT_ID);

        // Act
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> synchroniseBusinessUnitUsersService.synchroniseBusinessUnitsUsers(user,
                                                                                    List.of(legacyBusinessUnitUser))
        );

        // Assert
        assertEquals(errorMessage(USER_ID, "invalid business unit user id: "), exception.getMessage());
        verifyNoInteractions(
            businessUnitRepository,
            businessUnitUserRepository,
            businessUnitUserRoleRepository,
            userEntitlementRepository
        );
    }

    @Test
    void synchroniseBusinessUnitUsers_throwsWhenBusinessUnitIdIsInvalid() {

        // Arrange
        UserEntity user = user(USER_ID);
        LegacyBusinessUnitUserId legacyBusinessUnitUser = LegacyBusinessUnitUserId.builder()
            .businessUnitUserId(BUSINESS_UNIT_USER_ID)
            .businessUnitId("ABC")
            .build();

        // Act
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> synchroniseBusinessUnitUsersService.synchroniseBusinessUnitsUsers(user,
                                                                                    List.of(legacyBusinessUnitUser))
        );

        // Assert
        assertEquals(errorMessage(USER_ID, "invalid business unit id: ABC"), exception.getMessage());
        verifyNoInteractions(
            businessUnitRepository,
            businessUnitUserRepository,
            businessUnitUserRoleRepository,
            userEntitlementRepository
        );
    }

    @Test
    void synchroniseBusinessUnitUsers_deletesStaleBusinessUnitUsersAndRelatedData() {

        // Arrange
        BusinessUnitEntity businessUnit = businessUnit(BUSINESS_UNIT_ID);
        BusinessUnitUserEntity currentBusinessUnitUser = businessUnitUser(BUSINESS_UNIT_USER_ID,
                                                                          BUSINESS_UNIT_ID, USER_ID);
        BusinessUnitUserEntity staleBusinessUnitUser = businessUnitUser(STALE_BUSINESS_UNIT_USER_ID,
                                                                        DIFFERENT_BUSINESS_UNIT_ID,
                                                                        USER_ID);
        when(businessUnitRepository.findById(BUSINESS_UNIT_ID)).thenReturn(Optional.of(businessUnit));
        when(businessUnitUserRepository.findById(BUSINESS_UNIT_USER_ID))
            .thenReturn(Optional.of(currentBusinessUnitUser));
        when(businessUnitUserRepository.findAllByUser_UserIdAndBusinessUnitUserIdNotIn(
            USER_ID, java.util.Set.of(BUSINESS_UNIT_USER_ID)
        )).thenReturn(List.of(staleBusinessUnitUser));
        UserEntity user = user(USER_ID);
        LegacyBusinessUnitUserId legacyBusinessUnitUser = legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID,
                                                                                 BUSINESS_UNIT_ID);

        // Act
        synchroniseBusinessUnitUsersService.synchroniseBusinessUnitsUsers(user, List.of(legacyBusinessUnitUser));

        // Assert
        List<String> staleBusinessUnitUserIds = List.of(STALE_BUSINESS_UNIT_USER_ID);
        verify(userEntitlementRepository).deleteAllByBusinessUnitUser_BusinessUnitUserIdIn(staleBusinessUnitUserIds);
        verify(businessUnitUserRoleRepository)
            .deleteAllByBusinessUnitUser_BusinessUnitUserIdIn(staleBusinessUnitUserIds);
        verify(businessUnitUserRepository).deleteAllById(staleBusinessUnitUserIds);
    }

    @Test
    void synchroniseBusinessUnitUsers_throwsSynchronisePermissionsException_whenUnexpectedRuntimeExceptionOccurs() {

        // Arrange
        UserEntity user = user(USER_ID);
        RuntimeException runtimeException = new RuntimeException("db boom");
        LegacyBusinessUnitUserId legacyBusinessUnitUser = legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID,
                                                                                 BUSINESS_UNIT_ID);
        when(businessUnitRepository.findById(BUSINESS_UNIT_ID)).thenThrow(runtimeException);

        // Act
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> synchroniseBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
                user,
                List.of(legacyBusinessUnitUser)
            )
        );

        // Assert
        assertEquals(errorMessage(USER_ID, UNEXPECTED_RUNTIME_EXCEPTION_REASON), exception.getMessage());
        assertSame(runtimeException, exception.getCause());
    }

    private String errorMessage(long userId, String reason) {
        return "Could not synchronise permissions for user " + userId
            + " at stage: " + SYNC_STAGE
            + ". Reason: " + reason;
    }

    private UserEntity user(long userId) {
        return UserEntity.builder().userId(userId).build();
    }

    private BusinessUnitEntity businessUnit(short businessUnitId) {
        return BusinessUnitEntity.builder().businessUnitId(businessUnitId).build();
    }

    private BusinessUnitUserEntity businessUnitUser(String businessUnitUserId, short businessUnitId, long userId) {
        return BusinessUnitUserEntity.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnit(businessUnit(businessUnitId))
            .user(user(userId))
            .build();
    }

    private LegacyBusinessUnitUserId legacyBusinessUnitUser(String businessUnitUserId, short businessUnitId) {
        return LegacyBusinessUnitUserId.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnitId(Short.toString(businessUnitId))
            .build();
    }

}
