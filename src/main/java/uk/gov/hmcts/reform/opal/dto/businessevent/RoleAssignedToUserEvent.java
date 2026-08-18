package uk.gov.hmcts.reform.opal.dto.businessevent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RoleAssignedToUserEvent(Long roleId, Long roleVersion,
                                      Set<Short> addedBusinessUnitIds) implements BusinessEvent {

    @Override
    public Map<String, Object> getSecurityEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("roleId", roleId);
        data.put("roleVersion", roleVersion);
        data.put("addedBusinessUnitIds", addedBusinessUnitIds);
        return data;
    }
}
