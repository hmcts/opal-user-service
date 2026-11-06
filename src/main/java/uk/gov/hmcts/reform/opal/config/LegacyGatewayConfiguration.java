package uk.gov.hmcts.reform.opal.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.opal.common.legacy.client.LegacyGatewayClient;
import uk.gov.hmcts.opal.common.legacy.config.LegacyGatewayProperties;
import uk.gov.hmcts.opal.common.legacy.service.GatewayService;
import uk.gov.hmcts.opal.common.legacy.service.LegacyGatewayService;

@Configuration
@EnableConfigurationProperties(LegacyGatewayProperties.class)
public class LegacyGatewayConfiguration {

    @Bean
    public GatewayService gatewayService(LegacyGatewayProperties legacyGatewayProperties,
                                         LegacyGatewayClient legacyGatewayClient) {
        return new LegacyGatewayService(legacyGatewayProperties, legacyGatewayClient);
    }
}
