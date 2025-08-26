package uk.gov.hmcts.reform.opal.launchdarkly;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j(topic = "opal.FeatureToggleService")
@Service
@RequiredArgsConstructor
public class FeatureToggleService {

    private final FeatureToggleApi featureToggleApi;

    public boolean isFeatureEnabled(String feature) {
        return this.featureToggleApi.isFeatureEnabled(feature);
    }

    public String getFeatureValue(String feature) {
        return this.featureToggleApi.getFeatureValue(feature, "");
    }
}
