package uk.gov.hmcts.reform.opal.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
