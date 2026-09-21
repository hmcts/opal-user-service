package uk.gov.hmcts.reform.opal.steps;

import io.cucumber.java.AfterAll;
import io.cucumber.java.en.When;
import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.reform.opal.utils.TestHttpClient;
import uk.gov.hmcts.reform.opal.utils.TestHttpClient.TestHttpResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines steps and helpers for obtaining bearer tokens used by functional scenarios.
 */
public class BearerTokenStepDef extends BaseStepDef {

    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();
    private static final ThreadLocal<String> ALT_TOKEN = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();

    /**
     * Returns a cached access token for the supplied user, fetching it on first use.
     *
     * @param user email address of the user whose token is required.
     * @return bearer token issued for the supplied user.
     */
    public String getAccessTokenForUser(String user) {
        return tokenCache.computeIfAbsent(user, BearerTokenStepDef::fetchAccessToken);
    }

    /**
     * Fetches an access token for the supplied user.
     *
     * @param user email address of the user whose token is required.
     * @return bearer token issued for the supplied user.
     */
    private static String fetchAccessToken(String user) {
        return fetchToken(user);
    }

    /**
     * Calls the testing-support token endpoint for the supplied user and returns the access token
     * from the JSON response body.
     *
     * @param user email address of the user whose token is required.
     * @return access token returned by the testing-support endpoint.
     */
    private static String fetchToken(String user) {
        TestHttpResponse response = TestHttpClient.get(
            getTestUrl() + "/testing-support/token/user",
            Map.of(
                "Accept", "*/*",
                "Content-Type", "application/json",
                "X-User-Email", user
            )
        );

        Serenity.setSessionVariable("LAST_RESPONSE").to(response);

        return response.jsonPath("access_token");
    }

    /**
     * Returns the token currently active for the scenario, preferring any explicit override.
     *
     * @return active bearer token for the current scenario thread.
     */
    public static String getToken() {
        return ALT_TOKEN.get() != null ? ALT_TOKEN.get() : TOKEN.get();
    }

    /**
     * Sets the scenario token to one issued for the supplied user.
     *
     * @param user email address of the user to test as.
     */
    @When("I am testing as the {string} user")
    public void setTokenWithUser(String user) {
        ALT_TOKEN.set(getAccessTokenForUser(user));
    }

    /**
     * Clears the cached and thread-local token state after the test run.
     */
    @SuppressWarnings("unused")
    @AfterAll
    public static void clearCache() {
        tokenCache.clear();
        ALT_TOKEN.remove();
        TOKEN.remove();
    }
}
