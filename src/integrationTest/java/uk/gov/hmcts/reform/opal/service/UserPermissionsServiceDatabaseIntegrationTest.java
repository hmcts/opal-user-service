package uk.gov.hmcts.reform.opal.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.BusinessUnitUserDto;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateDto;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_user_state_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("UserPermissionsService database integration tests")
class UserPermissionsServiceDatabaseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserPermissionsService userPermissionsService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should build user state from users, business unit users, entitlements and application functions")
    void buildUserState_includesEntitlements() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();

        UserStateDto result = userPermissionsService.buildUserState(user);

        assertNotNull(result);
        assertEquals(500000000L, result.getUserId());
        assertEquals("opal-test@HMCTS.NET", result.getUsername());
        assertEquals(3, result.getBusinessUnitUsers().size());

        Optional<BusinessUnitUserDto> businessUnit70 = result.getBusinessUnitUsers()
            .stream()
            .filter(businessUnitUser -> businessUnitUser.getBusinessUnitId() == 70)
            .findFirst();

        assertTrue(businessUnit70.isPresent());
        assertEquals("L065JG", businessUnit70.get().getBusinessUnitUserId());
        assertEquals(2, businessUnit70.get().getPermissions().size());
        assertTrue(
            businessUnit70.get().getPermissions().stream()
                .anyMatch(permission -> "Account Enquiry".equals(permission.getPermissionName())));
        assertTrue(
            businessUnit70.get().getPermissions().stream()
                .anyMatch(permission -> "Account Enquiry - Account Notes".equals(permission.getPermissionName())));
    }

    @Test
    @DisplayName("Should preserve business unit users when entitlements are missing")
    void buildUserState_keepsUnitsWhenEntitlementsMissing() {
        UserEntity user = userRepository.findById(500000006L).orElseThrow();

        UserStateDto result = userPermissionsService.buildUserState(user);

        assertNotNull(result);
        assertEquals(500000006L, result.getUserId());
        assertEquals("no-go-user@HMCTS.NET", result.getUsername());
        assertEquals("No Permissions", result.getName());
        assertEquals(2, result.getBusinessUnitUsers().size());
        assertTrue(result.getBusinessUnitUsers().stream().allMatch(businessUnitUser ->
            businessUnitUser.getPermissions().isEmpty()));
    }

    @Test
    @DisplayName("Should map users columns for state responses even when no business units exist")
    void buildUserState_handlesUsersWithoutBusinessUnits() {
        UserEntity user = userRepository.findById(500000003L).orElseThrow();

        UserStateDto result = userPermissionsService.buildUserState(user);

        assertNotNull(result);
        assertEquals(500000003L, result.getUserId());
        assertEquals("test-user@HMCTS.NET", result.getUsername());
        assertEquals("Pablo", result.getName());
        assertEquals("active", result.getStatus());
        assertEquals(2L, result.getVersion().longValue());
        assertTrue(result.getBusinessUnitUsers().isEmpty());
    }
}
