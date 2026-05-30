package uk.gov.hmcts.reform.opal.service.opal;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.common.exceptions.standard.UnauthorizedException;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.service.synchronise.TestHelperUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private UserStateService userStateService;

    @Test
    @SneakyThrows
    @DisplayName("UserService should return all business unit users including those without permissions")
    void userService_getUserStateByUsername_returnsAllBusinessUnitUsers() {
        OpalJwtAuthenticationToken authenticationToken = TestHelperUtil
            .createJwtPrincipal("k9LpT2xVqR8m", "opal-test@HMCTS.NET", "Pablo");
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        UserStateV2 result = userStateService.getUserStateUsingAuthToken();

        assertEquals(500000000L, result.getUserId());
        assertEquals("opal-test@HMCTS.NET", result.getUsername());
        DomainBusinessUnitUsers domainBusinessUnitUsers = result.getDomainBusinessUnitUsers(Domain.FINES);
        assertEquals(7, domainBusinessUnitUsers.getBusinessUnitUsers().size());

        assertTrue(domainBusinessUnitUsers.getBusinessUnitUserForBusinessUnit((short) 70).isPresent());
        assertTrue(domainBusinessUnitUsers.getBusinessUnitUserForBusinessUnit((short) 68).isPresent());
        assertTrue(domainBusinessUnitUsers.getBusinessUnitUserForBusinessUnit((short) 73).isPresent());
        assertTrue(domainBusinessUnitUsers.getBusinessUnitUserForBusinessUnit((short) 71).isPresent());
        assertTrue(domainBusinessUnitUsers.getBusinessUnitUserForBusinessUnit((short) 67).isPresent());
        assertTrue(domainBusinessUnitUsers.getBusinessUnitUserForBusinessUnit((short) 69).isPresent());
        assertTrue(domainBusinessUnitUsers.getBusinessUnitUserForBusinessUnit((short) 61).isPresent());

        Optional<BusinessUnitUser> businessUnit70 =
            domainBusinessUnitUsers.getBusinessUnitUserForBusinessUnit((short) 70);
        Optional<BusinessUnitUser> businessUnit73 =
            domainBusinessUnitUsers.getBusinessUnitUserForBusinessUnit((short) 73);

        assertTrue(businessUnit70.isPresent());
        //BU 70 has two roles with 6 unqiue perms between them
        assertEquals(6, businessUnit70.get().getPermissions().size());
        assertTrue(businessUnit73.isPresent());
        assertTrue(businessUnit73.get().getPermissions().isEmpty());
    }


    @Test
    @DisplayName("UserStateService should reject unknown usernames when developer mode is disabled")
    void userStateService_getUserStateByUsername_whenUserMissing_throwsAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(null);
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> userStateService.getUserStateUsingAuthToken()
        );
        assertEquals("Current user is not authenticated with OpalJwtAuthenticationToken", exception.getMessage());
    }
}
