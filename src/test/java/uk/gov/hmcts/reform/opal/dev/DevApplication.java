package uk.gov.hmcts.reform.opal.dev;

import org.springframework.boot.SpringApplication;
import uk.gov.hmcts.reform.opal.Application;

public class DevApplication {

    public static void main(String[] args) {
        SpringApplication.from(Application::main)
            .with(ContainerConfiguration.class)
            .run(args);
    }
}
