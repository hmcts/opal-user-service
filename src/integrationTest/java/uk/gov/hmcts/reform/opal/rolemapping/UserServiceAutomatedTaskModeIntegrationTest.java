package uk.gov.hmcts.reform.opal.rolemapping;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("integration")
@TestPropertySource(properties = "opal.automated-task=true")
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DisplayName("UserServiceAutomatedTaskModeIntegrationTest")
@Slf4j(topic = "opal.UserServiceAutomatedTaskModeIntegrationTest")
@EnabledIfEnvironmentVariable(
    named = "ROLE_MAPPING_INTEGRATION_TESTS",
    matches = "true"
)
class UserServiceAutomatedTaskModeIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_BASE = "/users";

    @Test
    @DisplayName("AC1: should return 404 when AutomatedTask is enabled")
    void shouldReturn404WhenAutomatedTaskIsEnabled() throws Exception {
        long userIdWithPermissions = 500000000L;

        mockMvc.perform(get(URL_BASE + "/" + userIdWithPermissions + "/state"))
            .andExpect(status().isNotFound());
    }
}
