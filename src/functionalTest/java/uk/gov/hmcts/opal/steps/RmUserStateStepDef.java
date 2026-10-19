package uk.gov.hmcts.opal.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.core.Serenity;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.actions.GetUserState;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponseDetails;

import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;

/**
 * Defines API functional coverage for Maintenance assignments in V2 User State.
 */
public class RmUserStateStepDef extends BaseStepDef {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TestHttpResponseDetails response;

    /**
     * Selects the scenario's synthetic user and requests their current V2 state.
     *
     * @param userEmail synthetic user's email address.
     */
    @When("I request my V2 User State as the {string} user")
    public void requestUserState(String userEmail) {
        String token = new BearerTokenStepDef().getAccessTokenForUser(userEmail);
        assertThat(token).as("Testing-support token issuance must succeed").isNotBlank();
        recordResponse(GetUserState.getUserStateV2(getTestUrl(), token));
    }

    /**
     * Requests User State with no Authorization header, irrespective of earlier scenarios.
     */
    @When("I request V2 User State without authentication")
    public void requestWithoutAuthentication() {
        recordResponse(GetUserState.getUserStateV2(getTestUrl(), null));
    }

    /**
     * Verifies the authentication error contract without exposing user-state data.
     */
    @Then("the User State response is an authentication problem without assignment data")
    public void authenticationProblem() throws JacksonException {
        assertThat(response).as("An unauthenticated request must precede this assertion").isNotNull();
        String mediaType = response.firstHeader("Content-Type").orElse("")
            .split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        assertThat(mediaType).isEqualTo("application/problem+json");

        JsonNode problem = responseBody();
        assertThat(problem.isObject()).isTrue();
        assertThat(problem.path("status").intValue()).isEqualTo(401);
        assertThat(problem.path("title").asString()).isEqualTo("Unauthorized");
        assertThat(problem.path("detail").asString()).isNotBlank();
        for (String field : List.of("user_id", "username", "name", "domains", "business_unit_users",
            "business_unit_id", "permissions", "permission_id", "permission_name", "trace", "stackTrace",
            "stack_trace")) {
            assertThat(problem.findValues(field)).as("Authentication errors must not expose %s", field).isEmpty();
        }
    }

    /**
     * Verifies that token selection returned state for the expected user.
     *
     * @param userId expected database user identifier.
     */
    @Then("my User State belongs to user {long}")
    public void identifiesUser(long userId) throws JacksonException {
        assertThat(responseBody().path("user_id").longValue()).isEqualTo(userId);
    }

    /**
     * Verifies a permission on one existing Maintenance Business Unit assignment.
     *
     * @param businessUnitId expected Business Unit identifier.
     * @param permissionId expected permission identifier.
     * @param permissionName expected permission display name.
     */
    @Then("my Maintenance Business Unit {int} includes permission {int} named {string}")
    public void includesPermission(int businessUnitId, int permissionId, String permissionName)
        throws JacksonException {

        List<JsonNode> units = maintenanceUnits().stream()
            .filter(unit -> unit.path("business_unit_id").intValue() == businessUnitId)
            .toList();
        assertThat(units).as("Maintenance Business Unit %s must exist exactly once", businessUnitId).hasSize(1);
        assertThat(permissions(units.get(0))).anySatisfy(permission -> {
            assertThat(permission.path("permission_id").intValue()).isEqualTo(permissionId);
            assertThat(permission.path("permission_name").asString()).isEqualTo(permissionName);
        });
    }

    /**
     * Verifies that no Maintenance assignment exposes the excluded permission.
     *
     * @param permissionId excluded permission identifier.
     * @param permissionName excluded permission display name.
     */
    @Then("no Maintenance Business Unit grants permission {int} named {string}")
    public void excludesPermission(int permissionId, String permissionName) throws JacksonException {
        for (JsonNode unit : maintenanceUnits()) {
            for (JsonNode permission : permissions(unit)) {
                assertThat(permission.path("permission_id").intValue()).isNotEqualTo(permissionId);
                assertThat(permission.path("permission_name").asString()).isNotEqualTo(permissionName);
            }
        }
    }

    private void recordResponse(TestHttpResponseDetails latestResponse) {
        response = latestResponse;
        Serenity.setSessionVariable("LAST_RESPONSE").to(latestResponse.toTestHttpResponse());
    }

    private JsonNode responseBody() throws JacksonException {
        assertThat(response).as("A User State request must precede assertions").isNotNull();
        return objectMapper.readTree(response.body());
    }

    private List<JsonNode> maintenanceUnits() throws JacksonException {
        JsonNode units = responseBody().path("domains").path("maintenance").path("business_unit_users");
        assertThat(units.isArray()).as("Maintenance assignments must be returned").isTrue();
        List<JsonNode> assignments = StreamSupport.stream(units.spliterator(), false).toList();
        assertThat(assignments).as("A Maintenance membership is required by this scenario").isNotEmpty();
        return assignments;
    }

    private List<JsonNode> permissions(JsonNode unit) {
        JsonNode values = unit.path("permissions");
        assertThat(values.isArray()).as("A Maintenance assignment must have a permissions array").isTrue();
        return StreamSupport.stream(values.spliterator(), false).toList();
    }
}
