package uk.gov.hmcts.opal.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponseCodeStepDef {
    @Then("The response returns the status code {int}")
    public void responseReturnsCode(int statusCode) {
        TestHttpResponse response = Serenity.sessionVariableCalled("LAST_RESPONSE");
        assertThat(response.statusCode()).isEqualTo(statusCode);
    }

    @Then("The response contains the following")
    public void responseContains(DataTable responseDetails) {
        TestHttpResponse response = Serenity.sessionVariableCalled("LAST_RESPONSE");
        responseDetails.asMap(String.class, String.class).forEach((key, value) -> {
            String actualValue = response.jsonPath(key);
            assertThat(actualValue).isEqualTo(value);
        });
    }
}
