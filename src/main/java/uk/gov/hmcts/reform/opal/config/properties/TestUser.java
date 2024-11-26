package uk.gov.hmcts.reform.opal.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "opal.test-user")
public class TestUser {

    private String email;
    private String password;
}
