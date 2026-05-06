package uk.gov.hmcts.reform.opal.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@ConditionalOnProperty(
    name = "opal.automated-task",
    havingValue = "false",
    matchIfMissing = true
)
@ComponentScan(
    basePackages = "uk.gov.hmcts.reform.opal",
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = RestController.class
    ),
    useDefaultFilters = false
)
// config class which turns controllers on or off depending on automated task running
public class ControllerConfig {
}
