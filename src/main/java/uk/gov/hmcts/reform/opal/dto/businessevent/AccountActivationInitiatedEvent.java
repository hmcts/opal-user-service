package uk.gov.hmcts.reform.opal.dto.businessevent;

import java.time.OffsetDateTime;

public record AccountActivationInitiatedEvent(OffsetDateTime accountActivationDate) implements BusinessEvent {

}

