package uk.gov.hmcts.reform.opal.service.opal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("User state lookup database integration tests")
class UserStateLookupDatabaseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserEntitlementService userEntitlementService;

    @Autowired
    private UserStateService userStateService;

    @Test
    @DisplayName("UserService should return all business unit users including those without permissions")
    void userService_getUserStateByUsername_returnsAllBusinessUnitUsers() {
        UserState result = userService.getUserStateByUsername("opal-test@HMCTS.NET");

        assertEquals(500000000L, result.getUserId());
        assertEquals("opal-test@HMCTS.NET", result.getUserName());
        assertEquals(7, result.getBusinessUnitUser().size());

        Optional<BusinessUnitUser> businessUnit70 = result.getBusinessUnitUserForBusinessUnit((short) 70);
        Optional<BusinessUnitUser> businessUnit73 = result.getBusinessUnitUserForBusinessUnit((short) 73);

        assertTrue(businessUnit70.isPresent());
        assertEquals(2, businessUnit70.get().getPermissions().size());
        assertTrue(businessUnit73.isPresent());
        assertTrue(businessUnit73.get().getPermissions().isEmpty());
    }

    @Test
    @DisplayName("UserEntitlementService should return only business unit users that have entitlements")
    void userEntitlementService_getUserStateByUsername_returnsOnlyEntitledBusinessUnits() {
        UserState result = userEntitlementService.getUserStateByUsername("opal-test@HMCTS.NET").orElseThrow();

        assertEquals(500000000L, result.getUserId());
        assertEquals("opal-test@HMCTS.NET", result.getUserName());
        assertEquals(3, result.getBusinessUnitUser().size());
        assertTrue(result.getBusinessUnitUserForBusinessUnit((short) 70).isPresent());
        assertTrue(result.getBusinessUnitUserForBusinessUnit((short) 68).isPresent());
        assertTrue(result.getBusinessUnitUserForBusinessUnit((short) 61).isPresent());
        assertFalse(result.getBusinessUnitUserForBusinessUnit((short) 73).isPresent());
    }

    @Test
    @DisplayName("UserStateService should fall back to limited business unit users when entitlements are absent")
    void userStateService_getUserStateByUsername_fallsBackToLimitedBusinessUnits() {
        UserState result = userStateService.getUserStateByUsername("no-go-user@HMCTS.NET");

        assertEquals(500000006L, result.getUserId());
        assertEquals("no-go-user@HMCTS.NET", result.getUserName());
        assertEquals(2, result.getBusinessUnitUser().size());
        assertTrue(result.getBusinessUnitUser().stream().allMatch(businessUnitUser ->
            businessUnitUser.getPermissions().isEmpty()));
    }

    @Test
    @DisplayName("UserStateService should reject unknown usernames when developer mode is disabled")
    void userStateService_getUserStateByUsername_whenUserMissing_throwsAccessDenied() {
        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            () -> userStateService.getUserStateByUsername("missing-user@HMCTS.NET")
        );

        assertEquals("No authorised user with username 'missing-user@HMCTS.NET' found", exception.getMessage());
    }
}
