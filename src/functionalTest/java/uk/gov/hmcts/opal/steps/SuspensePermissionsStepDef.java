package uk.gov.hmcts.opal.steps;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.core.Serenity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.actions.GetUserState;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Defines API steps for Suspense permissions in V2 user state.
 */
public class SuspensePermissionsStepDef extends BaseStepDef {

    /**
     * Calls the V2 endpoint for the authenticated user.
     */
    @When("I fetch the current V2 user state")
    public void fetchCurrentUserStateV2() {
        GetUserState.getUserStateV2(getTestUrl());
    }

    /**
     * Checks that a permission ID and description occur together in the specified business unit.
     *
     * @param permissionId expected permission ID.
     * @param description expected permission description.
     * @param businessUnitId business unit containing the permission.
     */
    @Then("The user state contains permission {int} named {string} in business unit {int}")
    public void userStateContainsPermission(int permissionId, String description, int businessUnitId) {
        TestHttpResponse response = Serenity.sessionVariableCalled("LAST_RESPONSE");
        JsonNode businessUnits = new ObjectMapper().readTree(response.body())
            .at("/domains/fines/business_unit_users");

        assertThat(businessUnits.isArray()).as("Fines business unit users").isTrue();
        List<JsonNode> units = StreamSupport.stream(businessUnits.spliterator(), false).toList();

        assertThat(units)
            .as("Business unit %s in user state", businessUnitId)
            .filteredOn(unit -> unit.path("business_unit_id").asInt() == businessUnitId)
            .singleElement()
            .satisfies(unit -> {
                JsonNode permissions = unit.path("permissions");
                assertThat(permissions.isArray()).as("Business unit permissions").isTrue();
                assertThat(StreamSupport.stream(permissions.spliterator(), false).toList())
                    .as("Permission %s (%s) in business unit %s", permissionId, description, businessUnitId)
                    .anySatisfy(permission -> {
                        assertThat(permission.path("permission_id").asLong()).isEqualTo(permissionId);
                        assertThat(permission.path("permission_name").asString()).isEqualTo(description);
                    });
            });
    }
}
