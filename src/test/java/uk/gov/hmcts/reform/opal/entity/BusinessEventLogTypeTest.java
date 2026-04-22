package uk.gov.hmcts.reform.opal.entity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountActivationInitiatedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountDeactivationDateAmendedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountSuspensionAttributesAmendedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.BusinessEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.FunctionsAssociatedToRoleAmendedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleAssignedToUserEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleUnassignedFromUserEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.UnitsAssociatedToRoleAmendedEvent;

class BusinessEventLogTypeTest {

    @ParameterizedTest
    @MethodSource("matchingEvents")
    void validateEventDetails_acceptsMatchingEventType(BusinessEventLogType businessEventLogType,
        BusinessEvent eventDetails) {
        assertDoesNotThrow(() -> businessEventLogType.validateEventDetails(eventDetails));
    }

    @ParameterizedTest
    @MethodSource("mismatchedEvents")
    void validateEventDetails_throwsWhenEventTypeDoesNotMatch(BusinessEventLogType businessEventLogType,
        BusinessEvent wrongEvent) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> businessEventLogType.validateEventDetails(wrongEvent));

        assertEquals(
            "eventDetails must be of type " + businessEventLogType.getEventDtoClass().getSimpleName()
                + " for event type " + businessEventLogType.name(),
            exception.getMessage());
    }

    private static Stream<Arguments> matchingEvents() {
        return Stream.of(
            Arguments.of(BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED, new AccountActivationInitiatedEvent()),
            Arguments.of(
                BusinessEventLogType.ACCOUNT_SUSPENSION_ATTRIBUTES_AMENDED,
                new AccountSuspensionAttributesAmendedEvent()
            ),
            Arguments.of(
                BusinessEventLogType.ACCOUNT_DEACTIVATION_DATE_AMENDED,
                new AccountDeactivationDateAmendedEvent()
            ),
            Arguments.of(
                BusinessEventLogType.ROLE_ASSIGNED_TO_USER,
                new RoleAssignedToUserEvent(101L, Set.of((short) 11))
            ),
            Arguments.of(
                BusinessEventLogType.BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED,
                new UnitsAssociatedToRoleAmendedEvent(101L, Set.of((short) 11), Set.of((short) 12))
            ),
            Arguments.of(
                BusinessEventLogType.ROLE_UNASSIGNED_FROM_USER,
                new RoleUnassignedFromUserEvent(101L, Set.of((short) 11, (short) 12), 4L)
            ),
            Arguments.of(
                BusinessEventLogType.FUNCTIONS_ASSOCIATED_TO_ROLE_AMENDED,
                new FunctionsAssociatedToRoleAmendedEvent()
            )
        );
    }

    private static Stream<Arguments> mismatchedEvents() {
        AccountActivationInitiatedEvent activationEvent = new AccountActivationInitiatedEvent();

        return matchingEvents()
            .map(Arguments::get)
            .map(arguments -> Arguments.of(
                arguments[0],
                arguments[0] == BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED
                    ? new RoleUnassignedFromUserEvent(101L, Set.of((short) 11), 4L)
                    : activationEvent
            ));
    }
}
