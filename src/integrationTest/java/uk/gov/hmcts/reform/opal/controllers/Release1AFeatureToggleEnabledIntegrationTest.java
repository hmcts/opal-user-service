package uk.gov.hmcts.reform.opal.controllers;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import uk.gov.hmcts.opal.common.controllers.advice.OpalGlobalExceptionHandler;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureToggleAspect;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;
import uk.gov.hmcts.opal.common.user.authorisation.client.dto.UserStateV2Dto;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

@WebMvcTest
@ActiveProfiles({"integration"})
@ContextConfiguration(classes = {
    UserPermissionsV2Controller.class,
    FeatureToggleAspect.class,
    OpalGlobalExceptionHandler.class,
    Release1AFeatureToggleEnabledIntegrationTest.TestAopConfiguration.class})
@Import(Release1AFeatureToggleEnabledIntegrationTest.TestAopConfiguration.class)
@DisplayName("Release 1A gated endpoints reach the controller when enabled")
class Release1AFeatureToggleEnabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeatureToggleApi featureToggleApi;

    @MockitoBean
    private UserPermissionsService userPermissionsService;

    @BeforeEach
    void setUpFeatureEnabled() {
        when(featureToggleApi.isFeatureEnabledWithPropertyValueDefault(anyString(), anyString(), anyBoolean()))
            .thenReturn(true);
        when(userPermissionsService.getUserStateV2(false)).thenReturn(new UserStateV2Dto());
    }

    @Test
    @DisplayName("GET /v2/users/{userId}/state returns 200 for user id 0 when enabled")
    void getUserStateV2_whenRelease1aEnabledAndUserIdIsZero_returnsOk() throws Exception {
        mockMvc.perform(get("/v2/users/0/state"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(userPermissionsService).getUserStateV2(false);
    }

    @TestConfiguration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestAopConfiguration {
    }
}
