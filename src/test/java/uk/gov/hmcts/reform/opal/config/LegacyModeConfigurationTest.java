package uk.gov.hmcts.reform.opal.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class LegacyModeConfigurationTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isLegacyMode_returnsLaunchDarklyFlagValue(boolean legacyMode) {
        FeatureToggleApi featureToggleApi = mock(FeatureToggleApi.class);
        doReturn(legacyMode).when(featureToggleApi).isFeatureEnabled("is-legacy-mode");
        LegacyModeConfiguration config = new LegacyModeConfiguration(featureToggleApi);
        assertThat(config.isLegacyMode()).isEqualTo(legacyMode);
    }
}
