package uk.gov.hmcts.opal.steps;

import io.cucumber.java.en.When;
import uk.gov.hmcts.opal.actions.CreateUser;
import uk.gov.hmcts.opal.actions.GetUserState;

public class UsersStepDef extends BaseStepDef {

    @When("I create a user using the stored token")
    public void createUser() {
        CreateUser.postUser(getTestUrl());
    }

    @When("I fetch the current user state")
    public void fetchCurrentUserState() {
        GetUserState.getUserState(getTestUrl());
    }
}
