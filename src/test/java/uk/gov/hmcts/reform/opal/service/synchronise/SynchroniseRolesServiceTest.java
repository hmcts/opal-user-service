package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUser;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.service.opal.BusinessUnitUserService;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynchroniseRolesServiceTest {

    private static final String TOKEN_SUBJECT = "subject-123";

    @Mock
    private UserService userService;

    @Mock
    private BusinessUnitUserService businessUnitUserService;

    @Mock
    private RoleMappingCacheLookupService roleMappingCacheLookupService;

    @InjectMocks
    private SynchroniseRolesService synchroniseRolesService;

    @Test
    void process_throwsLegacyRefreshException_whenLookupCannotReadCache() {

        // Arrange
        UserEntity user = UserEntity.builder().tokenSubject(TOKEN_SUBJECT).build();
        when(roleMappingCacheLookupService.getRoleMappingByTokenSubject(TOKEN_SUBJECT))
            .thenThrow(new SynchroniseRolesException("Could not parse role mapping cache"));

        // Act / Assert
        assertThrows(SynchroniseRolesException.class, () -> synchroniseRolesService.process(user, List.of()));
        verify(roleMappingCacheLookupService).getRoleMappingByTokenSubject(TOKEN_SUBJECT);
    }

    @Test
    void process_deletesOnlyRolesMissingFromCachedRoleIds() {

        // Arrange
        UserEntity user = UserEntity.builder().tokenSubject(TOKEN_SUBJECT).build();
        when(roleMappingCacheLookupService.getRoleMappingByTokenSubject(TOKEN_SUBJECT))
            .thenReturn(java.util.Map.of(101L, Set.of((short) 7)));

        RoleEntity roleStillInCache = RoleEntity.builder().roleId(101L).name("name-not-id").build();
        RoleEntity roleMissingFromCache = RoleEntity.builder().roleId(202L).name("101").build();
        when(businessUnitUserService.findAllRolesOfUser(user)).thenReturn(Set.of(roleStillInCache,
                                                                                 roleMissingFromCache));

        LegacyBusinessUnitUser legacyBusinessUnitUser = LegacyBusinessUnitUser.builder()
            .businessUnitId("7")
            .build();

        // Act
        synchroniseRolesService.process(user, List.of(legacyBusinessUnitUser));

        // Assert
        verify(userService, never()).deleteRoleFromUser(user, 101L);
        verify(userService).deleteRoleFromUser(user, 202L);
    }

}
