package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUsersResponse;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyGetUserResponse;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.BusinessUnitUserService;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynchronisePermissionsServiceTest {

    @Mock
    private LegacyUserService legacyUserService;

    @Mock
    private SynchroniseBusinessUnitUsersService refreshBusinessUnitUsersService;

    @Mock
    private SynchroniseRolesService synchroniseRolesService;

    @Mock
    private BusinessUnitUserService businessUnitUserService;

    @Mock
    private UserService userService;

    @InjectMocks
    private SynchronisePermissionsService synchronisePermissionsService;

    @Test
    void legacyRefresh_removesAllUserRoles_whenSynchroniseExceptionOccurs() throws SynchronisePermissionsException {
        // Arrange
        UserEntity user = UserEntity.builder()
            .userId(500000000L)
            .username("opal-test@hmcts.net")
            .build();

        when(legacyUserService.getUserIds(any())).thenReturn(
            LegacyGetUserResponse.builder().libraUserIds(List.of("123")).build()
        );
        when(legacyUserService.getBusinessUnitUsers(any())).thenReturn(
            LegacyBusinessUnitUsersResponse.builder().build()
        );
        doThrowSynchroniseExceptionOnCacheProcessing();

        RoleEntity role1 = RoleEntity.builder().roleId(1L).name("role-1").build();
        RoleEntity role2 = RoleEntity.builder().roleId(2L).name("role-2").build();
        when(businessUnitUserService.findAllRolesOfUser(user)).thenReturn(Set.of(role1, role2));

        // Act
        synchronisePermissionsService.synchronise(user);

        // Assert
        verify(userService).deleteRoleFromUser(user, 1L);
        verify(userService).deleteRoleFromUser(user, 2L);
        verify(userService, never()).activateUser(user);
    }

    private void doThrowSynchroniseExceptionOnCacheProcessing() throws SynchronisePermissionsException {
        doThrow(new SynchronisePermissionsException("refresh failed"))
            .when(synchroniseRolesService)
            .process(any(), any());
    }
}
