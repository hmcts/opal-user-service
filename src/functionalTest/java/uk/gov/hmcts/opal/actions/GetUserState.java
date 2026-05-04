package uk.gov.hmcts.opal.actions;

import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.utils.TestHttpClient;
import uk.gov.hmcts.opal.utils.TestHttpClient.TestHttpResponse;

import java.util.Map;

import static uk.gov.hmcts.opal.steps.BearerTokenStepDef.getToken;

public class GetUserState {
    public static final String USER_STATE_URI = "/users/state";

    public static TestHttpResponse getUserState(String baseUri) {
        TestHttpResponse response = TestHttpClient.get(
            baseUri + USER_STATE_URI,
            Map.of(
                "Content-Type", "application/json",
                "Authorization", "Bearer " + getToken()
            )
        );

        Serenity.setSessionVariable("LAST_RESPONSE").to(response);
        return response;
    }
}
