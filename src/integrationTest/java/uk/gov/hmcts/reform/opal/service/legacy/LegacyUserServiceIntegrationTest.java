package uk.gov.hmcts.reform.opal.service.legacy;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.LegacyStubContainerConfig;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetUserResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("LegacyUserService integration tests")
class LegacyUserServiceIntegrationTest extends AbstractIntegrationTest {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final LegacyUserService legacyUserService;

    LegacyUserServiceIntegrationTest(@Autowired LegacyUserService legacyUserService) {
        this.legacyUserService = legacyUserService;
    }

    @DynamicPropertySource
    static void registerLegacyGatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("legacy-gateway.url", LegacyStubContainerConfig::legacyGatewayUrl);
    }

    @Test
    @DisplayName("Should call legacy gateway using email request and map Libra system user ids response")
    void getUser_callsLegacyGatewayAndMapsLibraUserIdsResponse() throws Exception {
        GatewayService.Response<LegacyGetUserResponse> response =
            legacyUserService.getUser("legacy.user@hmcts.net");

        assertThat(response).isNotNull();
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.responseEntity).isNotNull();
        assertThat(response.responseEntity.getCount()).isEqualTo(2);
        assertThat(response.responseEntity.getLibraUserIds()).containsExactly("SU001", "SU002");

        HttpRequest requestJournalRequest = HttpRequest.newBuilder()
            .uri(URI.create(LegacyStubContainerConfig.legacyAdminUrl() + "/requests"))
            .GET()
            .build();

        HttpResponse<String> requestJournalResponse =
            HTTP_CLIENT.send(requestJournalRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(requestJournalResponse.statusCode()).isEqualTo(200);

        JsonNode requestJournal = objectMapper.readTree(requestJournalResponse.body());
        assertThat(requestJournal.get("requests")).hasSize(1);

        JsonNode loggedRequest = requestJournal.get("requests").get(0).get("request");
        assertThat(loggedRequest.get("url").asText()).isEqualTo("/opal?actionType=GetSystemUserIdsByEmail");
        assertThat(loggedRequest.get("headers").get("Authorization").asText()).startsWith("Basic ");

        JsonNode requestJson = objectMapper.readTree(loggedRequest.get("body").asText());
        assertThat(requestJson.get("email_address").asText()).isEqualTo("legacy.user@hmcts.net");
    }
}
