package uk.gov.hmcts.reform.opal.dto.businessevent;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Set;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record UnitsAssociatedToRoleAmendedEvent(
    Long roleId, Set<Short> addedBusinessUnitIds, Set<Short> removedBusinessUnitIds) implements BusinessEvent {
}
