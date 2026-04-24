package uk.gov.hmcts.reform.opal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.opal.common.dto.ToJsonString.objectToPrettyJson;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountActivationInitiatedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountSuspensionAttributesAmendedEvent;
import uk.gov.hmcts.reform.opal.entity.BusinessEventEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.repository.BusinessEventRepository;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_user_state_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("BusinessEventServiceInterface database integration tests")
class BusinessEventServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BusinessEventService businessEventService;

    @MockitoBean
    private UserPermissionsService userPermissionsService;

    @Autowired
    private BusinessEventRepository businessEventRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should create and persist a business event when all parameters are provided")
    void logBusinessEvent_persistsExplicitEventDetails() throws JsonProcessingException {
        AccountActivationInitiatedEvent eventDetails = new AccountActivationInitiatedEvent();

        BusinessEventEntity result = businessEventService.logBusinessEvent(
            BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED, 500000000L, 500000003L, eventDetails);

        assertNotNull(result.getBusinessEventId());
        assertEquals(BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED, result.getEventType());
        assertEquals(500000000L, result.getSubjectUserId());
        assertEquals(500000003L, result.getInitiatorUserId());
        assertJsonEquals(objectToPrettyJson(eventDetails), result.getEventDetails());

        BusinessEventEntity savedEntity = businessEventRepository.findById(result.getBusinessEventId()).orElseThrow();
        assertEquals(result.getBusinessEventId(), savedEntity.getBusinessEventId());
        assertEquals(BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED, savedEntity.getEventType());
        assertEquals(500000000L, savedEntity.getSubjectUserId());
        assertEquals(500000003L, savedEntity.getInitiatorUserId());
        assertJsonEquals(objectToPrettyJson(eventDetails), savedEntity.getEventDetails());
    }

    @Test
    @DisplayName("Should create and persist a business event using the authenticated user as initiator")
    void logBusinessEvent_persistsEventDetailsForAuthenticatedUser() throws JsonProcessingException {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("principal", "credentials");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        AccountActivationInitiatedEvent eventDetails = new AccountActivationInitiatedEvent();

        when(userPermissionsService.getAuthenticatedUserId(userPermissionsService)).thenReturn(500000003L);

        BusinessEventEntity result = businessEventService.logBusinessEvent(
            BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED,
            500000000L,
            eventDetails,
            businessEventService
        );

        assertNotNull(result.getBusinessEventId());
        assertEquals(BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED, result.getEventType());
        assertEquals(500000000L, result.getSubjectUserId());
        assertEquals(500000003L, result.getInitiatorUserId());
        assertJsonEquals(objectToPrettyJson(eventDetails), result.getEventDetails());

        BusinessEventEntity savedEntity = businessEventRepository.findById(result.getBusinessEventId()).orElseThrow();
        assertEquals(result.getBusinessEventId(), savedEntity.getBusinessEventId());
        assertEquals(BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED, savedEntity.getEventType());
        assertEquals(500000000L, savedEntity.getSubjectUserId());
        assertEquals(500000003L, savedEntity.getInitiatorUserId());
        assertJsonEquals(objectToPrettyJson(eventDetails), savedEntity.getEventDetails());

        verify(userPermissionsService).getAuthenticatedUserId(userPermissionsService);
    }

    @Test
    @DisplayName("Should reject event details when they do not match the business event type")
    void logBusinessEvent_rejectsMismatchedEventDetailsType() {
        AccountSuspensionAttributesAmendedEvent eventDetails = new AccountSuspensionAttributesAmendedEvent();
        long businessEventCountBeforeCall = businessEventRepository.count();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> businessEventService.logBusinessEvent(
                BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED,
                500000000L,
                500000003L,
                eventDetails
            )
        );

        assertEquals(
            "eventDetails must be of type AccountActivationInitiatedEvent"
                + " for event type ACCOUNT_ACTIVATION_INITIATED",
            exception.getMessage()
        );
        assertEquals(businessEventCountBeforeCall, businessEventRepository.count());
    }

    private void assertJsonEquals(String expectedJson, String actualJson) throws JsonProcessingException {
        JsonNode expected = objectMapper.readTree(expectedJson);
        JsonNode actual = objectMapper.readTree(actualJson);
        assertEquals(expected, actual);
    }
}
