package uk.gov.hmcts.reform.opal.service.legacy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetBusinessUnitUserIdsResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LegacyBusinessUnitUserService integration tests")
class LegacyBuuServiceIntegrationTest extends AbstractIntegrationTest {

    private final LegacyBusinessUnitUserService legacyBusinessUnitUserService;

    LegacyBuuServiceIntegrationTest(@Autowired LegacyBusinessUnitUserService legacyBusinessUnitUserService) {
        this.legacyBusinessUnitUserService = legacyBusinessUnitUserService;
    }

    @Test
    @DisplayName("Should call legacy gateway using libra user ids request and map business unit user ids response")
    void getBusinessUnitUserIds_callsLegacyGatewayAndMapsBusinessUnitUserIdsResponse() {
        GatewayService.Response<LegacyGetBusinessUnitUserIdsResponse> response =
            legacyBusinessUnitUserService.getBusinessUnitUserIds(List.of("SU001", "SU002"));

        assertThat(response).isNotNull();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.responseEntity).isNotNull();
        assertThat(response.responseEntity.getCount()).isEqualTo(2);
        assertThat(response.responseEntity.getBusinessUnitUserIds()).hasSize(2);
        assertThat(response.responseEntity.getBusinessUnitUserIds().get(0).getBusinessUnitUserId()).isEqualTo("L066JG");
        assertThat(response.responseEntity.getBusinessUnitUserIds().get(0).getBusinessUnitId()).isEqualTo("66");
        assertThat(response.responseEntity.getBusinessUnitUserIds().get(1).getBusinessUnitUserId()).isEqualTo("L067JG");
        assertThat(response.responseEntity.getBusinessUnitUserIds().get(1).getBusinessUnitId()).isEqualTo("67");
    }
}
