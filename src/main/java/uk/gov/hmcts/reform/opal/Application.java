package uk.gov.hmcts.reform.opal;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = {
    "uk.gov.hmcts.reform.opal",
    "uk.gov.hmcts.opal.common"
})
@EnableFeignClients(basePackages = "uk.gov.hmcts.opal")
@SuppressWarnings("HideUtilityClassConstructor")
public class Application {

    static final String AUTOMATED_TASK_ARG =
        "AutomatedTask:UserRoleMappingFileRefresh";

    public static void main(final String[] args) {
        System.exit(start(args));
    }

    static int start(final String[] args) {
        boolean automatedTask = isAutomatedTask(args);

        SpringApplication app = new SpringApplication(Application.class);

        if (automatedTask) {
            app.setWebApplicationType(WebApplicationType.NONE);
        }

        ConfigurableApplicationContext context = app.run(args);

        if (automatedTask) {
            return SpringApplication.exit(context);
        }

        return 0;
    }

    static boolean isAutomatedTask(final String[] args) {
        return Arrays.asList(args).contains(AUTOMATED_TASK_ARG);
    }
}
