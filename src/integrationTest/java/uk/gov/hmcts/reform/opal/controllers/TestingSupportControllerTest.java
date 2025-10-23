package uk.gov.hmcts.reform.opal.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.opal.authentication.model.AccessTokenResponse;
import uk.gov.hmcts.reform.opal.authentication.service.AccessTokenService;
import uk.gov.hmcts.reform.opal.launchdarkly.FeatureToggleService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@WebMvcTest
@ContextConfiguration(classes = TestingSupportController.class)
@ActiveProfiles({"integration"})
class TestingSupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeatureToggleService featureToggleService;

    @MockitoBean
    private AccessTokenService accessTokenService;


    @Test
    void testIsFeatureEnabled() throws Exception {
        when(featureToggleService.isFeatureEnabled(anyString())).thenReturn(true);

        mockMvc.perform(get("/testing-support/launchdarkly/bool/testFeature"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isBoolean());
    }

    @Test
    void testGetFeatureValue() throws Exception {
        String featureValue = "testValue";
        when(featureToggleService.getFeatureValue(anyString())).thenReturn(featureValue);

        mockMvc.perform(get("/testing-support/launchdarkly/string/testFeature"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(featureValue));
    }

    @Test
    void testGetToken() throws Exception {
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        accessTokenResponse.setAccessToken("testAccessToken");

        when(accessTokenService.getTestUserToken()).thenReturn(accessTokenResponse);

        mockMvc.perform(get("/testing-support/token/test-user"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").value("testAccessToken"))
            .andExpect(jsonPath("$.user_state").doesNotExist());

    }

    @Test
    void testGetTokenForUser() throws Exception {
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        accessTokenResponse.setAccessToken("testAccessToken");

        when(accessTokenService.getTestUserToken(anyString())).thenReturn(accessTokenResponse);

        mockMvc.perform(get("/testing-support/token/user")
                .header("X-User-Email", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").value("testAccessToken"))
            .andExpect(jsonPath("$.user_state").doesNotExist());
    }

    @Test
    void testParseToken() throws Exception {
        String token = "Bearer testToken";

        when(accessTokenService.extractPreferredUsername(token)).thenReturn("testUser");

        mockMvc.perform(get("/testing-support/token/parse")
                .header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("testUser"));
    }

    @Test
    void testGetTokenForUserFailure() throws Exception {
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        accessTokenResponse.setAccessToken("testAccessToken");

        when(accessTokenService.getTestUserToken(anyString())).thenReturn(accessTokenResponse);

        mockMvc.perform(get("/testing-support/token/user")
                .header("X-User-Email", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").value("testAccessToken"))
            .andExpect(jsonPath("$.user_state").doesNotExist());
    }

    @Test
    void testGetToken_testUser_strictShape_onlyAccessToken() throws Exception {
        AccessTokenResponse resp = new AccessTokenResponse();
        resp.setAccessToken("testAccessToken");
        when(accessTokenService.getTestUserToken()).thenReturn(resp);

        mockMvc.perform(get("/testing-support/token/test-user"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").value("testAccessToken"))
            // Ensure no additional fields appear now or in the future:
            .andExpect(jsonPath("$.*", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void testGetToken_forUser_strictShape_onlyAccessToken() throws Exception {
        AccessTokenResponse resp = new AccessTokenResponse();
        resp.setAccessToken("testAccessToken");
        when(accessTokenService.getTestUserToken(anyString())).thenReturn(resp);

        mockMvc.perform(get("/testing-support/token/user")
                            .header("X-User-Email", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").value("testAccessToken"))
            .andExpect(jsonPath("$.*", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void testGetTokenForUser_missingEmail_returnsBadRequest() throws Exception {
        when(accessTokenService.getTestUserToken(isNull()))
            .thenThrow(new IllegalArgumentException("email required"));

        // Act + Assert
        mockMvc.perform(get("/testing-support/token/user"))
            .andExpect(status().isBadRequest());
    }


    @Test
    void testGetTokenForUser_unknownEmail_returnsNotFound() throws Exception {
        when(accessTokenService.getTestUserToken("nobody@example.com"))
            .thenThrow(new ResponseStatusException(NOT_FOUND, "Test user not found"));

        mockMvc.perform(get("/testing-support/token/user")
                            .header("X-User-Email", "nobody@example.com"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetToken_testUser_upstreamFailure_returns500() throws Exception {
        when(accessTokenService.getTestUserToken())
            .thenThrow(new ResponseStatusException(INTERNAL_SERVER_ERROR, "upstream boom"));

        mockMvc.perform(get("/testing-support/token/test-user"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetToken_alwaysJsonContentType() throws Exception {
        AccessTokenResponse resp = new AccessTokenResponse();
        resp.setAccessToken("abc");
        when(accessTokenService.getTestUserToken()).thenReturn(resp);

        mockMvc.perform(get("/testing-support/token/test-user"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
