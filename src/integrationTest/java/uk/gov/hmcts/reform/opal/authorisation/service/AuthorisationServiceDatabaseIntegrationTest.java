package uk.gov.hmcts.reform.opal.authorisation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.opal.common.user.authentication.model.SecurityToken;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.insertData/insert_user_state_data.sql", executionPhase = BEFORE_TEST_CLASS)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create-drop"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("AuthorisationService database integration tests")
class AuthorisationServiceDatabaseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthorisationService authorisationService;

    @MockitoBean
    private AccessTokenService accessTokenService;

    @Test
    @DisplayName("Should build a security token with DB-backed user state when the token username exists")
    void getSecurityToken_whenPreferredUsernameExists_returnsDatabaseBackedUserState() {
        String accessToken = "access-token";
        when(accessTokenService.extractPreferredUsername(accessToken)).thenReturn("opal-test@HMCTS.NET");

        SecurityToken result = authorisationService.getSecurityToken(accessToken);

        assertNotNull(result);
        assertEquals(accessToken, result.getAccessToken());
        assertNotNull(result.getUserState());
        assertEquals(500000000L, result.getUserState().getUserId());
        assertEquals("opal-test@HMCTS.NET", result.getUserState().getUserName());
        assertEquals(7, result.getUserState().getBusinessUnitUser().size());
        assertTrue(result.getUserState().getBusinessUnitUserForBusinessUnit((short) 73).isPresent());
        assertTrue(result.getUserState().getBusinessUnitUserForBusinessUnit((short) 73)
                       .orElseThrow()
                       .getPermissions()
                       .isEmpty());

        verify(accessTokenService).extractPreferredUsername(accessToken);
    }

    @Test
    @DisplayName("Should return a security token without user state when the token has no preferred username")
    void getSecurityToken_whenPreferredUsernameMissing_returnsTokenWithoutUserState() {
        String accessToken = "access-token-without-username";
        when(accessTokenService.extractPreferredUsername(accessToken)).thenReturn(null);

        SecurityToken result = authorisationService.getSecurityToken(accessToken);

        assertNotNull(result);
        assertEquals(accessToken, result.getAccessToken());
        assertNull(result.getUserState());

        verify(accessTokenService).extractPreferredUsername(accessToken);
    }
}
