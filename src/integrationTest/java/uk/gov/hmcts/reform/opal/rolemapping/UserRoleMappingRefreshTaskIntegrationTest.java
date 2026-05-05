package uk.gov.hmcts.reform.opal.rolemapping;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleMappingRefreshService;

import java.io.IOException;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles("integration")
@TestPropertySource(properties = "opal.automated-task=true")
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@SpringBootTest
@DisplayName("UserRoleMappingRefreshTaskIntegrationTest")
@Slf4j(topic = "opal.UserRoleMappingRefreshTaskIntegrationTest")
class UserRoleMappingRefreshTaskIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private UserRoleMappingRefreshService userRoleMappingRefreshService;

    @Test
    @DisplayName("AC3: should call refreshMappings on startup when AutomatedTask is enabled")
    void shouldCallRefreshMappingsOnStartup() throws IOException {
        verify(userRoleMappingRefreshService, times(1)).refreshMappings();
    }
}
