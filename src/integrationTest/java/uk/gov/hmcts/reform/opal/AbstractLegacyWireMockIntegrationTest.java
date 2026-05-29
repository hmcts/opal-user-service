package uk.gov.hmcts.reform.opal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class AbstractLegacyWireMockIntegrationTest extends AbstractIntegrationTest {

    protected LegacyWireMockXmlStubHelper legacyWireMockXmlStubHelper;

    @BeforeEach
    protected void initialiseLegacyGatewayWireMock() throws Exception {
        legacyWireMockXmlStubHelper = LegacyWireMockXmlStubHelper.initialise(objectMapper);
    }

    @AfterEach
    protected void clearLegacyGatewayWireMockState() throws Exception {
        SecurityContextHolder.clearContext();
        if (legacyWireMockXmlStubHelper != null) {
            legacyWireMockXmlStubHelper.clearRegisteredStubs();
        }
    }
}
