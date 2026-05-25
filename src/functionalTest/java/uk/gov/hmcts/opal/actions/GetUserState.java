package uk.gov.hmcts.opal.actions;

import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;

import java.util.Map;

import static uk.gov.hmcts.opal.steps.BearerTokenStepDef.getToken;
import static uk.gov.hmcts.opal.utils.ContentDigestUtils.contentDigestHeaderForEmptyBody;

/**
 * Provides reusable API calls for the current-user state endpoint.
 */
public class GetUserState {
    public static final String USER_STATE_URI = "/users/state";
    private static final String CONTENT_DIGEST = "Content-Digest";

    /**
     * Retrieves the current user's state using the bearer token stored for the scenario.
     *
     * @param baseUri base URL of the service under test.
     */
    public static void getUserState(String baseUri) {
        // Functional API calls include a valid digest for environments that enforce request validation.
        TestHttpResponse response = TestHttpClient.get(
            baseUri + USER_STATE_URI,
            Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer " + getToken(),
                CONTENT_DIGEST, contentDigestHeaderForEmptyBody()
            )
        );

        Serenity.setSessionVariable("LAST_RESPONSE").to(response);
    }
}
