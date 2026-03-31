package uk.gov.hmcts.reform.opal.authentication.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.opal.common.user.authentication.model.SecurityToken;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.authentication.service.AuthenticationService;

import java.net.URI;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"integration"})
@DisplayName("AuthenticationInternalUserController integration tests")
class AuthenticationInternalUserControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private AccessTokenService accessTokenService;

    @Test
    @DisplayName("Should redirect to the login URI when no authorisation header is provided")
    void loginOrRefresh_whenNoAuthorisationHeader_redirectsToLoginUri() throws Exception {
        when(authenticationService.getLoginUri("https://frontend/callback"))
            .thenReturn(URI.create("https://idam/login"));

        mockMvc.perform(get("/internal-user/login-or-refresh")
                            .param("redirect_uri", "https://frontend/callback"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "https://idam/login"));

        verify(authenticationService).getLoginUri("https://frontend/callback");
    }

    @Test
    @DisplayName("Should redirect to the landing URI when a bearer token is provided")
    void loginOrRefresh_whenAuthorisationHeaderPresent_redirectsToAuthenticationServiceUri() throws Exception {
        when(authenticationService.loginOrRefresh("raw-token", "https://frontend/callback"))
            .thenReturn(URI.create("https://frontend/landing"));

        mockMvc.perform(get("/internal-user/login-or-refresh")
                            .header("Authorization", "Bearer raw-token")
                            .param("redirect_uri", "https://frontend/callback"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "https://frontend/landing"));

        verify(authenticationService).loginOrRefresh("raw-token", "https://frontend/callback");
    }

    @Test
    @DisplayName("Should return a security token payload from handle-oauth-code")
    void handleOauthCode_returnsSecurityTokenPayload() throws Exception {
        String accessToken = "issued-access-token";
        UserState userState = UserState.builder()
            .userId(123L)
            .userName("opal-test@HMCTS.NET")
            .businessUnitUser(Collections.emptySet())
            .build();
        SecurityToken securityToken = SecurityToken.builder()
            .accessToken(accessToken)
            .userState(userState)
            .build();

        when(authenticationService.handleOauthCode("valid-code")).thenReturn(accessToken);
        when(accessTokenService.extractPreferredUsername(accessToken)).thenReturn("opal-test@HMCTS.NET");
        when(authenticationService.getSecurityToken(accessToken)).thenReturn(securityToken);

        mockMvc.perform(post("/internal-user/handle-oauth-code")
                            .param("code", "valid-code")
                            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").value(accessToken))
            .andExpect(jsonPath("$.user_state.user_id").value(123))
            .andExpect(jsonPath("$.user_state.user_name").value("opal-test@HMCTS.NET"));

        verify(authenticationService).handleOauthCode("valid-code");
        verify(accessTokenService).extractPreferredUsername(accessToken);
        verify(authenticationService).getSecurityToken(accessToken);
    }

    @Test
    @DisplayName("Should redirect to the logout URI with the bearer token stripped")
    void logout_redirectsToAuthenticationServiceUri() throws Exception {
        when(accessTokenService.extractPreferredUsername("logout-token")).thenReturn("opal-test@HMCTS.NET");
        when(authenticationService.logout("logout-token", "https://frontend/logout"))
            .thenReturn(URI.create("https://idam/logout"));

        mockMvc.perform(get("/internal-user/logout")
                            .header("Authorization", "Bearer logout-token")
                            .param("redirect_uri", "https://frontend/logout"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "https://idam/logout"));

        verify(accessTokenService).extractPreferredUsername("logout-token");
        verify(authenticationService).logout("logout-token", "https://frontend/logout");
    }

    @Test
    @DisplayName("Should redirect to the reset password URI")
    void resetPassword_redirectsToAuthenticationServiceUri() throws Exception {
        when(authenticationService.resetPassword("https://frontend/reset"))
            .thenReturn(URI.create("https://idam/reset"));

        mockMvc.perform(get("/internal-user/reset-password")
                            .param("redirect_uri", "https://frontend/reset"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "https://idam/reset"));

        verify(authenticationService).resetPassword("https://frontend/reset");
    }
}
