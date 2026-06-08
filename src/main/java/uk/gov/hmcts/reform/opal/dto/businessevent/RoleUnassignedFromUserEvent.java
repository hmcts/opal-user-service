package uk.gov.hmcts.reform.opal.dto.businessevent;

import java.util.Set;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RoleUnassignedFromUserEvent(
    Long roleId, Set<Short> businessUnitIds, Long roleVersion) implements BusinessEvent {
}
