package uk.gov.hmcts.reform.opal.rolemapping;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.service.synchronise.TestHelperUtil;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("integration")
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DisplayName("UserServiceNormalModeIntegrationTest")
@Slf4j(topic = "opal.UserServiceNormalModeIntegrationTest")
class UserServiceNormalModeIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_BASE = "/v2/users";

    @Test
    @DisplayName("AC2: should load APIs normally when AutomatedTask is absent")
    void shouldLoadApiNormally() throws Exception {

        Authentication auth = TestHelperUtil.createJwtPrincipal("k9LpT2xVqR8m","opal-test@HMCTS.NET", "Pablo");
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequestBuilder builder = get(URL_BASE + "/0/state");
        mockMvc.perform(builder)
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.user_id").value(500000000))
            .andExpect(jsonPath("$.username").value("opal-test@HMCTS.NET"));
    }
}
