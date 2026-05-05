package uk.gov.hmcts.reform.opal.rolemapping;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;

import java.time.Instant;
import java.util.Map;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ActiveProfiles("integration")
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DisplayName("UserServiceNormalModeIntegrationTest")
@Slf4j(topic = "opal.UserServiceNormalModeIntegrationTest")
class UserServiceNormalModeIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_BASE = "/users";

    @Test
    @DisplayName("AC2: should load APIs normally when AutomatedTask is absent")
    void shouldLoadApiNormally() throws Exception {
        long userIdWithPermissions = 500000000L;

        MockHttpServletRequestBuilder builder = get(URL_BASE + "/" + userIdWithPermissions + "/state")
            .principal(createJwtPrincipal());

        mockMvc.perform(builder)
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON))
            .andExpect(jsonPath("$.user_id").value(500000000))
            .andExpect(jsonPath("$.username").value("opal-test@HMCTS.NET"));
    }

    private JwtAuthenticationToken createJwtPrincipal() {
        Jwt jwt = new Jwt(
            "mock-token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of(
                "sub", "jjqwGAERGW43",
                "preferred_username", "opal-test@HMCTS.NET",
                "name", "Pablo"
            )
        );

        return new JwtAuthenticationToken(jwt);
    }
}
