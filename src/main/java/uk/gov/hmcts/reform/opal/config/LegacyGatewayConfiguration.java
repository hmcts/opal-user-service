package uk.gov.hmcts.reform.opal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import uk.gov.hmcts.opal.common.legacy.config.LegacyGatewayProperties;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.opal.common.legacy.service.LegacyGatewayService;

@Configuration
@EnableConfigurationProperties(LegacyGatewayProperties.class)
public class LegacyGatewayConfiguration {

    @Bean
    public GatewayService gatewayService(LegacyGatewayProperties legacyGatewayProperties, RestClient restClient) {
        return new LegacyGatewayService(legacyGatewayProperties, restClient);
    }
}
