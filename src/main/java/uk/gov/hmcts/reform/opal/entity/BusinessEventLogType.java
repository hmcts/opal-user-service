package uk.gov.hmcts.reform.opal.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.opal.dto.businessevent.AccountActivationInitiatedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.BusinessEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.FunctionsAssociatedToRoleAmendedEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleAssignedToUserEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.RoleUnassignedFromUserEvent;
import uk.gov.hmcts.reform.opal.dto.businessevent.BusinessUnitsAssociatedToRoleAmendedEvent;

@Getter
@RequiredArgsConstructor
public enum BusinessEventLogType {
    ACCOUNT_ACTIVATION_INITIATED(
        AccountActivationInitiatedEvent.class,
        "User Management Function - Account Activation",
        "Account Activation",
        "Success"),
    ROLE_ASSIGNED_TO_USER(RoleAssignedToUserEvent.class,
        "User Management Function - Role assigned to user",
        "Permission Change",
        "Success"),
    BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED(BusinessUnitsAssociatedToRoleAmendedEvent.class,
        "User Management Function - Business units associated to role amended",
        "Permission Change",
        "Success"),
    ROLE_UNASSIGNED_FROM_USER(RoleUnassignedFromUserEvent.class,
        "User Management Function - Role unassigned from user",
        "Permission Change",
        "Success"),
    FUNCTIONS_ASSOCIATED_TO_ROLE_AMENDED(FunctionsAssociatedToRoleAmendedEvent.class,
        "User Management Function - Functions associated to role amended",
        "Permission Change",
        "Success");

    private final Class<? extends BusinessEvent> eventDtoClass;
    private final String securityEventName;
    private final String securityEventOperationType;
    private final String securityEventOutcome;

    public void validateEventDetails(BusinessEvent eventDetails) {
        if (!eventDtoClass.isInstance(eventDetails)) {
            throw new IllegalArgumentException(
                "eventDetails must be of type " + eventDtoClass.getSimpleName() + " for event type " + name());
        }
    }
}
