package uk.gov.hmcts.reform.opal.controllers;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

class SampleFunctionalTest {

    private static final String TEST_URL = System.getenv().getOrDefault("TEST_URL", "http://localhost:4555");

    protected static String getTestUrl() {
        return TEST_URL;
    }

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = getTestUrl();
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void functionalTest() {
        Response response = given()
            .contentType(ContentType.JSON)
            .when()
            .get("/health")
            .then()
            .extract().response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertTrue(response.asString().contains("\"db\":{\"status\":\"UP\""));
    }
}
