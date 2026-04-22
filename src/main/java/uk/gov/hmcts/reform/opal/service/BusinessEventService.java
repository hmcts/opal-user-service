package uk.gov.hmcts.reform.opal.service;

import static uk.gov.hmcts.opal.common.dto.ToJsonString.objectToJson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.opal.dto.businessevent.BusinessEvent;
import uk.gov.hmcts.reform.opal.entity.BusinessEventEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.repository.BusinessEventRepository;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.BusinessEventServiceInterface")
public class BusinessEventService implements BusinessEventServiceInterface, BusinessEventServiceProxy {

    private final BusinessEventRepository businessEventRepository;
    private final UserPermissionsService userPermissionsService;

    @Transactional
    @Override
    public <T extends BusinessEvent> BusinessEventEntity logBusinessEvent(
        BusinessEventLogType businessEventLogType, Long subjectUserId, T eventDetails,
        BusinessEventServiceProxy businessEventServiceProxy) {

        return businessEventServiceProxy.logBusinessEvent(businessEventLogType, subjectUserId,
            userPermissionsService.getAuthenticatedUserId(userPermissionsService), eventDetails);
    }

    @Transactional
    @Override
    public <T extends BusinessEvent> BusinessEventEntity logBusinessEvent(
        BusinessEventLogType businessEventLogType, Long subjectUserId, Long initiatorUserId, T eventDetails) {

        businessEventLogType.validateEventDetails(eventDetails);

        BusinessEventEntity businessEventEntity = BusinessEventEntity.builder()
            .eventType(businessEventLogType)
            .subjectUserId(subjectUserId)
            .initiatorUserId(initiatorUserId)
            .eventDetails(objectToJson(eventDetails))
            .build();

        BusinessEventEntity savedBusinessEvent = businessEventRepository.saveAndFlush(businessEventEntity);
        log.debug("Logged business event {} for subject user {} by initiator user {}",
                 savedBusinessEvent.getEventType(),
                 savedBusinessEvent.getSubjectUserId(),
                 savedBusinessEvent.getInitiatorUserId());
        return savedBusinessEvent;
    }
}
