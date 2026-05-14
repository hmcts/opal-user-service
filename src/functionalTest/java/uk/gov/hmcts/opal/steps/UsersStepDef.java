package uk.gov.hmcts.opal.steps;

import io.cucumber.java.en.When;
import uk.gov.hmcts.opal.actions.CreateUser;
import uk.gov.hmcts.opal.actions.GetUserState;

/**
 * Defines user-service steps used by the core functional scenarios.
 */
public class UsersStepDef extends BaseStepDef {

    /**
     * Calls the create-user endpoint using the token currently stored for the scenario.
     */
    @When("I create a user using the stored token")
    public void createUser() {
        CreateUser.postUser(getTestUrl());
    }

    /**
     * Calls the current-user state endpoint using the token currently stored for the scenario.
     */
    @When("I fetch the current user state")
    public void fetchCurrentUserState() {
        GetUserState.getUserState(getTestUrl());
    }
}
