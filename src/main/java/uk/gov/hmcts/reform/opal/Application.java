package uk.gov.hmcts.reform.opal;

import java.util.Arrays;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@ComponentScan(basePackages = {"uk.gov.hmcts.reform.opal", "uk.gov.hmcts.opal.common"},
    excludeFilters =
    @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = RestController.class))
@EnableFeignClients(basePackages = "uk.gov.hmcts.opal")
@SuppressWarnings("HideUtilityClassConstructor")
public class Application {

    static final String AUTOMATED_TASK_ARG = "AutomatedTask:UserRoleMappingFileRefresh";

    public static void main(final String[] args) {
        System.exit(start(args));
    }

    static int start(final String[] args) {
        boolean automatedTask = isAutomatedTask(args);

        SpringApplication app = new SpringApplication(Application.class);

        if (automatedTask) {
            app.setDefaultProperties(Map.of("opal.automated-task", "true"));
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
