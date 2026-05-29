package uk.gov.hmcts.reform.opal.config.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleMappingRefreshService;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "opal.automated-task", havingValue = "true")
@Slf4j
class AutomatedUserRoleMappingRefreshRunner implements ApplicationRunner {

    private final UserRoleMappingRefreshService service;

    AutomatedUserRoleMappingRefreshRunner(UserRoleMappingRefreshService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.info("Starting automated role mapping refresher");
        service.refreshMappings();
        log.info("Completed automated role mapping refresher");
    }
}
