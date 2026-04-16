package uk.gov.hmcts.reform.opal.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountActivationInitiatedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountDeactivationDateAmendedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountSuspensionAttributesAmendedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.BusinessEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.FunctionsAssociatedToRoleAmendedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleAssignedToUserEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleUnassignedFromUserEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.UnitsAssociatedToRoleAmendedEvent;

@Getter
@RequiredArgsConstructor
public enum BusinessEventLogType {
    ACCOUNT_ACTIVATION_INITIATED(AccountActivationInitiatedEvent.class),
    ACCOUNT_SUSPENSION_ATTRIBUTES_AMENDED(AccountSuspensionAttributesAmendedEvent.class),
    ACCOUNT_DEACTIVATION_DATE_AMENDED(AccountDeactivationDateAmendedEvent.class),
    ROLE_ASSIGNED_TO_USER(RoleAssignedToUserEvent.class),
    BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED(UnitsAssociatedToRoleAmendedEvent.class),
    ROLE_UNASSIGNED_FROM_USER(RoleUnassignedFromUserEvent.class),
    FUNCTIONS_ASSOCIATED_TO_ROLE_AMENDED(FunctionsAssociatedToRoleAmendedEvent.class);

    private final Class<? extends BusinessEvent> eventDtoClass;

    public void validateEventDetails(BusinessEvent eventDetails) {
        if (!eventDtoClass.isInstance(eventDetails)) {
            throw new IllegalArgumentException(
                "eventDetails must be of type " + eventDtoClass.getSimpleName() + " for event type " + name());
        }
    }
}
