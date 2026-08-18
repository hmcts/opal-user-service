package uk.gov.hmcts.reform.opal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.opal.common.logging.SecurityEventLoggingService;
import uk.gov.hmcts.reform.opal.config.LegacyModeConfiguration;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountActivationInitiatedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleAssignedToUserEvent;
import uk.gov.hmcts.reform.opal.entity.BusinessEventEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.repository.BusinessEventRepository;

@ExtendWith(MockitoExtension.class)
class BusinessEventServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-13T09:30:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime EVENT_DATE = LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);

    @Mock
    private BusinessEventRepository businessEventRepository;

    @Mock
    private UserPermissionsService userPermissionsService;

    @Mock
    private SecurityEventLoggingService securityEventLoggingService;

    @Mock
    private LegacyModeConfiguration legacyModeConfiguration;

    private BusinessEventService businessEventService;

    @BeforeEach
    void setUp() {
        businessEventService = new BusinessEventService(
            businessEventRepository,
            userPermissionsService,
            legacyModeConfiguration,
            CLOCK,
            securityEventLoggingService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupLegacyMode(boolean isLegacyMode) {
        when(legacyModeConfiguration.isLegacyMode()).thenReturn(isLegacyMode);
    }

    @Test
    void logBusinessEvent_usesAuthenticatedUserAsInitiator() {
        RoleAssignedToUserEvent eventDetails = new RoleAssignedToUserEvent(201L, 1L, Set.of((short) 11));
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(10L).build();

        when(userPermissionsService.getAuthenticatedUserId()).thenReturn(99L);
        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        final BusinessEventEntity result = businessEventService.logBusinessEvent(
            BusinessEventLogType.ROLE_ASSIGNED_TO_USER, 42L, eventDetails);

        verify(userPermissionsService).getAuthenticatedUserId();
        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());

        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        assertEquals(BusinessEventLogType.ROLE_ASSIGNED_TO_USER, capturedEntity.getEventType());
        assertEquals(42L, capturedEntity.getSubjectUserId());
        assertEquals(99L, capturedEntity.getInitiatorUserId());
        assertEquals("{\"role_id\":201,\"role_version\":1,\"added_business_unit_ids\":[11]}",
            capturedEntity.getEventDetails());
        assertEventDate(capturedEntity);
        assertSame(savedEntity, result);
    }

    @Test
    void logBusinessEvent_usesExplicitInitiatorUserIdWhenProvided() {
        OffsetDateTime time = OffsetDateTime.parse("2026-05-13T09:30:00Z");
        AccountActivationInitiatedEvent eventDetails = new AccountActivationInitiatedEvent(time);
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(11L).build();

        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        final BusinessEventEntity result = businessEventService.logBusinessEvent(
            BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED, 42L, 88L, eventDetails);

        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());
        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        assertEquals(BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED, capturedEntity.getEventType());
        assertEquals(42L, capturedEntity.getSubjectUserId());
        assertEquals(88L, capturedEntity.getInitiatorUserId());
        assertEquals("{\"account_activation_date\":\"2026-05-13T09:30:00Z\"}", capturedEntity.getEventDetails());
        assertEventDate(capturedEntity);
        assertSame(savedEntity, result);
    }

    @Test
    void logBusinessEvent_throwsWhenEventDetailsTypeDoesNotMatchEventType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> businessEventService.logBusinessEvent(
                BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED, 42L, 88L,
                new RoleAssignedToUserEvent(201L, 1L, Set.of((short) 11))));

        assertEquals(
            "eventDetails must be of type AccountActivationInitiatedEvent"
                + " for event type ACCOUNT_ACTIVATION_INITIATED",
            exception.getMessage());
    }

    @Test
    void logBusinessEvent_usesSystemUserWhenLegacyMode() {
        setupLegacyMode(true);
        RoleAssignedToUserEvent eventDetails = new RoleAssignedToUserEvent(201L, 1L, Set.of((short) 11));
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(13L).build();

        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        businessEventService.logBusinessEvent(
            BusinessEventLogType.ROLE_ASSIGNED_TO_USER, 42L, eventDetails);

        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());

        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        assertEquals(-1L, capturedEntity.getInitiatorUserId());
        assertEventDate(capturedEntity);

        verifyNoInteractions(userPermissionsService);
    }

    @Test
    void logBusinessEvent_usesLoggedInUserWhenNotLegacyMode() {
        setupLegacyMode(false);
        RoleAssignedToUserEvent eventDetails = new RoleAssignedToUserEvent(201L, 1L, Set.of((short) 11));
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(13L).build();

        when(userPermissionsService.getAuthenticatedUserId()).thenReturn(99L);
        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        businessEventService.logBusinessEvent(
            BusinessEventLogType.ROLE_ASSIGNED_TO_USER, 42L, eventDetails);

        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());

        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        assertEquals(99L, capturedEntity.getInitiatorUserId());
        assertEventDate(capturedEntity);
        verify(userPermissionsService).getAuthenticatedUserId();
    }

    @Test
    void logBusinessEvent_shouldCallLogEventWithCorrectData() {
        RoleAssignedToUserEvent eventDetails = new RoleAssignedToUserEvent(201L, 1L, Set.of((short) 11));
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(13L).build();

        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        businessEventService.logBusinessEvent(
            BusinessEventLogType.ROLE_ASSIGNED_TO_USER, 42L,47L, eventDetails);

        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());

        Map<String, Object> expectedEventData = new HashMap<>();
        expectedEventData.put("roleId", 201L);
        expectedEventData.put("roleVersion", 1L);
        expectedEventData.put("addedBusinessUnitIds", Set.of((short) 11));
        expectedEventData.put("subjectUserId", 42L);
        expectedEventData.put("initiatorUserId", 47L);

        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        verify(securityEventLoggingService).logEvent(
            "User Management Function - Role assigned to user",
            "Success",
            null,
            "Permission Change",
            capturedEntity.getEventDate(),
            expectedEventData
        );
    }

    private void assertEventDate(BusinessEventEntity capturedEntity) {
        assertEquals(EVENT_DATE, capturedEntity.getEventDate());
    }
}
