package uk.gov.hmcts.opal.steps;

import io.cucumber.java.AfterAll;
import io.cucumber.java.en.When;
import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BearerTokenStepDef extends BaseStepDef {

    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();
    private static final ThreadLocal<String> ALT_TOKEN = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();

    public String getAccessTokenForUser(String user) {
        return tokenCache.computeIfAbsent(user, BearerTokenStepDef::fetchAccessToken);
    }

    private static String fetchAccessToken(String user) {
        return fetchToken(user);
    }

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

    public static String getToken() {
        return ALT_TOKEN.get() != null ? ALT_TOKEN.get() : TOKEN.get();
    }

    @When("I am testing as the {string} user")
    public void setTokenWithUser(String user) {
        ALT_TOKEN.set(getAccessTokenForUser(user));
    }

    @When("I set an invalid token")
    public void setInvalidToken() {
        ALT_TOKEN.set("invalid-token");
    }

    @AfterAll
    public static void clearCache() {
        tokenCache.clear();
        ALT_TOKEN.remove();
        TOKEN.remove();
    }
}
