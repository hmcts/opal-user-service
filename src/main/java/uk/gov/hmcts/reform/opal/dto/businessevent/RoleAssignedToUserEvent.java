package uk.gov.hmcts.reform.opal.dto.businessevent;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.Set;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RoleAssignedToUserEvent(Long roleId, Long roleVersion,
                                      Set<Short> addedBusinessUnitIds) implements BusinessEvent {
}
