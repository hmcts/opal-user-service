package uk.gov.hmcts.reform.opal.dto.businessevent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class FunctionsAssociatedToRoleAmendedEventTest {
    @Test
    void getSecurityEventDataReturnsCorrectData() {
        Long roleId = 123L;
        Long roleVersion = 321L;

        FunctionsAssociatedToRoleAmendedEvent event = new FunctionsAssociatedToRoleAmendedEvent(
            roleId, roleVersion);

        assertThat(event.getSecurityEventData())
            .containsEntry("roleId", roleId)
            .containsEntry("roleVersion", roleVersion)
            .hasSize(2);
    }
}
