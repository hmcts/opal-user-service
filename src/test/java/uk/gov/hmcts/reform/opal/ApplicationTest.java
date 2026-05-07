package uk.gov.hmcts.reform.opal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
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
    void startShouldRunNormallyWhenAutomatedTaskArgIsMissing() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

        try (MockedConstruction<SpringApplication> construction =
                 mockConstruction(SpringApplication.class, (mock, mockContext) -> {
                     when(mock.run(any(String[].class))).thenReturn(context);
                 })) {

            int exitCode = Application.start(new String[0]);

            assertThat(exitCode).isZero();

            SpringApplication app = construction.constructed().get(0);
            verify(app, never()).setWebApplicationType(WebApplicationType.NONE);
            verify(app).run(new String[0]);
        }
    }

    @Test
    void startShouldSetWebApplicationTypeNoneAndExitWhenAutomatedTaskArgIsPresent() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

        try (MockedConstruction<SpringApplication> construction =
                 mockConstruction(SpringApplication.class, (mock, mockContext) -> {
                     when(mock.run(any(String[].class))).thenReturn(context);
                 });
             MockedStatic<SpringApplication> springApplicationStatic = mockStatic(SpringApplication.class)) {

            springApplicationStatic.when(() -> SpringApplication.exit(context)).thenReturn(0);

            int exitCode = Application.start(
                new String[] {"AutomatedTask:UserRoleMappingFileRefresh"}
            );

            assertThat(exitCode).isZero();

            SpringApplication app = construction.constructed().get(0);
            verify(app).setWebApplicationType(WebApplicationType.NONE);
            verify(app).run(new String[] {"AutomatedTask:UserRoleMappingFileRefresh"});
            springApplicationStatic.verify(() -> SpringApplication.exit(context));
        }
    }
}
