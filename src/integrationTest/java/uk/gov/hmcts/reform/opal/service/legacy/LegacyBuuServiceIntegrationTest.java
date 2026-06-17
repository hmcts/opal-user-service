package uk.gov.hmcts.reform.opal.service.legacy;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetBusinessUnitUserIdsResponse;
import java.util.List;
import static org.assertj.core.api.Assertions.tuple;


import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
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

        log.info(response.toString());

        assertThat(response).isNotNull();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.responseEntity).isNotNull();
        assertThat(response.responseEntity.getCount()).isEqualTo(29);
        assertThat(response.responseEntity.getBusinessUnitUserIds()).hasSize(29);
        assertThat(response.responseEntity.getBusinessUnitUserIds())
            .hasSize(29)
            .extracting(
                LegacyBusinessUnitUserId::getBusinessUnitUserId,
                LegacyBusinessUnitUserId::getBusinessUnitId)
            .containsExactly(
                tuple("L013JF", "13"),
                tuple("L016JF", "16"),
                tuple("L017JF", "17"),
                tuple("L019JF", "19"),
                tuple("L025JF", "25"),
                tuple("L032JF", "32"),
                tuple("L040JF", "40"),
                tuple("L062JF", "62"),
                tuple("L063JF", "63"),
                tuple("L064JF", "64"),
                tuple("L065JF", "65"),
                tuple("L066JF", "66"),
                tuple("L067JF", "67"),
                tuple("L068JF", "68"),
                tuple("L069JF", "69"),
                tuple("L070JF", "70"),
                tuple("L071JF", "71"),
                tuple("L072JF", "72"),
                tuple("L073JF", "73"),
                tuple("L074JF", "74"),
                tuple("L075JF", "75"),
                tuple("L076JF", "76"),
                tuple("L077JF", "77"),
                tuple("L078JF", "78"),
                tuple("L079JF", "79"),
                tuple("L080JF", "80"),
                tuple("L081JF", "81"),
                tuple("L035SA", "35"),
                tuple("L036SA", "36")
            );
    }
}
