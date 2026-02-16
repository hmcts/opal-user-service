package uk.gov.hmcts.opal.steps;

import io.cucumber.java.en.When;
import uk.gov.hmcts.opal.actions.CreateUser;

public class UsersStepDef extends BaseStepDef {

    @When("I create a user using the stored token")
    public void createUser() {
        CreateUser.postUser(getTestUrl());
    }
}
