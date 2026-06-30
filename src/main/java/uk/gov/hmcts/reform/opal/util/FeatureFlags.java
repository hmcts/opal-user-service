package uk.gov.hmcts.reform.opal.util;

public final class FeatureFlags {

    public static final String DEFAULT_VALUE_PROPERTY_PREFIX = "launchdarkly.default-flag-values.";
    public static final String RELEASE_1A = "release-1a";
    public static final String RELEASE_1A_ENABLED_PROPERTY = DEFAULT_VALUE_PROPERTY_PREFIX + RELEASE_1A;
    public static final String IS_LEGACY_MODE = "is-legacy-mode";
    public static final String IS_LEGACY_MODE_PROPERTY = DEFAULT_VALUE_PROPERTY_PREFIX + IS_LEGACY_MODE;

    private FeatureFlags() {
    }

    public static String defaultValueProperty(String featureFlag) {
        return DEFAULT_VALUE_PROPERTY_PREFIX + featureFlag;
    }
}
