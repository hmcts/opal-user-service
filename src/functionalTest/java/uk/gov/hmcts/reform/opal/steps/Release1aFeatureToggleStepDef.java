package uk.gov.hmcts.reform.opal.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.reform.opal.actions.Release1aFeatureToggleActions;
import uk.gov.hmcts.reform.opal.utils.TestHttpClient.TestHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Defines Cucumber steps for release-1a feature-toggle coverage in the user service.
 */
public class Release1aFeatureToggleStepDef extends BaseStepDef {

    private static final String LAST_RELEASE_1A_ACCESS_TOKEN = "LAST_RELEASE_1A_ACCESS_TOKEN";
    private static final String REPRESENTATIVE_AUTHORIZATION = "release-1a-representative-token";
    private static final String ACTIVATION_DATE = "2026-01-01T00:00:00Z";

    /**
     * Calls the named release-1a gated endpoint using a representative request for that route.
     *
     * @param endpoint user-facing endpoint name from the feature file.
     */
    @When("I call the release-1a gated {string} endpoint")
    public void callRelease1aGatedEndpoint(String endpoint) {
        switch (endpoint) {
            case "get test user token" -> Release1aFeatureToggleActions.requestTestUserToken(getTestUrl());
            case "get user token" -> Release1aFeatureToggleActions.requestTokenForUser(
                getTestUrl(), "opal-test@dev.platform.hmcts.net"
            );
            case "parse token" -> Release1aFeatureToggleActions.parseToken(getTestUrl(), REPRESENTATIVE_AUTHORIZATION);
            case "add user" -> Release1aFeatureToggleActions.addUser(getTestUrl(), REPRESENTATIVE_AUTHORIZATION);
            case "update current user" -> Release1aFeatureToggleActions.updateCurrentUser(
                getTestUrl(), REPRESENTATIVE_AUTHORIZATION, "0"
            );
            case "update user by id" -> Release1aFeatureToggleActions.updateUserById(
                getTestUrl(), 500000002L, REPRESENTATIVE_AUTHORIZATION, "0"
            );
            case "get current user state" -> Release1aFeatureToggleActions.getCurrentUserState(getTestUrl());
            case "get user state by id" -> Release1aFeatureToggleActions.getUserStateById(getTestUrl(), 500000003L);
            case "add or replace role information on user" -> Release1aFeatureToggleActions
                .addOrReplaceRoleInformationOnUser(getTestUrl(), 987L, 101L);
            case "activate user" -> Release1aFeatureToggleActions.activateUser(getTestUrl(), 987L, ACTIVATION_DATE);
            default -> throw new IllegalArgumentException("Unsupported release-1a endpoint: " + endpoint);
        }
    }

    /**
     * Requests a release-1a access token for the supplied user and remembers it for later steps.
     *
     * @param userEmail email address to request a token for.
     */
    @When("I request the release-1a token for the {string} user")
    public void requestRelease1aTokenForUser(String userEmail) {
        rememberAccessToken(Release1aFeatureToggleActions.requestTokenForUser(getTestUrl(), userEmail));
    }

    /**
     * Parses the access token most recently returned by the release-1a token endpoint.
     */
    @When("I parse the last returned release-1a access token")
    public void parseLastReturnedRelease1aAccessToken() {
        String accessToken = Serenity.sessionVariableCalled(LAST_RELEASE_1A_ACCESS_TOKEN);
        assertThat(accessToken).isNotBlank();
        Release1aFeatureToggleActions.parseToken(getTestUrl(), "Bearer " + accessToken);
    }

    /**
     * Asserts that the latest response contains the named JSON field with a non-blank value.
     *
     * @param fieldName JSON field name to assert.
     */
    @Then("The response contains a non-empty {string} field")
    public void responseContainsNonEmptyField(String fieldName) {
        TestHttpResponse response = Serenity.sessionVariableCalled("LAST_RESPONSE");
        assertThat(response.jsonPath(fieldName)).isNotBlank();
    }

    /**
     * Asserts that the latest response body matches the supplied plain-text value exactly.
     *
     * @param expectedValue expected response body.
     */
    @Then("The response body is {string}")
    public void responseBodyIs(String expectedValue) {
        TestHttpResponse response = Serenity.sessionVariableCalled("LAST_RESPONSE");
        assertThat(response.body()).isEqualTo(expectedValue);
    }

    /**
     * Stores the `access_token` field from the supplied response for later parsing steps.
     *
     * @param response response whose token should be remembered.
     */
    private void rememberAccessToken(TestHttpResponse response) {
        Serenity.setSessionVariable(LAST_RELEASE_1A_ACCESS_TOKEN).to(response.jsonPath("access_token"));
    }
}
