package uk.gov.hmcts.reform.opal.authentication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.opal.authentication.exception.AuthenticationError;
import uk.gov.hmcts.reform.opal.exception.OpalApiException;

@Configuration
public class AuthenticationConfiguration {

    @Bean
    public AuthConfigFallback getNoAuthConfigurationFallback(AuthenticationConfigurationPropertiesStrategy strategy) {
        return (req) -> {
            throw new OpalApiException(AuthenticationError.FAILED_TO_OBTAIN_AUTHENTICATION_CONFIG);
        };
    }
}
