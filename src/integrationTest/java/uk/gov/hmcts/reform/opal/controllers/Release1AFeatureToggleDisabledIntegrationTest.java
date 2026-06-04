package uk.gov.hmcts.reform.opal.controllers;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.opal.common.controllers.advice.OpalGlobalExceptionHandler;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureToggleAspect;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;
import uk.gov.hmcts.opal.common.user.authentication.model.AccessTokenResponse;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.reform.opal.authentication.service.TestingSupportAccessTokenService;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;
import uk.gov.hmcts.reform.opal.service.opal.UserService;

@WebMvcTest
@ActiveProfiles({"integration"})
@ContextConfiguration(classes = {
    TestingSupportController.class, UserPermissionsController.class, UserPermissionsV2Controller.class,
    FeatureToggleAspect.class,
    OpalGlobalExceptionHandler.class, Release1AFeatureToggleDisabledIntegrationTest.TestAopConfiguration.class})
@Import(Release1AFeatureToggleDisabledIntegrationTest.TestAopConfiguration.class)
@DisplayName("Release 1A gated endpoints return 405 when disabled")
class Release1AFeatureToggleDisabledIntegrationTest {

    private static final String AUTHORIZATION = "Authorization";
    private static final String AUTHORIZATION_VALUE = "Bearer test";
    private static final String IF_MATCH = "If-Match";
    private static final String IF_MATCH_VALUE = "\"0\"";
    private static final String X_USER_EMAIL = "X-User-Email";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeatureToggleApi featureToggleApi;

    @MockitoBean
    private TestingSupportAccessTokenService testingSupportAccessTokenService;

    @MockitoBean
    private AccessTokenService accessTokenService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserPermissionsService userPermissionsService;

    @MockitoBean
    private UserStateClientService userStateClientService;

    @MockitoBean
    private SecurityEventLoggingService securityEventLoggingService;

    @BeforeEach
    void setUpFeatureDisabled() {
        when(featureToggleApi.isFeatureEnabledWithPropertyValueDefault(anyString(), anyString(), anyBoolean()))
            .thenReturn(false);
        AccessTokenResponse accessTokenResponse = AccessTokenResponse.builder().accessToken("testAccessToken").build();
        when(testingSupportAccessTokenService.getTestUserToken()).thenReturn(accessTokenResponse);
        when(testingSupportAccessTokenService.getTestUserToken(anyString())).thenReturn(accessTokenResponse);
        when(accessTokenService.extractPreferredUsername(anyString())).thenReturn("test-user@dev.platform.hmcts.net");
    }

    static Stream<Arguments> release1aEndpoints() {
        return Stream.of(
            args("GET /testing-support/token/test-user", get("/testing-support/token/test-user")),
            args("GET /testing-support/token/user",
                get("/testing-support/token/user").header(X_USER_EMAIL, "opal-test@dev.platform.hmcts.net")),
            args("GET /testing-support/token/parse",
                get("/testing-support/token/parse").header(AUTHORIZATION, AUTHORIZATION_VALUE)),
            args("POST /users", post("/users").header(AUTHORIZATION, AUTHORIZATION_VALUE)),
            args("PUT /users", put("/users")
                .header(AUTHORIZATION, AUTHORIZATION_VALUE)
                .header(IF_MATCH, IF_MATCH_VALUE)),
            args("PUT /users/{userId}", put("/users/500000002")
                .header(AUTHORIZATION, AUTHORIZATION_VALUE)
                .header(IF_MATCH, IF_MATCH_VALUE)),
            args("GET /v2/users/{userId}/state", get("/v2/users/0/state"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("release1aEndpoints")
    void shouldReturnFeatureDisabledWhenRelease1aIsDisabled(String description,
                                                            MockHttpServletRequestBuilder request)
        throws Exception {
        mockMvc.perform(request)
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Feature Disabled"))
            .andExpect(jsonPath("$.detail").value("The requested feature is not currently available"));
    }

    private static Arguments args(String description, MockHttpServletRequestBuilder request) {
        return Arguments.of(description, request);
    }

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestAopConfiguration {
    }
}
