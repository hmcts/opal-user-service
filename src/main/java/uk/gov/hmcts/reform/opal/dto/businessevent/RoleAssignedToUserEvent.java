package uk.gov.hmcts.reform.opal.dto.businessevent;

import java.util.Set;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RoleAssignedToUserEvent(Long roleId, Long roleVersion,
                                      Set<Short> addedBusinessUnitIds) implements BusinessEvent {
}
