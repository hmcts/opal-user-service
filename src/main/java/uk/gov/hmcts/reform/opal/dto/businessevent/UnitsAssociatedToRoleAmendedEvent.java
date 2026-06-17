package uk.gov.hmcts.reform.opal.dto.businessevent;

import java.util.Set;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UnitsAssociatedToRoleAmendedEvent(
    Long roleId, Long roleVersion, Set<Short> addedBusinessUnitIds,
    Set<Short> removedBusinessUnitIds) implements BusinessEvent {
}
