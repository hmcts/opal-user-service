package uk.gov.hmcts.reform.opal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
    "uk.gov.hmcts.reform.opal",
    "uk.gov.hmcts.opal.common"
})
@EnableFeignClients("uk.gov.hmcts.opal.*")
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
