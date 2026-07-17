package uk.gov.hmcts.opal.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponseDetails;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.opal.utils.ContentDigestUtils.contentDigestHeaderFor;
import static uk.gov.hmcts.opal.utils.ContentDigestUtils.contentDigestHeaderForEmptyBody;
import static uk.gov.hmcts.opal.utils.ContentDigestUtils.invalidContentDigestHeader;
import static uk.gov.hmcts.opal.utils.ContentDigestUtils.malformedContentDigestHeader;

public class ContentDigestStepDef extends BaseStepDef {

    private static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_DIGEST = "Content-Digest";
    private static final String LAST_CONTENT_DIGEST_RESPONSE = "LAST_CONTENT_DIGEST_RESPONSE";
    private static final String POST_ENDPOINT = "/testing-support/post-user";
    private static final String POST_BODY = "{}";

    @When("I make a content digest request without a Content-Digest header")
    public void getRootWithoutContentDigestHeader() {
        getRoot(Map.of("Accept", "*/*"));
    }

    @When("I make a content digest request with a valid Content-Digest header")
    public void getRootWithValidContentDigestHeader() {
        getRoot(Map.of("Accept", "*/*", CONTENT_DIGEST, contentDigestHeaderForEmptyBody()));
    }

    @When("I make a content digest request with an invalid Content-Digest header")
    public void getRootWithInvalidContentDigestHeader() {
        post(Map.of("Accept", "*/*",
            CONTENT_DIGEST, invalidContentDigestHeader(),
            "Content-Type", APPLICATION_JSON));
    }

    @When("I make a content digest request with a malformed Content-Digest header")
    public void getRootWithMalformedContentDigestHeader() {
        post(Map.of("Accept", "*/*",
            CONTENT_DIGEST, malformedContentDigestHeader(),
            "Content-Type", APPLICATION_JSON));
    }

    @Then("The response has a valid Content-Digest header")
    public void responseHasValidContentDigestHeader() {
        TestHttpResponseDetails response = Serenity.sessionVariableCalled(LAST_CONTENT_DIGEST_RESPONSE);

        assertThat(response.firstHeader(CONTENT_DIGEST))
            .hasValue(contentDigestHeaderFor(response.bodyBytes()));
    }

    private static void getRoot(Map<String, String> headers) {
        TestHttpResponseDetails response = TestHttpClient.getWithResponseDetails(getTestUrl() + "/", headers);
        Serenity.setSessionVariable("LAST_RESPONSE").to(response.toTestHttpResponse());
        Serenity.setSessionVariable(LAST_CONTENT_DIGEST_RESPONSE).to(response);
    }

    private static void post(Map<String, String> headers) {
        TestHttpResponseDetails response = TestHttpClient.postWithResponseDetails(
            getTestUrl() + POST_ENDPOINT, POST_BODY, headers);
        Serenity.setSessionVariable("LAST_RESPONSE").to(response.toTestHttpResponse());
        Serenity.setSessionVariable(LAST_CONTENT_DIGEST_RESPONSE).to(response);
    }
}
