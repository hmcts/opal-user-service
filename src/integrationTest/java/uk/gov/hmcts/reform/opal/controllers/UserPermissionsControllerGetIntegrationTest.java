package uk.gov.hmcts.reform.opal.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.verification.VerificationMode;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.opal.common.logging.EventLoggingService;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.opal.common.dto.ToJsonString.toPrettyJson;

@ActiveProfiles({"integration"})
@Slf4j(topic = "opal.UserPermissionsControllerIntegrationTest")
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DisplayName("UserPermissionsControllerGetIntegrationTest")
class UserPermissionsControllerGetIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_BASE = "/users";
    private static final String X_NEW_LOGIN = "X-New-Login";

    @MockitoSpyBean
    private EventLoggingService eventLoggingService;

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Should return 200 and full user state for a user with permissions [PO-857]")
    void getUserState_returnsFullState(Boolean newLogin) throws Exception {
        long userIdWithPermissions = 500000000L;

        MockHttpServletRequestBuilder builder = get(URL_BASE + "/" + userIdWithPermissions + "/state")
            .principal(createJwtPrincipal());
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenUserHasPermissions_returns200AndCorrectPayload: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$['user_id']").value(500000000))
            .andExpect(jsonPath("$['username']").value("opal-test@HMCTS.NET"))
            .andExpect(jsonPath("$['business_unit_users']", hasSize(3)))
            .andExpect(jsonPath("$['business_unit_users'][*]['business_unit_id']",
                                containsInAnyOrder(70, 68, 61)))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==70)]['business_unit_user_id']")
                           .value("L065JG"))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==70)]['permissions'][*]['permission_name']",
                                containsInAnyOrder("Account Enquiry - Account Notes", "Account Enquiry")))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==68)]['business_unit_user_id']")
                           .value("L066JG"))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==68)]['permissions'][0]['permission_name']")
                           .value("Account Enquiry - Account Notes"))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==61)]['business_unit_user_id']")
                           .value("L080JG"))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==61)]['permissions'][0]['permission_name']")
                           .value("Collection Order"));

        boolean testNewLogin = Boolean.TRUE.equals(newLogin);
        VerificationMode mode = testNewLogin ? times(1) : never();
        verify(eventLoggingService, mode).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Should return 200 and state with empty list for a user that exists but has no permissions [PO-857]")
    void getUserState_returnsNoUnitsForUnentitledUser(Boolean newLogin) throws Exception {
        long userIdWithoutPermissions = 500000001L;

        MockHttpServletRequestBuilder builder = get(URL_BASE + "/" + userIdWithoutPermissions + "/state")
            .principal(createJwtPrincipal());
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenUserExistsButHasNoPermissions_returns200AndEmptyList: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(header().string("ETag", "\"0\""))
            .andExpect(jsonPath("$['user_id']").value(500000001))
            .andExpect(jsonPath("$['username']").value("opal-test-2@HMCTS.NET"))
            .andExpect(jsonPath("$['status']").value("active"))
            .andExpect(jsonPath("$['business_unit_users']", hasSize(0)));

        boolean testNewLogin = Boolean.TRUE.equals(newLogin);
        VerificationMode mode = testNewLogin ? times(1) : never();
        verify(eventLoggingService, mode).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Get existing user should return 200")
    void getUserState_existingUser(Boolean newLogin) throws Exception {
        long userIdWithoutPermissions = 500000003L;

        MockHttpServletRequestBuilder builder = get(URL_BASE + "/" + userIdWithoutPermissions + "/state")
            .principal(createJwtPrincipal());
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUser: Response body:\n{}", toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(header().string("ETag", "\"2\""))
            .andExpect(jsonPath("$['user_id']").value(500000003))
            .andExpect(jsonPath("$['username']").value("test-user@HMCTS.NET"))
            .andExpect(jsonPath("$['name']").value("Pablo"))
            .andExpect(jsonPath("$['status']").value("active"))
            .andExpect(jsonPath("$['version']").value(2))
            .andExpect(jsonPath("$['business_unit_users']", hasSize(0)));

        boolean testNewLogin = Boolean.TRUE.equals(newLogin);
        VerificationMode mode = testNewLogin ? times(1) : never();
        verify(eventLoggingService, mode).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Should return 200 from Authentication Principal and no User Id")
    void getUserState_existingUserViaPrinciple_noUserId(Boolean newLogin) throws Exception {

        JwtAuthenticationToken jwtAuthToken = createJwtPrincipal();
        MockHttpServletRequestBuilder builder = get(URL_BASE + "/state").principal(jwtAuthToken);
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUserViaPrinciple: Response body:\n{}", toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(header().string("ETag", "\"2\""))
            .andExpect(jsonPath("$['user_id']").value(500000003))
            .andExpect(jsonPath("$['username']").value("test-user@HMCTS.NET"))
            .andExpect(jsonPath("$['name']").value("Pablo"))
            .andExpect(jsonPath("$['status']").value("active"))
            .andExpect(jsonPath("$['version']").value(2))
            .andExpect(jsonPath("$['business_unit_users']", hasSize(0)));

        boolean testNewLogin = Boolean.TRUE.equals(newLogin);
        VerificationMode mode = testNewLogin ? times(1) : never();
        verify(eventLoggingService, mode).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Should return 200 from Authentication Principal")
    void getUserState_existingUserViaPrinciple(Boolean newLogin) throws Exception {

        JwtAuthenticationToken jwtAuthToken = createJwtPrincipal();
        MockHttpServletRequestBuilder builder = get(URL_BASE + "/0/state").principal(jwtAuthToken);
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUserViaPrinciple: Response body:\n{}", toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(header().string("ETag", "\"2\""))
            .andExpect(jsonPath("$['user_id']").value(500000003))
            .andExpect(jsonPath("$['username']").value("test-user@HMCTS.NET"))
            .andExpect(jsonPath("$['name']").value("Pablo"))
            .andExpect(jsonPath("$['status']").value("active"))
            .andExpect(jsonPath("$['version']").value(2))
            .andExpect(jsonPath("$['business_unit_users']", hasSize(0)));

        boolean testNewLogin = Boolean.TRUE.equals(newLogin);
        VerificationMode mode = testNewLogin ? times(1) : never();
        verify(eventLoggingService, mode).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Should return 404 Not Found from Authentication Principal")
    void getUserState_existingUserViaPrinciple_doesNotExist(Boolean newLogin) throws Exception {

        JwtAuthenticationToken jwtAuthToken = createJwtPrincipal("invalid_sub","test-user@HMCTS.NET", "Pablo");
        MockHttpServletRequestBuilder builder = get(URL_BASE + "/0/state").principal(jwtAuthToken);
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUserViaPrinciple_doesNotExist: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isNotFound())
            .andExpect(jsonPath("$['reason']").value("User not found with subject: invalid_sub"));

        verify(eventLoggingService, never()).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Should return 409 Conflict from different preferred username")
    void getUserState_existingUserViaPrinciple_conflitPreferred(Boolean newLogin) throws Exception {

        JwtAuthenticationToken jwtAuthToken = createJwtPrincipal("jjqwGAERGW43","different@HMCTS.NET", "Pablo");
        MockHttpServletRequestBuilder builder = get(URL_BASE + "/0/state").principal(jwtAuthToken);
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUserViaPrinciple_doesNotExist: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isConflict())
            .andExpect(header().string("ETag", "\"2\""))
            .andExpect(jsonPath("$['conflictReason']").value(
                "Preferred Username mismatch: token: different@HMCTS.NET, db: test-user@HMCTS.NET"));

        verify(eventLoggingService, never()).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Should return 409 Conflict from different name")
    void getUserState_existingUserViaPrinciple_conflitName(Boolean newLogin) throws Exception {

        JwtAuthenticationToken jwtAuthToken = createJwtPrincipal("jjqwGAERGW43","test-user@HMCTS.NET", "Peter");
        MockHttpServletRequestBuilder builder = get(URL_BASE + "/0/state").principal(jwtAuthToken);
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_existingUserViaPrinciple_doesNotExist: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isConflict())
            .andExpect(header().string("ETag", "\"2\""))
            .andExpect(jsonPath("$['conflictReason']").value("Name mismatch: token: Peter, db: Pablo"));

        verify(eventLoggingService, never()).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Should return 404 Not Found for a user that does not exist [PO-857]")
    void getUserState_whenUserDoesNotExist_returns404(Boolean newLogin) throws Exception {
        long nonExistentUserId = 999999999L;

        MockHttpServletRequestBuilder builder = get(URL_BASE + "/" + nonExistentUserId + "/state");
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenUserDoesNotExist_returns404: Response body:\n{}", toPrettyJson(body));

        actions.andExpect(status().isNotFound())
            .andExpect(jsonPath("$['reason']").value("User not found with id: 999999999"));

        verify(eventLoggingService, never()).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Should return 406 for invalid User ID format [PO-857]")
    void getUserState_rejectsInvalidUserId(Boolean newLogin) throws Exception {
        String invalidUserId = "invalidUserId";

        MockHttpServletRequestBuilder builder = get(URL_BASE + "/" + invalidUserId + "/state");
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenUserIdFormatIsInvalid_returns406: Response body:\n{}", toPrettyJson(body));

        actions.andExpect(status().isNotAcceptable());

        verify(eventLoggingService, never()).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Should handle business unit with one permission [PO-857]")
    void getUserState_returnsSinglePermission(Boolean newLogin) throws Exception {
        long userIdWithPermissions = 500000000L;

        MockHttpServletRequestBuilder builder = get(URL_BASE + "/" + userIdWithPermissions + "/state")
            .principal(createJwtPrincipal());
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenBusinessUnitHasOnePermission_returnsCorrectPermission: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==68)]['permissions']", hasSize(1)))
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==68)]['permissions'][0]['permission_name']")
                .value("Account Enquiry - Account Notes"));

        boolean testNewLogin = Boolean.TRUE.equals(newLogin);
        VerificationMode mode = testNewLogin ? times(1) : never();
        verify(eventLoggingService, mode).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false, true})
    @DisplayName("Should handle business unit with multiple permissions [PO-857]")
    void getUserState_returnsMultiplePermissions(Boolean newLogin) throws Exception {
        long userIdWithPermissions = 500000000L;

        MockHttpServletRequestBuilder builder = get(URL_BASE + "/" + userIdWithPermissions + "/state")
            .principal(createJwtPrincipal());
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();
        log.info(":getUserState_whenBusinessUnitHasMultiplePermissions_returnsAllPermissions: Response body:\n{}",
                 toPrettyJson(body));

        actions.andExpect(status().isOk())
            .andExpect(jsonPath(
                "$['business_unit_users'][?(@['business_unit_id']==70)]['permissions'][*]['permission_name']",
                        containsInAnyOrder("Account Enquiry - Account Notes", "Account Enquiry")));

        boolean testNewLogin = Boolean.TRUE.equals(newLogin);
        VerificationMode mode = testNewLogin ? times(1) : never();
        verify(eventLoggingService, mode).logEvent(any(), any(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("V2 with ID Should return 200 and full V2 user state for a user with permissions")
    void getV2UserStateWithId_returnsFullState(boolean newLogin) throws Exception {
        long userIdWithPermissions = 500000000L;
        Authentication auth = createJwtPrincipal("k9LpT2xVqR8m","opal-test@HMCTS.NET", "Pablo");
        SecurityContextHolder.getContext().setAuthentication(auth);
        MockHttpServletRequestBuilder builder = get("/v2" + URL_BASE + "/" + userIdWithPermissions + "/state");
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        assertThat(body).isEqualTo(objectMapper.readTree(EXPECTED_V2_USER_STATE).toString());
        if (newLogin) {
            verify(eventLoggingService).logEvent(
                eq("User Authentication"),
                eq("Success"),
                isNull(),
                eq("Authentication"),
                any(),
                argThat(data ->
                            data != null
                                && data.size() == 1
                                && Long.valueOf(500000000L).equals(data.get("UserIdentifier"))
                ));
        } else {
            verifyNoInteractions(eventLoggingService);
        }

    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("PO-2816, AC3: V2 with ID should update last_login_date only when newLogin is true")
    void getV2UserStateWithId_updatesLastLoginDateInDb(boolean newLogin) throws Exception {
        long userIdWithPermissions = 500000000L;

        Authentication auth = createJwtPrincipal("k9LpT2xVqR8m", "opal-test@HMCTS.NET", "Pablo");
        SecurityContextHolder.getContext().setAuthentication(auth);

        LocalDateTime before = readLastLoginDate(userIdWithPermissions);

        MockHttpServletRequestBuilder builder =
            get("/v2" + URL_BASE + "/" + userIdWithPermissions + "/state");
        addLoginHeader(newLogin, builder);

        mockMvc.perform(builder)
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        LocalDateTime after = readLastLoginDate(userIdWithPermissions);

        if (newLogin) {
            assertThat(after).isNotNull();
            assertThat(after).isAfter(before);
        } else {
            assertThat(after).isEqualTo(before);
        }
    }

    private LocalDateTime readLastLoginDate(long userId) {
        return jdbcTemplate.queryForObject(
            "select last_login_date from users where user_id = ?",
            (rs, rowNum) -> rs.getObject("last_login_date", LocalDateTime.class),
            userId
        );
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock clock() {
            return Clock.fixed(
                Instant.parse("2026-04-14T10:15:30Z"),
                ZoneId.of("UTC")
            );
        }
    }

    public static final String EXPECTED_V2_USER_STATE =
        """
            {
              "user_id": 500000000,
              "username": "opal-test@HMCTS.NET",
              "name": "Pablo",
              "status": "ACTIVE",
              "version": 0,
              "cache_name": null,
              "domains": {
                "fines": {
                  "business_unit_users": [
                    {
                      "business_unit_user_id": "L065JG",
                      "business_unit_id": 70,
                      "permissions": [
                        {
                          "permission_id": 1,
                          "permission_name": "Create and Manage Draft Accounts"
                        },
                        {
                          "permission_id": 3,
                          "permission_name": "Account Enquiry"
                        },
                        {
                          "permission_id": 4,
                          "permission_name": "Collection Order"
                        },
                        {
                          "permission_id": 5,
                          "permission_name": "Check and Validate Draft Accounts"
                        },
                        {
                          "permission_id": 6,
                          "permission_name": "Search and View Accounts"
                        }
                      ]
                    },
                    {
                      "business_unit_user_id": "L066JG",
                      "business_unit_id": 68,
                      "permissions": []
                    },
                    {
                      "business_unit_user_id": "L067JG",
                      "business_unit_id": 73,
                      "permissions": []
                    },
                    {
                      "business_unit_user_id": "L073JG",
                      "business_unit_id": 71,
                      "permissions": []
                    },
                    {
                      "business_unit_user_id": "L077JG",
                      "business_unit_id": 67,
                      "permissions": []
                    },
                    {
                      "business_unit_user_id": "L078JG",
                      "business_unit_id": 69,
                      "permissions": []
                    },
                    {
                      "business_unit_user_id": "L080JG",
                      "business_unit_id": 61,
                      "permissions": []
                    }
                  ]
                }
              }
            }
        """;

    private JwtAuthenticationToken createJwtPrincipal() {
        return createJwtPrincipal("jjqwGAERGW43","test-user@HMCTS.NET", "Pablo");
    }

    private JwtAuthenticationToken createJwtPrincipal(String sub, String preferred, String name) {
        Jwt jwt = new Jwt(
            "mock-token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of("sub", sub,
                   "preferred_username", preferred,
                   "name", name
            )
        );

        return new JwtAuthenticationToken(jwt);
    }

    private void addLoginHeader(Boolean newLogin, MockHttpServletRequestBuilder builder) {
        Optional.ofNullable(newLogin).ifPresent(value -> builder.header(X_NEW_LOGIN, value));

    }
}
