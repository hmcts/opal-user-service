package uk.gov.hmcts.reform.opal.dto.businessevent;

import java.util.Set;

@com.fasterxml.jackson.databind.annotation.JsonNaming(
    com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class
)
@tools.jackson.databind.annotation.JsonNaming(
    tools.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class
)
public record UnitsAssociatedToRoleAmendedEvent(
    Long roleId, Long roleVersion, Set<Short> addedBusinessUnitIds,
    Set<Short> removedBusinessUnitIds) implements BusinessEvent {
}
