package uk.gov.hmcts.reform.opal.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;
import uk.gov.hmcts.reform.opal.config.LegacyModeConfiguration;
import uk.gov.hmcts.reform.opal.dto.businessevent.BusinessEvent;
import uk.gov.hmcts.reform.opal.entity.BusinessEventEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.repository.BusinessEventRepository;

import java.time.Clock;
import java.time.LocalDateTime;

import static uk.gov.hmcts.opal.common.dto.ToJsonString.objectToJson;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.BusinessEventServiceInterface")
public class BusinessEventService implements BusinessEventServiceInterface, BusinessEventServiceProxy {

    // OPAL system user
    private static final long SYSTEM_USER_ID = -1L;
    private final BusinessEventRepository businessEventRepository;
    private final UserPermissionsService userPermissionsService;
    private final LegacyModeConfiguration legacyModeConfiguration;
    private final Clock clock;

    @Transactional
    @Override
    public <T extends BusinessEvent> BusinessEventEntity logBusinessEvent(
        BusinessEventLogType businessEventLogType, Long subjectUserId, T eventDetails,
        BusinessEventServiceProxy businessEventServiceProxy) {

        return businessEventServiceProxy.logBusinessEvent(
            businessEventLogType, subjectUserId,
            resolveInitiatorUserId(), eventDetails
        );
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
            .eventDate(LocalDateTime.now(clock))
            .build();

        BusinessEventEntity savedBusinessEvent = businessEventRepository.saveAndFlush(businessEventEntity);
        log.debug("Logged business event {} for subject user {} by initiator user {}",
            savedBusinessEvent.getEventType(),
            savedBusinessEvent.getSubjectUserId(),
            savedBusinessEvent.getInitiatorUserId());

        return savedBusinessEvent;

    }

    private Long resolveInitiatorUserId() {
        try {
            if (legacyModeConfiguration.isLegacyMode()) {
                return SYSTEM_USER_ID;
            } else {
                return userPermissionsService.getAuthenticatedUserId();
            }
        } catch (Exception ex) {
            throw new InternalServerErrorException("Failed to resolve initiator user ID for business event logging",
                "No authenticated user found or failed to determine LEGACY mode", ex);
        }
    }
}
