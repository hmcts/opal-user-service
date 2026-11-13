package uk.gov.hmcts.reform.opal.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.opal.common.legacy.client.LegacyGatewayClient;
import uk.gov.hmcts.opal.common.legacy.config.LegacyGatewayProperties;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyGetUserRequest;

class LegacyGatewayConfigurationTest {

    @Test
    void gatewayService_usesFeignClientForLegacyUserLookup() {
        LegacyGatewayProperties properties = new LegacyGatewayProperties();
        properties.setUsername("user");
        properties.setPassword("password");
        LegacyGatewayClient client = mock(LegacyGatewayClient.class);
        LegacyGetUserRequest request = LegacyGetUserRequest.builder().emailAddress("user@example.test").build();
        String action = "getLibraSystemUserIDs";
        String authorization = "Basic dXNlcjpwYXNzd29yZA==";
        when(client.post(action, authorization, request)).thenReturn(ResponseEntity.ok("legacy response"));

        GatewayService gatewayService = new LegacyGatewayConfiguration().gatewayService(properties, client);
        GatewayService.Response<String> response = gatewayService.postToGateway(action, String.class, request, null);

        assertThat(response.code).isEqualTo(HttpStatus.OK);
        assertThat(response.body).isEqualTo("legacy response");
    }
}
