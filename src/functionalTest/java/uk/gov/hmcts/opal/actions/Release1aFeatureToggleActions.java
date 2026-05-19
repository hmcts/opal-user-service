package uk.gov.hmcts.opal.actions;

import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;

import java.util.Map;

/**
 * Provides reusable HTTP calls for the release-1a feature-toggle scenarios.
 */
public final class Release1aFeatureToggleActions {

    private static final String JSON_CONTENT_TYPE = "application/json";

    /**
     * Utility class.
     */
    private Release1aFeatureToggleActions() {
    }

    /**
     * Requests a token for the configured default test user.
     *
     * @param baseUri base URL of the service under test.
     */
    public static void requestTestUserToken(String baseUri) {
        record(TestHttpClient.get(
            baseUri + "/testing-support/token/test-user",
            Map.of(
                "Accept", "*/*",
                "Content-Type", JSON_CONTENT_TYPE
            )
        ));
    }

    /**
     * Requests a token for the supplied user email.
     *
     * @param baseUri base URL of the service under test.
     * @param userEmail email address to request a token for.
     * @return response returned by the user-token endpoint.
     */
    public static TestHttpResponse requestTokenForUser(String baseUri, String userEmail) {
        return record(TestHttpClient.get(
            baseUri + "/testing-support/token/user",
            Map.of(
                "Accept", "*/*",
                "Content-Type", JSON_CONTENT_TYPE,
                "X-User-Email", userEmail
            )
        ));
    }

    /**
     * Parses the supplied bearer token through the testing-support endpoint.
     *
     * @param baseUri base URL of the service under test.
     * @param authorization authorization header value to parse.
     */
    public static void parseToken(String baseUri, String authorization) {
        record(TestHttpClient.get(
            baseUri + "/testing-support/token/parse",
            Map.of(
                "Accept", "*/*",
                "Content-Type", JSON_CONTENT_TYPE,
                "Authorization", authorization
            )
        ));
    }

    /**
     * Calls the add-user endpoint using a representative bearer token.
     *
     * @param baseUri base URL of the service under test.
     * @param authorization authorization header value to send.
     */
    public static void addUser(String baseUri, String authorization) {
        record(TestHttpClient.post(
            baseUri + "/users",
            "",
            Map.of(
                "Content-Type", JSON_CONTENT_TYPE,
                "Authorization", authorization
            )
        ));
    }

    /**
     * Calls the update-current-user endpoint using a representative bearer token and ETag.
     *
     * @param baseUri base URL of the service under test.
     * @param authorization authorization header value to send.
     * @param ifMatch ETag value to send in the `If-Match` header.
     */
    public static void updateCurrentUser(String baseUri, String authorization, String ifMatch) {
        record(TestHttpClient.put(
            baseUri + "/users",
            "",
            Map.of(
                "Content-Type", JSON_CONTENT_TYPE,
                "Authorization", authorization,
                "If-Match", ifMatch
            )
        ));
    }

    /**
     * Calls the update-user-by-id endpoint using a representative bearer token and ETag.
     *
     * @param baseUri base URL of the service under test.
     * @param userId user identifier to update.
     * @param authorization authorization header value to send.
     * @param ifMatch ETag value to send in the `If-Match` header.
     */
    public static void updateUserById(String baseUri, long userId, String authorization, String ifMatch) {
        record(TestHttpClient.put(
            baseUri + "/users/" + userId,
            "",
            Map.of(
                "Content-Type", JSON_CONTENT_TYPE,
                "Authorization", authorization,
                "If-Match", ifMatch
            )
        ));
    }

    /**
     * Calls the current-user state endpoint.
     *
     * @param baseUri base URL of the service under test.
     */
    public static void getCurrentUserState(String baseUri) {
        record(TestHttpClient.get(
            baseUri + "/users/state",
            Map.of("Content-Type", JSON_CONTENT_TYPE)
        ));
    }

    /**
     * Calls the user-state-by-id endpoint.
     *
     * @param baseUri base URL of the service under test.
     * @param userId user identifier whose state should be requested.
     */
    public static void getUserStateById(String baseUri, long userId) {
        record(TestHttpClient.get(
            baseUri + "/users/" + userId + "/state",
            Map.of("Content-Type", JSON_CONTENT_TYPE)
        ));
    }

    /**
     * Calls the testing-support endpoint that replaces a user's role information.
     *
     * @param baseUri base URL of the service under test.
     * @param userId user identifier whose roles should be updated.
     * @param roleId role identifier to include in the path.
     */
    public static void addOrReplaceRoleInformationOnUser(String baseUri, long userId, long roleId) {
        record(TestHttpClient.put(
            baseUri + "/testing-support/users/" + userId + "/roles/" + roleId,
            "[1,4,5]",
            Map.of("Content-Type", JSON_CONTENT_TYPE)
        ));
    }

    /**
     * Calls the testing-support endpoint that activates a user.
     *
     * @param baseUri base URL of the service under test.
     * @param userId user identifier to activate.
     * @param activationDate activation date to send in the patch request body.
     */
    public static void activateUser(String baseUri, long userId, String activationDate) {
        record(TestHttpClient.patch(
            baseUri + "/testing-support/users/" + userId,
            """
                {
                  "activationDate": "%s"
                }
                """.formatted(activationDate),
            Map.of("Content-Type", JSON_CONTENT_TYPE)
        ));
    }

    /**
     * Stores the supplied response as the latest scenario response and returns it unchanged.
     *
     * @param response response to record in Serenity session state.
     * @return the same response instance.
     */
    private static TestHttpResponse record(TestHttpResponse response) {
        Serenity.setSessionVariable("LAST_RESPONSE").to(response);
        return response;
    }
}
