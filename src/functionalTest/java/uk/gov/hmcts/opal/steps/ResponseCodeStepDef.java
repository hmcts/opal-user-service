package uk.gov.hmcts.opal.steps;

import io.cucumber.java.en.Then;

import static net.serenitybdd.rest.SerenityRest.then;

public class ResponseCodeStepDef {
    @Then("The response returns the status code {int}")
    public void responseReturnsCode(int statusCode) {
        then().assertThat()
            .statusCode(statusCode);
    }
}
