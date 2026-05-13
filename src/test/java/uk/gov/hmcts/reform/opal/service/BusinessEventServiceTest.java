package uk.gov.hmcts.reform.opal.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountDeactivationDateAmendedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleAssignedToUserEvent;
import uk.gov.hmcts.reform.opal.entity.BusinessEventEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.repository.BusinessEventRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessEventServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-13T09:30:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime EVENT_DATE = LocalDateTime.ofInstant(CLOCK.instant(), ZoneOffset.UTC);

    @Mock
    private BusinessEventRepository businessEventRepository;

    @Mock
    private UserPermissionsService userPermissionsService;

    @Mock
    private FeatureToggleApi featureToggleApi;

    private BusinessEventService businessEventService;

    @BeforeEach
    void setUp() {
        businessEventService = new BusinessEventService(
            businessEventRepository,
            userPermissionsService,
            featureToggleApi,
            CLOCK
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    public void setupFeatureFlags(boolean isLegacyMode) {
        when(featureToggleApi.isFeatureEnabledWithPropertyValueDefault(
            "is-legacy-mode",
            "opal.feature-flags.is-legacy-mode",
            false)).thenReturn(isLegacyMode);
    }

    @Test
    void logBusinessEvent_usesAuthenticatedUserAsInitiator() {
        RoleAssignedToUserEvent eventDetails = new RoleAssignedToUserEvent(201L, 1L, Set.of((short) 11));
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(10L).build();

        when(userPermissionsService.getAuthenticatedUserId()).thenReturn(99L);
        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        final BusinessEventEntity result = businessEventService.logBusinessEvent(
            BusinessEventLogType.ROLE_ASSIGNED_TO_USER, 42L, eventDetails, businessEventService);

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
        assertEventDate(capturedEntity);
        assertSame(savedEntity, result);
    }

    @Test
    void logBusinessEvent_throwsWhenEventDetailsTypeDoesNotMatchEventType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> businessEventService.logBusinessEvent(
                BusinessEventLogType.ACCOUNT_DEACTIVATION_DATE_AMENDED, 42L, 88L,
                new RoleAssignedToUserEvent(201L, 1L, Set.of((short) 11))));

        assertEquals(
            "eventDetails must be of type AccountDeactivationDateAmendedEvent"
                + " for event type ACCOUNT_DEACTIVATION_DATE_AMENDED",
            exception.getMessage());
    }

    @Test
    void logBusinessEvent_usesSystemUserWhenAppModeIsLegacy() {
        setupFeatureFlags(true);
        RoleAssignedToUserEvent eventDetails = new RoleAssignedToUserEvent(201L, 1L, Set.of((short) 11));
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(13L).build();

        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        businessEventService.logBusinessEvent(
            BusinessEventLogType.ROLE_ASSIGNED_TO_USER, 42L, eventDetails, businessEventService);

        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());

        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        assertEquals(-1L, capturedEntity.getInitiatorUserId());
        assertEventDate(capturedEntity);

        verifyNoInteractions(userPermissionsService);
    }

    @Test
    void logBusinessEvent_usesLoggedInUserWhenAppModeIsNotLegacy() {
        setupFeatureFlags(false);
        RoleAssignedToUserEvent eventDetails = new RoleAssignedToUserEvent(201L, 1L, Set.of((short) 11));
        BusinessEventEntity savedEntity = BusinessEventEntity.builder().businessEventId(13L).build();

        when(userPermissionsService.getAuthenticatedUserId()).thenReturn(99L);
        when(businessEventRepository.saveAndFlush(any(BusinessEventEntity.class))).thenReturn(savedEntity);

        businessEventService.logBusinessEvent(
            BusinessEventLogType.ROLE_ASSIGNED_TO_USER, 42L, eventDetails, businessEventService);

        ArgumentCaptor<BusinessEventEntity> entityCaptor = ArgumentCaptor.forClass(BusinessEventEntity.class);
        verify(businessEventRepository).saveAndFlush(entityCaptor.capture());

        BusinessEventEntity capturedEntity = entityCaptor.getValue();
        assertEquals(99L, capturedEntity.getInitiatorUserId());
        assertEventDate(capturedEntity);
        verify(userPermissionsService).getAuthenticatedUserId();
    }

    private void assertEventDate(BusinessEventEntity capturedEntity) {
        assertEquals(EVENT_DATE, capturedEntity.getEventDate());
    }
}
