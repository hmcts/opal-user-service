package uk.gov.hmcts.opal.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Defines generic response assertions shared across functional scenarios.
 */
public class ResponseCodeStepDef {
    /**
     * Asserts that the latest recorded response returned the expected HTTP status code.
     *
     * @param statusCode expected HTTP status code.
     */
    @Then("The response returns the status code {int}")
    public void responseReturnsCode(int statusCode) {
        TestHttpResponse response = Serenity.sessionVariableCalled("LAST_RESPONSE");
        assertThat(response.statusCode()).isEqualTo(statusCode);
    }

    /**
     * Asserts that the latest recorded response contains the expected field values.
     *
     * @param responseDetails expected response field/value pairs from the feature file.
     */
    @Then("The response contains the following")
    public void responseContains(DataTable responseDetails) {
        TestHttpResponse response = Serenity.sessionVariableCalled("LAST_RESPONSE");
        responseDetails.asMap(String.class, String.class).forEach((key, value) -> {
            String actualValue = response.jsonPath(key);
            assertThat(actualValue).isEqualTo(value);
        });
    }
}
