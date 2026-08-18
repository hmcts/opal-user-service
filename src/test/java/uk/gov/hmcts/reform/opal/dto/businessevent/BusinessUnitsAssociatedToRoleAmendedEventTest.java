package uk.gov.hmcts.reform.opal.dto.businessevent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class BusinessUnitsAssociatedToRoleAmendedEventTest {

    @Test
    void getSecurityEventDataReturnsCorrectData() {
        Set<Short> addedBusinessUnits = Set.of((short) 1, (short) 2, (short) 3);
        Set<Short> removedBusinessUnits = Set.of((short) 4, (short) 5, (short) 6);
        Long roleId = 123L;
        Long roleVersion = 321L;

        BusinessUnitsAssociatedToRoleAmendedEvent event = new BusinessUnitsAssociatedToRoleAmendedEvent(
            roleId, roleVersion, addedBusinessUnits, removedBusinessUnits);

        assertThat(event.getSecurityEventData())
            .containsEntry("roleId", roleId)
            .containsEntry("roleVersion", roleVersion)
            .containsEntry("addedBusinessUnitIds", addedBusinessUnits)
            .containsEntry("removedBusinessUnitIds", removedBusinessUnits)
            .hasSize(4);
    }
}
