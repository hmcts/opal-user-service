package uk.gov.hmcts.reform.opal.dto.businessevent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.Map;
import uk.gov.hmcts.opal.common.dto.ToJsonString;

public sealed interface BusinessEvent extends ToJsonString
    permits AccountActivationInitiatedEvent,
    RoleAssignedToUserEvent,
    BusinessUnitsAssociatedToRoleAmendedEvent,
    RoleUnassignedFromUserEvent,
    FunctionsAssociatedToRoleAmendedEvent {

    @JsonIgnore
    default Map<String, Object> getSecurityEventData() {
        return Collections.emptyMap();
    }
}
