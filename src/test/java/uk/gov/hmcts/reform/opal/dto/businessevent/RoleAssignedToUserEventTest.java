package uk.gov.hmcts.reform.opal.dto.businessevent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleAssignedToUserEventTest {

    @Test
    void getSecurityEventDataReturnsCorrectData() {
        Set<Short> addedBusinessUnits = Set.of((short) 1, (short) 2, (short) 3);
        Long roleId = 123L;
        Long roleVersion = 321L;

        RoleAssignedToUserEvent event = new RoleAssignedToUserEvent(
            roleId, roleVersion, addedBusinessUnits);

        assertThat(event.getSecurityEventData())
            .containsEntry("roleId", roleId)
            .containsEntry("roleVersion", roleVersion)
            .containsEntry("addedBusinessUnitIds", addedBusinessUnits)
            .hasSize(3);
    }
}
