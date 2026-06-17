package uk.gov.hmcts.reform.opal.service.legacy;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetUserResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("LegacyUserService integration tests")
class LegacyUserServiceIntegrationTest extends AbstractIntegrationTest {

    // TODO PO-2941/PO-3442: Refactor this into a higher-level MockMvc integration test once the
    // legacy user lookup is exercised through a real controller or user-state flow. Fines usually
    // integration-tests legacy calls through controller paths rather than directly at thin wrapper level.
    private final LegacyUserService legacyUserService;

    LegacyUserServiceIntegrationTest(@Autowired LegacyUserService legacyUserService) {
        this.legacyUserService = legacyUserService;
    }

    @Test
    @DisplayName("Should call legacy gateway using email request and map Libra system user ids response")
    void getUser_callsLegacyGatewayAndMapsLibraUserIdsResponse() {
        GatewayService.Response<LegacyGetUserResponse> response =
            legacyUserService.getUser("legacy.user@hmcts.net");

        log.info("legacyUserService.getUser() response: {}", response);

        assertThat(response).isNotNull();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.responseEntity).isNotNull();
        assertThat(response.responseEntity.getLibraUserIds())
            .containsExactly("869", "632", "609", "388", "1607", "1568", "1648", "1627", "1628", "1758");
    }
}
