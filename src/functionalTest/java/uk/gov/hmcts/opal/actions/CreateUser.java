package uk.gov.hmcts.opal.actions;

import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;

import java.util.Map;

import static uk.gov.hmcts.opal.steps.BearerTokenStepDef.getToken;

/**
 * Provides reusable API calls for creating users in functional tests.
 */
public class CreateUser {
    public static final String USERS_URI = "/users";

    /**
     * Creates a user using the bearer token currently stored for the scenario.
     *
     * @param baseURI base URL of the service under test.
     */
    public static void postUser(String baseURI) {
        TestHttpResponse response = TestHttpClient.post(
            baseURI + USERS_URI,
            "",
            Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer " + getToken()
            )
        );

        Serenity.setSessionVariable("LAST_RESPONSE").to(response);
    }
}
