package uk.gov.hmcts.reform.opal.dto.businessevent;

import uk.gov.hmcts.opal.common.dto.ToJsonString;

public sealed interface BusinessEvent extends ToJsonString
    permits AccountActivationInitiatedEvent,
            AccountSuspensionAttributesAmendedEvent,
            AccountDeactivationDateAmendedEvent,
            RoleAssignedToUserEvent,
            UnitsAssociatedToRoleAmendedEvent,
            RoleUnassignedFromUserEvent,
            FunctionsAssociatedToRoleAmendedEvent {
}
