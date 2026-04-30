package uk.gov.hmcts.reform.opal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountDeactivationDateAmendedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleAssignedToUserEvent;
import uk.gov.hmcts.reform.opal.entity.BusinessEventEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.repository.BusinessEventRepository;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class BusinessEventServiceTest {

    @Mock
    private BusinessEventRepository businessEventRepository;

    @Mock
    private UserPermissionsService userPermissionsService;

    @InjectMocks
    private BusinessEventService businessEventService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logBusinessEvent_usesAuthenticatedUserAsInitiator() {
        RoleAssignedToUserEvent eventDetails = new RoleAssignedToUserEvent(201L, Set.of((short) 11));
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(10L).build();

        when(userPermissionsService.getAuthenticatedUserId(userPermissionsService)).thenReturn(99L);
        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        final BusinessEventEntity result = businessEventService.logBusinessEvent(
            BusinessEventLogType.ROLE_ASSIGNED_TO_USER, 42L, eventDetails, businessEventService);

        verify(userPermissionsService).getAuthenticatedUserId(userPermissionsService);
        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());

        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        assertEquals(BusinessEventLogType.ROLE_ASSIGNED_TO_USER, capturedEntity.getEventType());
        assertEquals(42L, capturedEntity.getSubjectUserId());
        assertEquals(99L, capturedEntity.getInitiatorUserId());
        assertEquals("{\"role_id\":201,\"added_business_unit_ids\":[11]}", capturedEntity.getEventDetails());
        assertSame(savedEntity, result);
    }

    @Test
    void logBusinessEvent_usesExplicitInitiatorUserIdWhenProvided() {
        AccountDeactivationDateAmendedEvent eventDetails = new AccountDeactivationDateAmendedEvent();
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(11L).build();

        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        final BusinessEventEntity result = businessEventService.logBusinessEvent(
            BusinessEventLogType.ACCOUNT_DEACTIVATION_DATE_AMENDED, 42L, 88L, eventDetails);

        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());
        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        assertEquals(BusinessEventLogType.ACCOUNT_DEACTIVATION_DATE_AMENDED, capturedEntity.getEventType());
        assertEquals(42L, capturedEntity.getSubjectUserId());
        assertEquals(88L, capturedEntity.getInitiatorUserId());
        assertEquals("{}", capturedEntity.getEventDetails());
        assertSame(savedEntity, result);
    }

    @Test
    void logBusinessEvent_throwsWhenEventDetailsTypeDoesNotMatchEventType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> businessEventService.logBusinessEvent(
                BusinessEventLogType.ACCOUNT_DEACTIVATION_DATE_AMENDED, 42L, 88L,
                new RoleAssignedToUserEvent(201L, Set.of((short) 11))));

        assertEquals(
            "eventDetails must be of type AccountDeactivationDateAmendedEvent"
                + " for event type ACCOUNT_DEACTIVATION_DATE_AMENDED",
            exception.getMessage());
    }

    @Test
    void logBusinessEvent_usesSystemUserWhenNoAuthenticatedUser() {
        RoleAssignedToUserEvent eventDetails = new RoleAssignedToUserEvent(201L, Set.of((short) 11));
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(12L).build();

        when(userPermissionsService.getAuthenticatedUserId(userPermissionsService)).thenReturn(null);
        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        businessEventService.logBusinessEvent(
            BusinessEventLogType.ROLE_ASSIGNED_TO_USER, 42L, eventDetails, businessEventService);

        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());

        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        assertEquals(-1L, capturedEntity.getInitiatorUserId());
    }

    @Test
    void logBusinessEvent_usesSystemUserWhenAuthenticationThrows() {
        RoleAssignedToUserEvent eventDetails = new RoleAssignedToUserEvent(201L, Set.of((short) 11));
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(13L).build();

        when(userPermissionsService.getAuthenticatedUserId(userPermissionsService))
            .thenThrow(new RuntimeException("No auth context"));
        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        businessEventService.logBusinessEvent(
            BusinessEventLogType.ROLE_ASSIGNED_TO_USER, 42L, eventDetails, businessEventService);

        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());

        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        assertEquals(-1L, capturedEntity.getInitiatorUserId());
    }

    @Test
    void logBusinessEvent_defaultsToSystemUserWhenExplicitInitiatorIsNull() {
        AccountDeactivationDateAmendedEvent eventDetails = new AccountDeactivationDateAmendedEvent();
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(14L).build();

        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        businessEventService.logBusinessEvent(
            BusinessEventLogType.ACCOUNT_DEACTIVATION_DATE_AMENDED,
            42L,
            null,
            eventDetails);

        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());

        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        assertEquals(-1L, capturedEntity.getInitiatorUserId());
    }
}
