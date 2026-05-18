package uk.gov.hmcts.opal.steps;

/**
 * Provides shared test-environment configuration for functional step definition classes.
 */
public class BaseStepDef {

    private static final String TEST_URL = System.getenv().getOrDefault("TEST_URL", "http://localhost:4555");

    /**
     * Returns the base URL for the service under test.
     *
     * @return configured test URL, defaulting to the local service.
     */
    protected static String getTestUrl() {
        return TEST_URL;
    }
}
