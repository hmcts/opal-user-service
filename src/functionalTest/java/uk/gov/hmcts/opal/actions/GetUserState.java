package uk.gov.hmcts.opal.actions;

import static uk.gov.hmcts.opal.steps.BearerTokenStepDef.getToken;
import static uk.gov.hmcts.opal.utils.ContentDigestUtils.contentDigestHeaderForEmptyBody;

import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponseDetails;

import java.util.Map;

/**
 * Provides reusable API calls for the current-user state endpoint.
 */
public class GetUserState {
    public static final String USER_STATE_URI = "/users/state";
    private static final String USER_STATE_V2_URI = "/v2/users/0/state";
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

    /**
     * Retrieves V2 state using the supplied bearer token without recording a new login.
     *
     * @param baseUri base URL of the service under test.
     * @param token bearer token for the selected synthetic user.
     * @return response status, body bytes and headers.
     */
    public static TestHttpResponseDetails getUserStateV2(String baseUri, String token) {
        return TestHttpClient.getWithResponseDetails(baseUri + USER_STATE_V2_URI, Map.of(
            "Accept", "application/json",
            "Authorization", "Bearer " + token,
            "X-New-Login", "false",
            CONTENT_DIGEST, contentDigestHeaderForEmptyBody()));
    }
}
