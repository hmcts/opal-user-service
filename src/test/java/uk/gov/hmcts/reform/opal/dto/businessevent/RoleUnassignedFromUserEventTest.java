package uk.gov.hmcts.reform.opal.dto.businessevent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class RoleUnassignedFromUserEventTest {

    @Test
    void getSecurityEventDataReturnsCorrectData() {
        Set<Short> businessUnits = Set.of((short) 1, (short) 2, (short) 3);
        Long roleId = 123L;
        Long roleVersion = 321L;

        RoleUnassignedFromUserEvent event = new RoleUnassignedFromUserEvent(
            roleId, roleVersion, businessUnits);

        assertThat(event.getSecurityEventData())
            .containsEntry("roleId", roleId)
            .containsEntry("roleVersion", roleVersion)
            .containsEntry("businessUnitIds", businessUnits)
            .hasSize(3);
    }
}
