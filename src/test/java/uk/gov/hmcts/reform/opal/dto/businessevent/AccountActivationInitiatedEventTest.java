package uk.gov.hmcts.reform.opal.dto.businessevent;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class AccountActivationInitiatedEventTest {

    @Test
    void getSecurityEventDataReturnsCorrectData() {
        AccountActivationInitiatedEvent event = new AccountActivationInitiatedEvent(OffsetDateTime.now());
        assertThat(event.getSecurityEventData()).isEmpty();
    }
}
