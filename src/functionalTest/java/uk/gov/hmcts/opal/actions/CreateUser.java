package uk.gov.hmcts.opal.actions;

import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;

import java.util.Map;

import static uk.gov.hmcts.opal.steps.BearerTokenStepDef.getToken;
import static uk.gov.hmcts.opal.utils.ContentDigestUtils.contentDigestHeaderForEmptyBody;

/**
 * Provides reusable API calls for creating users in functional tests.
 */
public class CreateUser {
    public static final String USERS_URI = "/users";
    private static final String CONTENT_DIGEST = "Content-Digest";

    /**
     * Creates a user using the bearer token currently stored for the scenario.
     *
     * @param baseURI base URL of the service under test.
     */
    public static void postUser(String baseURI) {
        // Functional API calls include a valid digest for environments that enforce request validation.
        TestHttpResponse response = TestHttpClient.post(
            baseURI + USERS_URI,
            "",
            Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer " + getToken(),
                CONTENT_DIGEST, contentDigestHeaderForEmptyBody()
            )
        );

        Serenity.setSessionVariable("LAST_RESPONSE").to(response);
    }
}
