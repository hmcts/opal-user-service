package uk.gov.hmcts.opal.actions;

import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;

import java.util.Map;

import static uk.gov.hmcts.opal.steps.BearerTokenStepDef.getToken;

public class CreateUser {
    public static final String USERS_URI = "/users";

    public static TestHttpResponse postUser(String baseURI) {
        TestHttpResponse response = TestHttpClient.post(
            baseURI + USERS_URI,
            "",
            Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer " + getToken()
            )
        );

        Serenity.setSessionVariable("LAST_RESPONSE").to(response);
        return response;
    }
}
