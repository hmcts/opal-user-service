package uk.gov.hmcts.reform.opal.service;

import uk.gov.hmcts.reform.opal.dto.businessevent.BusinessEvent;
import uk.gov.hmcts.reform.opal.entity.BusinessEventEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;

public interface BusinessEventServiceProxy {
    <T extends BusinessEvent> BusinessEventEntity logBusinessEvent(
        BusinessEventLogType businessEventLogType, Long subjectUserId, Long initiatorUserId, T eventDetails);
}
