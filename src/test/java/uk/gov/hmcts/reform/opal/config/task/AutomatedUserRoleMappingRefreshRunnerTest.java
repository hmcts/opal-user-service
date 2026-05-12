package uk.gov.hmcts.reform.opal.config.task;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleMappingRefreshService;

@ExtendWith(MockitoExtension.class)
class AutomatedUserRoleMappingRefreshRunnerTest {

    @Mock
    private UserRoleMappingRefreshService service;

    @Mock
    private ApplicationArguments args;

    @InjectMocks
    private AutomatedUserRoleMappingRefreshRunner runner;

    @Test
    void runRefreshesUserRoleMappings() throws Exception {

        // ACT
        runner.run(args);

        // ASSERT
        verify(service).refreshMappings();
    }

    @Test
    void runPropagatesRefreshFailure() throws Exception {

        // ARRANGE
        IOException failure = new IOException("Mapping file unavailable");
        doThrow(failure).when(service).refreshMappings();

        // ACT / ASSERT
        IOException thrown = assertThrows(IOException.class, () -> runner.run(args));
        assertSame(failure, thrown);
        verify(service).refreshMappings();
    }
}
