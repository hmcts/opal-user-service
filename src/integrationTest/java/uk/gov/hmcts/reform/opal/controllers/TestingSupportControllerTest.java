package uk.gov.hmcts.reform.opal.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.opal.authentication.model.AccessTokenResponse;
import uk.gov.hmcts.reform.opal.authentication.model.SecurityToken;
import uk.gov.hmcts.reform.opal.authentication.service.AccessTokenService;
import uk.gov.hmcts.reform.opal.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.reform.opal.authorisation.model.Permission;
import uk.gov.hmcts.reform.opal.authorisation.model.UserState;
import uk.gov.hmcts.reform.opal.authorisation.service.AuthorisationService;
import uk.gov.hmcts.reform.opal.launchdarkly.FeatureToggleService;

import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = TestingSupportController.class)
@ActiveProfiles({"integration"})
class TestingSupportControllerTest {

    private static final String TEST_TOKEN = "testToken";
    private static final UserState USER_STATE = UserState.builder()
        .userName("name")
        .userId(123L)
        .businessUnitUser(Set.of(BusinessUnitUser.builder()
            .businessUnitId((short) 123)
            .businessUnitUserId("BU123")
            .permissions(Set.of(
                Permission.builder()
                    .permissionId(1L)
                    .permissionName("Notes")
                    .build()))
            .build()))
        .build();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeatureToggleService featureToggleService;

    @MockBean
    private AccessTokenService accessTokenService;

    @MockBean
    private AuthorisationService authorisationService;


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

        SecurityToken securityToken = SecurityToken.builder()
            .accessToken(TEST_TOKEN)
            .userState(USER_STATE)
            .build();

        when(authorisationService.getSecurityToken("testAccessToken")).thenReturn(securityToken);

        mockMvc.perform(get("/testing-support/token/test-user"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").value("testToken"))
            .andExpect(jsonPath("$.user_state.user_name").value("name"))
            .andExpect(jsonPath("$.user_state.user_id").value("123"))
            .andExpect(jsonPath("$.user_state.business_unit_user[0].business_unit_id")
                .value("123"))
            .andExpect(jsonPath("$.user_state.business_unit_user[0].business_unit_user_id")
                .value("BU123"))
            .andExpect(jsonPath("$.user_state.business_unit_user[0].permissions[0].permission_id")
                .value("1"))
            .andExpect(
                jsonPath("$.user_state.business_unit_user[0].permissions[0].permission_name")
                    .value("Notes"));

    }

    @Test
    void testGetTokenForUser() throws Exception {
        AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        accessTokenResponse.setAccessToken("testAccessToken");

        when(accessTokenService.getTestUserToken(anyString())).thenReturn(accessTokenResponse);

        SecurityToken securityToken = SecurityToken.builder()
            .accessToken(TEST_TOKEN)
            .userState(USER_STATE)
            .build();
        when(authorisationService.getSecurityToken("testAccessToken")).thenReturn(securityToken);

        mockMvc.perform(get("/testing-support/token/user")
                .header("X-User-Email", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").value("testToken"))
            .andExpect(jsonPath("$.user_state.user_name").value("name"))
            .andExpect(jsonPath("$.user_state.user_id").value("123"))
            .andExpect(jsonPath("$.user_state.business_unit_user[0].business_unit_id")
                .value("123"))
            .andExpect(jsonPath("$.user_state.business_unit_user[0].business_unit_user_id")
                .value("BU123"))
            .andExpect(jsonPath("$.user_state.business_unit_user[0].permissions[0].permission_id")
                .value("1"))
            .andExpect(
                jsonPath("$.user_state.business_unit_user[0].permissions[0].permission_name")
                    .value("Notes"));
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

        SecurityToken securityToken = SecurityToken.builder()
            .accessToken(TEST_TOKEN)
            .userState(USER_STATE)
            .build();
        when(authorisationService.getSecurityToken("testAccessToken")).thenReturn(securityToken);

        mockMvc.perform(get("/testing-support/token/user")
                .header("X-User-Email", "test@example.com"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.access_token").value("testToken"))
            .andExpect(jsonPath("$.user_state.user_name").value("name"))
            .andExpect(jsonPath("$.user_state.user_id").value("123"))
            .andExpect(jsonPath("$.user_state.business_unit_user[0].business_unit_id")
                .value("123"))
            .andExpect(jsonPath("$.user_state.business_unit_user[0].business_unit_user_id")
                .value("BU123"))
            .andExpect(jsonPath("$.user_state.business_unit_user[0].permissions[0].permission_id")
                .value("1"))
            .andExpect(
                jsonPath("$.user_state.business_unit_user[0].permissions[0].permission_name")
                    .value("Notes"));
    }
}
