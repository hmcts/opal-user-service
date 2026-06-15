package uk.gov.hmcts.reform.opal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.reform.opal.util.FeatureFlags.defaultValueProperty;
import static uk.gov.hmcts.reform.opal.util.FeatureFlags.RELEASE_1A;
import static uk.gov.hmcts.reform.opal.util.FeatureFlags.RELEASE_1A_ENABLED_PROPERTY;
import static uk.gov.hmcts.reform.opal.util.FeatureFlags.IS_LEGACY_MODE;
import static uk.gov.hmcts.reform.opal.util.FeatureFlags.IS_LEGACY_MODE_PROPERTY;

import org.junit.jupiter.api.Test;

class FeatureFlagsTest {

    @Test
    void testDefaultValueProperty() {
        assertEquals("launchdarkly.default-flag-values.release-1b", defaultValueProperty("release-1b"));
    }

    @Test
    void testRelease1aEnabledProperty() {
        assertEquals(RELEASE_1A_ENABLED_PROPERTY, defaultValueProperty(RELEASE_1A));
    }

    @Test
    void testIsLegacyModeEnabledProperty() {
        assertEquals(IS_LEGACY_MODE_PROPERTY, defaultValueProperty(IS_LEGACY_MODE));
    }
}
