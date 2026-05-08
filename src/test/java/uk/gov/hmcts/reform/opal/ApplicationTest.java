package uk.gov.hmcts.reform.opal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class ApplicationTest {

    @Test
    void isAutomatedTaskShouldReturnTrueWhenArgumentPresent() {
        assertThat(Application.isAutomatedTask(
            new String[] {"AutomatedTask:UserRoleMappingFileRefresh"}
        )).isTrue();
    }

    @Test
    void isAutomatedTaskShouldReturnFalseWhenArgumentMissing() {
        assertThat(Application.isAutomatedTask(new String[] {"other-arg"})).isFalse();
        assertThat(Application.isAutomatedTask(new String[0])).isFalse();
    }

    @Test
    void mainShouldRunNormallyWhenAutomatedTaskArgIsMissing() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        String[] args = new String[0];

        try (MockedStatic<SpringApplication> springApplicationStatic = mockStatic(SpringApplication.class)) {
            springApplicationStatic.when(() -> SpringApplication.run(Application.class, args)).thenReturn(context);

            Application.main(args);

            springApplicationStatic.verify(() -> SpringApplication.run(Application.class, args));
        }
    }

    @Test
    void runAutomatedTaskShouldDisableWebLayerSetTaskPropertyAndExit() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        String[] args = new String[] {"AutomatedTask:UserRoleMappingFileRefresh"};

        try (MockedConstruction<SpringApplicationBuilder> construction =
                 mockConstruction(SpringApplicationBuilder.class, (mock, mockContext) -> {
                     when(mock.web(WebApplicationType.NONE)).thenReturn(mock);
                     when(mock.properties(anyMap())).thenReturn(mock);
                     when(mock.run(any(String[].class))).thenReturn(context);
                 });
             MockedStatic<SpringApplication> springApplicationStatic = mockStatic(SpringApplication.class)) {

            springApplicationStatic.when(() -> SpringApplication.exit(context)).thenReturn(0);

            int exitCode = Application.runAutomatedTask(args);

            assertThat(exitCode).isZero();

            SpringApplicationBuilder app = construction.constructed().get(0);
            verify(app).web(WebApplicationType.NONE);
            verify(app).properties(Map.of(Application.AUTOMATED_TASK_PROPERTY, "true"));
            verify(app).run(args);
            springApplicationStatic.verify(() -> SpringApplication.exit(context));
        }
    }
}
