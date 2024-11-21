package uk.gov.hmcts.reform.opal.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.opal.authentication.model.AccessTokenResponse;
import uk.gov.hmcts.reform.opal.authentication.model.SecurityToken;
import uk.gov.hmcts.reform.opal.authentication.service.AccessTokenService;
import uk.gov.hmcts.reform.opal.authorisation.service.AuthorisationService;
import uk.gov.hmcts.reform.opal.dto.AppMode;
import uk.gov.hmcts.reform.opal.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.opal.service.DynamicConfigService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(
    classes =
        {
            TestingSupportController.class,
            DynamicConfigService.class,
            FeatureToggleService.class
        },
    properties = {
        "opal.testing-support-endpoints.enabled=true"
    }
)
class TestingSupportControllerTest {

    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String TEST_TOKEN = "testToken";

    @Autowired
    private TestingSupportController controller;

    @MockBean
    private DynamicConfigService configService;

    @MockBean
    private FeatureToggleService featureToggleService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private AuthorisationService authorisationService;

    @Test
    void getAppMode() {
        AppMode mode = AppMode.builder().mode("opal").build();
        when(configService.getAppMode()).thenReturn(mode);

        ResponseEntity<AppMode> response = controller.getAppMode();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("opal", response.getBody().getMode());
    }

    @Test
    void updateMode() {
        AppMode mode = AppMode.builder().mode("legacy").build();
        when(configService.updateAppMode(any())).thenReturn(mode);

        ResponseEntity<AppMode> response = controller.updateMode(mode);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("legacy", response.getBody().getMode());

    }

    @Test
    void isFeatureEnabled() {
        when(featureToggleService.isFeatureEnabled("my-feature")).thenReturn(true);

        ResponseEntity<Boolean> response = controller.isFeatureEnabled("my-feature");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody());
    }

    @Test
    void getFeatureFlagValue() {
        when(featureToggleService.getFeatureValue("my-feature")).thenReturn("value");

        ResponseEntity<String> response = controller.getFeatureValue("my-feature");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("value", response.getBody());
    }

    @Test
    void getToken_shouldReturnResponse() {
        // Arrange
        AccessTokenResponse expectedResponse = AccessTokenResponse.builder().accessToken(TEST_TOKEN).build();
        when(accessTokenService.getTestUserToken())
            .thenReturn(expectedResponse);
        SecurityToken securityToken = SecurityToken.builder().accessToken(TEST_TOKEN).build();
        when(authorisationService.getSecurityToken(TEST_TOKEN)).thenReturn(securityToken);

        // Call the controller method
        ResponseEntity<SecurityToken> responseEntity = controller.getToken();

        // Verify the response
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(securityToken, responseEntity.getBody());
    }

    @Test
    void getToken_shouldHandleExceptions() {
        // Arrange
        when(accessTokenService.getTestUserToken())
            .thenThrow(new RuntimeException("Error!"));

        // Act and Assert
        assertThrows(
            RuntimeException.class,
            () -> controller.getToken()
        );
    }

    @Test
    void getTokenForUser_shouldReturnResponse() {
        // Arrange
        AccessTokenResponse expectedResponse = AccessTokenResponse.builder().accessToken(TEST_TOKEN).build();
        when(accessTokenService.getTestUserToken(TEST_USER_EMAIL))
            .thenReturn(expectedResponse);

        SecurityToken securityToken = SecurityToken.builder().accessToken(TEST_TOKEN).build();
        when(authorisationService.getSecurityToken(TEST_TOKEN)).thenReturn(securityToken);

        // Act
        ResponseEntity<SecurityToken> response = controller.getTokenForUser(TEST_USER_EMAIL);

        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(securityToken, response.getBody());
    }

    @Test
    void getTokenForUser_shouldHandleExceptions() {
        // Arrange
        when(accessTokenService.getTestUserToken(TEST_USER_EMAIL))
            .thenThrow(new RuntimeException("Error!"));

        // Act and Assert
        assertThrows(
            RuntimeException.class,
            () -> controller.getTokenForUser(TEST_USER_EMAIL)
        );
    }

    @Test
    void parseToken_shouldReturnEmail() {
        String bearerToken = "Bearer token";
        when(accessTokenService.extractPreferredUsername(bearerToken)).thenReturn("my@email.com");

        ResponseEntity<String> response = controller.parseToken(bearerToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("my@email.com", response.getBody());
    }
}
