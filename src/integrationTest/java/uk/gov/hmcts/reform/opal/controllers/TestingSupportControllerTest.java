package uk.gov.hmcts.reform.opal.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.opal.common.user.authentication.model.AccessTokenResponse;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.reform.opal.authentication.service.TestingSupportAccessTokenService;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.launchdarkly.FeatureToggleService;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = TestingSupportController.class)
@ActiveProfiles({"integration"})
class TestingSupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeatureToggleService featureToggleService;

    @MockitoBean
    private TestingSupportAccessTokenService testingSupportAccessTokenService;

    @MockitoBean
    private AccessTokenService accessTokenService;

    @MockitoBean
    private UserService userService;

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

        when(testingSupportAccessTokenService.getTestUserToken()).thenReturn(accessTokenResponse);

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

        when(testingSupportAccessTokenService.getTestUserToken(anyString())).thenReturn(accessTokenResponse);

        mockMvc.perform(get("/testing-support/token/user").header("X-User-Email", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").value("testAccessToken"))
            .andExpect(jsonPath("$.user_state").doesNotExist());
    }

    @Test
    void testParseToken() throws Exception {
        String token = "Bearer testToken";

        when(accessTokenService.extractPreferredUsername(token)).thenReturn("testUser");

        mockMvc.perform(get("/testing-support/token/parse").header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("testUser"));
    }

    @Test
    void testGetTokenForUserFailure() throws Exception {
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        accessTokenResponse.setAccessToken("testAccessToken");

        when(testingSupportAccessTokenService.getTestUserToken(anyString())).thenReturn(accessTokenResponse);

        mockMvc.perform(get("/testing-support/token/user").header("X-User-Email", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").value("testAccessToken"))
            .andExpect(jsonPath("$.user_state").doesNotExist());
    }

    @Test
    void testGetToken_testUser_strictShape_onlyAccessToken() throws Exception {
        AccessTokenResponse resp = new AccessTokenResponse();
        resp.setAccessToken("testAccessToken");
        when(testingSupportAccessTokenService.getTestUserToken()).thenReturn(resp);

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
        when(testingSupportAccessTokenService.getTestUserToken(anyString())).thenReturn(resp);

        mockMvc.perform(get("/testing-support/token/user").header("X-User-Email", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").value("testAccessToken"))
            .andExpect(jsonPath("$.*", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void testGetTokenForUser_missingEmail_returnsBadRequest() throws Exception {
        when(testingSupportAccessTokenService.getTestUserToken(isNull()))
            .thenThrow(new IllegalArgumentException("email required"));

        // Act + Assert
        mockMvc.perform(get("/testing-support/token/user"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetTokenForUser_unknownEmail_returnsNotFound() throws Exception {
        when(testingSupportAccessTokenService.getTestUserToken("nobody@example.com"))
            .thenThrow(new ResponseStatusException(NOT_FOUND, "Test user not found"));

        mockMvc.perform(get("/testing-support/token/user").header("X-User-Email", "nobody@example.com"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetToken_testUser_upstreamFailure_returns500() throws Exception {
        when(testingSupportAccessTokenService.getTestUserToken())
            .thenThrow(new ResponseStatusException(INTERNAL_SERVER_ERROR, "upstream boom"));

        mockMvc.perform(get("/testing-support/token/test-user"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetToken_alwaysJsonContentType() throws Exception {
        AccessTokenResponse resp = new AccessTokenResponse();
        resp.setAccessToken("abc");
        when(testingSupportAccessTokenService.getTestUserToken()).thenReturn(resp);

        mockMvc.perform(get("/testing-support/token/test-user"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testAddOrReplaceRoleInformationOnUser() throws Exception {
        mockMvc.perform(put("/testing-support/users/987/roles/101")
            .contentType(MediaType.APPLICATION_JSON)
            .content("[1,4,5]"))
            .andExpect(status().isNoContent());

        verify(userService).addOrReplaceRoleInformationOnUser(
            987L, 101L, Set.of((short) 1, (short) 4, (short) 5), userService);
    }

    @Test
    void testActivateUser() throws Exception {
        OffsetDateTime activationDate = OffsetDateTime.now();

        UserEntity user = UserEntity.builder().userId(987L).build();
        when(userService.getUser(987L)).thenReturn(user);

        mockMvc.perform(patch("/testing-support/users/987")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                {
                  "activationDate": "%s"
                }
                """.formatted(activationDate)))
            .andExpect(status().isNoContent());

        verify(userService).activateUser(
            eq(user.getUserId()),
            argThat(actual -> actual.toInstant().equals(activationDate.toInstant()))
        );
    }
}
