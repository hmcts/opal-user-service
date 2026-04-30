package uk.gov.hmcts.reform.opal.service.rolemapping;

import java.time.Duration;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cache.role-mapping")
public class RoleMappingCacheProperties {

    private Duration userTtl = Duration.ofHours(24);
    private Duration lastUpdateTtl = Duration.ofHours(1);

    @PostConstruct
    void validate() {
        if (userTtl == null || userTtl.isZero() || userTtl.isNegative()) {
            throw new IllegalStateException("userTtl must be greater than zero");
        }

        if (lastUpdateTtl == null || lastUpdateTtl.isZero() || lastUpdateTtl.isNegative()) {
            throw new IllegalStateException("lastUpdateTtl must be greater than zero");
        }

        if (lastUpdateTtl.compareTo(userTtl) >= 0) {
            throw new IllegalStateException("lastUpdateTtl must be less than userTtl");
        }
    }
}
