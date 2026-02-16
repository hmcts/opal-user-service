package uk.gov.hmcts.opal.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import net.serenitybdd.core.Serenity;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponseCodeStepDef {
    @Then("The response returns the status code {int}")
    public void responseReturnsCode(int statusCode) {
        Response response = Serenity.sessionVariableCalled("LAST_RESPONSE");
        assertThat(response.getStatusCode()).isEqualTo(statusCode);
    }

    @Then("The response contains the following")
    public void responseContains(DataTable responseDetails) {
        Response response = Serenity.sessionVariableCalled("LAST_RESPONSE");
        responseDetails.asMap(String.class, String.class).forEach((key, value) -> {
            String actualValue = response.jsonPath().getString(key);
            assertThat(actualValue).isEqualTo(value);
        });
    }
}

