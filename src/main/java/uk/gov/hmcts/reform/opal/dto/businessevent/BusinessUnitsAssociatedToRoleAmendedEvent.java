package uk.gov.hmcts.reform.opal.dto.businessevent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BusinessUnitsAssociatedToRoleAmendedEvent(
    Long roleId, Long roleVersion, Set<Short> addedBusinessUnitIds,
    Set<Short> removedBusinessUnitIds) implements BusinessEvent {

    @Override
    public Map<String, Object> getSecurityEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("roleId", roleId);
        data.put("roleVersion", roleVersion);
        data.put("addedBusinessUnitIds", addedBusinessUnitIds);
        data.put("removedBusinessUnitIds", removedBusinessUnitIds);
        return data;
    }
}
