package uk.gov.hmcts.opal.steps;

import io.cucumber.java.en.When;
import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;

import java.util.Map;

import static uk.gov.hmcts.opal.utils.ContentDigestUtils.contentDigestHeaderForEmptyBody;
import static uk.gov.hmcts.opal.utils.ContentDigestUtils.invalidContentDigestHeader;
import static uk.gov.hmcts.opal.utils.ContentDigestUtils.malformedContentDigestHeader;

public class ContentDigestStepDef extends BaseStepDef {

    @When("I make a content digest request without a Content-Digest header")
    public void getRootWithoutContentDigestHeader() {
        getRoot(Map.of("Accept", "*/*"));
    }

    @When("I make a content digest request with a valid Content-Digest header")
    public void getRootWithValidContentDigestHeader() {
        getRoot(Map.of("Accept", "*/*", "Content-Digest", contentDigestHeaderForEmptyBody()));
    }

    @When("I make a content digest request with an invalid Content-Digest header")
    public void getRootWithInvalidContentDigestHeader() {
        getRoot(Map.of("Accept", "*/*", "Content-Digest", invalidContentDigestHeader()));
    }

    @When("I make a content digest request with a malformed Content-Digest header")
    public void getRootWithMalformedContentDigestHeader() {
        getRoot(Map.of("Accept", "*/*", "Content-Digest", malformedContentDigestHeader()));
    }

    private static void getRoot(Map<String, String> headers) {
        TestHttpResponse response = TestHttpClient.get(getTestUrl() + "/", headers);
        Serenity.setSessionVariable("LAST_RESPONSE").to(response);
    }
}
