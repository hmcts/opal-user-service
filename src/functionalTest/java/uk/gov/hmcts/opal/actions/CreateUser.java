package uk.gov.hmcts.opal.actions;

import io.restassured.response.Response;
import net.serenitybdd.core.Serenity;

import static net.serenitybdd.rest.SerenityRest.given;
import static uk.gov.hmcts.opal.steps.BearerTokenStepDef.getToken;

public class CreateUser {
    public static final String USERS_URI = "/users";

    public static Response postUser(String baseURI) {
        Response response =
            given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + getToken())
                .when()
                .post(baseURI + USERS_URI).then().extract().response();

        Serenity.setSessionVariable("LAST_RESPONSE").to(response);
        return response;
    }
}
