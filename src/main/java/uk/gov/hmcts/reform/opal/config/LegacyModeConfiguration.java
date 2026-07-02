package uk.gov.hmcts.reform.opal.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;

@Component
@RequiredArgsConstructor
public class LegacyModeConfiguration {

    private final FeatureToggleApi featureToggleApi;

    private static final String IS_LEGACY_MODE_FLAG = "is-legacy-mode";

    public boolean isLegacyMode() {
        return featureToggleApi.isFeatureEnabled(IS_LEGACY_MODE_FLAG);
    }
}
