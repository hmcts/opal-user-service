package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetBusinessUnitUserIdsResponse;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetUserRequest;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetUserResponse;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.legacy.LegacyBusinessUnitUserService;
import uk.gov.hmcts.reform.opal.service.legacy.LegacyUserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyWrapperServiceTest {

    private static final String USERNAME = "legacy.user@hmcts.net";

    @Mock
    private LegacyBusinessUnitUserService legacyBusinessUnitUserService;

    @Mock
    private LegacyUserService legacyUserService;

    @InjectMocks
    private LegacyWrapperService legacyWrapperService;

    @Test
    void getBusinessUnitUserIds_returnsBusinessUnitUsers_whenBothLegacyCallsSucceed() {

        // Arrange
        UserEntity user = user(USERNAME);
        List<String> libraUserIds = List.of("SU001", "SU002");
        List<LegacyBusinessUnitUserId> expectedBusinessUnitUsers = List.of(
            LegacyBusinessUnitUserId.builder().businessUnitUserId("L066JG").businessUnitId("66").build(),
            LegacyBusinessUnitUserId.builder().businessUnitUserId("L067JG").businessUnitId("67").build()
        );
        when(legacyUserService.getUser(any(LegacyGetUserRequest.class))).thenReturn(
            new GatewayService.Response<>(
                HttpStatus.OK,
                LegacyGetUserResponse.builder().count(libraUserIds.size()).libraUserIds(libraUserIds).build()
            )
        );
        when(legacyBusinessUnitUserService.getBusinessUnitUserIds(libraUserIds)).thenReturn(
            new GatewayService.Response<>(
                HttpStatus.OK,
                LegacyGetBusinessUnitUserIdsResponse.builder()
                    .count(expectedBusinessUnitUsers.size())
                    .businessUnitUserIds(expectedBusinessUnitUsers)
                    .build()
            )
        );

        // Act
        List<LegacyBusinessUnitUserId> result = legacyWrapperService.getBusinessUnitUserIds(user);

        // Assert
        assertEquals(expectedBusinessUnitUsers, result);
        verify(legacyUserService).getUser(
            argThat((LegacyGetUserRequest request) -> USERNAME.equals(request.getEmailAddress()))
        );
        verify(legacyBusinessUnitUserService).getBusinessUnitUserIds(libraUserIds);
    }

    @Test
    void getBusinessUnitUserIds_returnsEmptyList_whenLibraUserIdsAreMissing() {

        // Arrange
        UserEntity user = user(USERNAME);
        when(legacyUserService.getUser(any(LegacyGetUserRequest.class))).thenReturn(
            new GatewayService.Response<>(HttpStatus.OK, LegacyGetUserResponse.builder().libraUserIds(null).build())
        );

        // Act
        List<LegacyBusinessUnitUserId> result = legacyWrapperService.getBusinessUnitUserIds(user);

        // Assert
        assertTrue(result.isEmpty());
        verifyNoInteractions(legacyBusinessUnitUserService);
    }

    @Test
    void getBusinessUnitUserIds_returnsEmptyList_whenBusinessUnitUserIdsAreNull() {

        // Arrange
        UserEntity user = user(USERNAME);
        List<String> libraUserIds = List.of("SU001");
        when(legacyUserService.getUser(any(LegacyGetUserRequest.class))).thenReturn(
            new GatewayService.Response<>(
                HttpStatus.OK,
                LegacyGetUserResponse.builder().count(libraUserIds.size()).libraUserIds(libraUserIds).build()
            )
        );
        when(legacyBusinessUnitUserService.getBusinessUnitUserIds(libraUserIds)).thenReturn(
            new GatewayService.Response<>(
                HttpStatus.OK,
                LegacyGetBusinessUnitUserIdsResponse.builder().businessUnitUserIds(null).build()
            )
        );

        // Act
        List<LegacyBusinessUnitUserId> result = legacyWrapperService.getBusinessUnitUserIds(user);

        // Assert
        assertTrue(result.isEmpty());
    }

    // User lookup exceptions (`requireSuccessfulResponse` for GetSystemUserIdsByEmail)
    @Test
    void getBusinessUnitUserIds_throwsSynchronisePermissionsException_whenUserLookupReturnsFailureResponse() {

        // Arrange
        UserEntity user = user(USERNAME);
        when(legacyUserService.getUser(any(LegacyGetUserRequest.class))).thenReturn(
            new GatewayService.Response<>(HttpStatus.INTERNAL_SERVER_ERROR, "legacy failure body")
        );

        // Act / Assert
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> legacyWrapperService.getBusinessUnitUserIds(user)
        );
        assertTrue(exception.getMessage().contains("Legacy call failed: GetSystemUserIdsByEmail"));
        assertTrue(exception.getMessage().contains("httpCode="));
        verifyNoInteractions(legacyBusinessUnitUserService);
    }

    @Test
    void getBusinessUnitUserIds_throwsSynchronisePermissionsException_whenUserLookupReturnsExceptionResponse() {

        // Arrange
        UserEntity user = user(USERNAME);
        RuntimeException legacyException = new RuntimeException("legacy boom");
        when(legacyUserService.getUser(any(LegacyGetUserRequest.class))).thenReturn(
            new GatewayService.Response<>(HttpStatus.INTERNAL_SERVER_ERROR, legacyException, "raw exception body")
        );

        // Act / Assert
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> legacyWrapperService.getBusinessUnitUserIds(user)
        );
        assertEquals("Legacy call failed: GetSystemUserIdsByEmail", exception.getMessage());
        assertSame(legacyException, exception.getCause());
        verifyNoInteractions(legacyBusinessUnitUserService);
    }

    @Test
    void getBusinessUnitUserIds_throwsSynchronisePermissionsException_whenUserLookupReturnsSuccessfulResponseWithNullEntity() {

        // Arrange
        UserEntity user = user(USERNAME);
        when(legacyUserService.getUser(any(LegacyGetUserRequest.class))).thenReturn(
            new GatewayService.Response<>(HttpStatus.OK, (LegacyGetUserResponse) null)
        );

        // Act / Assert
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> legacyWrapperService.getBusinessUnitUserIds(user)
        );
        assertTrue(exception.getMessage().contains("Legacy call failed: GetSystemUserIdsByEmail"));
        assertTrue(exception.getMessage().contains("httpCode="));
        verifyNoInteractions(legacyBusinessUnitUserService);
    }

    @Test
    void getBusinessUnitUserIds_throwsSynchronisePermissionsException_whenUserLookupReturnsNullResponse() {

        // Arrange
        UserEntity user = user(USERNAME);
        when(legacyUserService.getUser(any(LegacyGetUserRequest.class))).thenReturn(null);

        // Act / Assert
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> legacyWrapperService.getBusinessUnitUserIds(user)
        );
        assertEquals("Legacy call returned null response: GetSystemUserIdsByEmail", exception.getMessage());
        verifyNoInteractions(legacyBusinessUnitUserService);
    }

    // Business-unit lookup exceptions (`requireSuccessfulResponse` for GetBUUserIdsBySystemUserIds)
    @Test
    void getBusinessUnitUserIds_throwsSynchronisePermissionsException_whenBusinessUnitLookupReturnsFailureResponse() {

        // Arrange
        UserEntity user = user(USERNAME);
        List<String> libraUserIds = stubSuccessfulUserLookup("SU001");
        when(legacyBusinessUnitUserService.getBusinessUnitUserIds(libraUserIds)).thenReturn(
            new GatewayService.Response<>(HttpStatus.INTERNAL_SERVER_ERROR, "legacy failure body")
        );

        // Act / Assert
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> legacyWrapperService.getBusinessUnitUserIds(user)
        );
        assertTrue(exception.getMessage().contains("Legacy call failed: GetBUUserIdsBySystemUserIds"));
        assertTrue(exception.getMessage().contains("httpCode="));
    }

    @Test
    void getBusinessUnitUserIds_throwsSynchronisePermissionsException_whenBusinessUnitLookupReturnsExceptionResponse() {

        // Arrange
        UserEntity user = user(USERNAME);
        List<String> libraUserIds = stubSuccessfulUserLookup("SU001");
        RuntimeException legacyException = new RuntimeException("legacy boom");
        when(legacyBusinessUnitUserService.getBusinessUnitUserIds(libraUserIds)).thenReturn(
            new GatewayService.Response<>(HttpStatus.INTERNAL_SERVER_ERROR, legacyException, "raw exception body")
        );

        // Act / Assert
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> legacyWrapperService.getBusinessUnitUserIds(user)
        );
        assertEquals("Legacy call failed: GetBUUserIdsBySystemUserIds", exception.getMessage());
        assertSame(legacyException, exception.getCause());
    }

    @Test
    void getBusinessUnitUserIds_throwsSynchronisePermissionsException_whenBusinessUnitLookupReturnsSuccessfulResponseWithNullEntity() {

        // Arrange
        UserEntity user = user(USERNAME);
        List<String> libraUserIds = stubSuccessfulUserLookup("SU001");
        when(legacyBusinessUnitUserService.getBusinessUnitUserIds(libraUserIds)).thenReturn(
            new GatewayService.Response<>(HttpStatus.OK, (LegacyGetBusinessUnitUserIdsResponse) null)
        );

        // Act / Assert
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> legacyWrapperService.getBusinessUnitUserIds(user)
        );
        assertTrue(exception.getMessage().contains("Legacy call failed: GetBUUserIdsBySystemUserIds"));
        assertTrue(exception.getMessage().contains("httpCode="));
    }

    @Test
    void getBusinessUnitUserIds_throwsSynchronisePermissionsException_whenBusinessUnitLookupReturnsNullResponse() {

        // Arrange
        UserEntity user = user(USERNAME);
        List<String> libraUserIds = stubSuccessfulUserLookup("SU001");
        when(legacyBusinessUnitUserService.getBusinessUnitUserIds(libraUserIds)).thenReturn(null);

        // Act / Assert
        SynchronisePermissionsException exception = assertThrows(
            SynchronisePermissionsException.class,
            () -> legacyWrapperService.getBusinessUnitUserIds(user)
        );
        assertEquals("Legacy call returned null response: GetBUUserIdsBySystemUserIds", exception.getMessage());
    }

    private UserEntity user(String username) {
        return UserEntity.builder().username(username).build();
    }

    private List<String> stubSuccessfulUserLookup(String... ids) {
        List<String> libraUserIds = List.of(ids);
        when(legacyUserService.getUser(any(LegacyGetUserRequest.class))).thenReturn(
            new GatewayService.Response<>(
                HttpStatus.OK,
                LegacyGetUserResponse.builder().count(libraUserIds.size()).libraUserIds(libraUserIds).build()
            )
        );
        return libraUserIds;
    }
}
