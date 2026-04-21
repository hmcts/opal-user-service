package uk.gov.hmcts.reform.opal.config.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
@Validated
public class CacheConfiguration {
    @NotNull
    private Long userStateTimeoutMinutes;
}
