package uk.gov.hmcts.reform.opal.dto.businessevent;

import java.util.HashMap;
import java.util.Map;

public record FunctionsAssociatedToRoleAmendedEvent(Long roleId, Long roleVersion) implements BusinessEvent {
    @Override
    public Map<String, Object> getSecurityEventData() {
        Map<String, Object> data = new HashMap<>();
        data.put("roleId", roleId);
        data.put("roleVersion", roleVersion);
        return data;
    }
}
