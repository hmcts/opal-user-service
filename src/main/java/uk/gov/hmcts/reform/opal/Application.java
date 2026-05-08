package uk.gov.hmcts.reform.opal;

import java.util.Arrays;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
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
    static final String AUTOMATED_TASK_PROPERTY = "opal.automated-task";

    public static void main(final String[] args) {
        if (isAutomatedTask(args)) {
            System.exit(runAutomatedTask(args));
        } else {
            SpringApplication.run(Application.class, args);
        }
    }

    static int runAutomatedTask(final String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
            .web(WebApplicationType.NONE)
            .properties(Map.of(AUTOMATED_TASK_PROPERTY, "true"))
            .run(args);

        return SpringApplication.exit(context);
    }

    static boolean isAutomatedTask(final String[] args) {
        return Arrays.stream(args).anyMatch(AUTOMATED_TASK_ARG::equals);
    }
}
