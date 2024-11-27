package uk.gov.hmcts.opal.steps;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.cucumber.java.en.Then;
import net.serenitybdd.core.Serenity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.opal.utils.TokenUtils;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.opal.steps.BearerTokenStepDef.getToken;

public class AssertTokenStepDef {
    private static final Logger log = LoggerFactory.getLogger(AssertTokenStepDef.class);

    @Then("I validate the unique name claim matches {string}")
    public void validateTheUniqueNameClaimMatches(String testUser) {
        DecodedJWT decodedToken = TokenUtils.parseToken(getToken());

        assertEquals(testUser, decodedToken.getClaim("unique_name").asString());
        Serenity.recordReportData()
            .withTitle("Token unique name")
            .andContents(decodedToken.getClaim("unique_name").asString());

        log.info("Token unique name is: {}", decodedToken.getClaim("unique_name").asString());
    }
}
