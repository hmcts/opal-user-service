package uk.gov.hmcts.reform.opal.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.opal.common.logging.EventLoggingService;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.service.synchronise.TestHelperUtil;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"integration", "opal"})
@Slf4j(topic = "opal.UserPermissionsControllerIntegrationTest")
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DisplayName("UserPermissionsControllerGetIntegrationTest")
class UserPermissionsControllerGetIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_BASE = "/users";
    private static final String X_NEW_LOGIN = "X-New-Login";

    @MockitoSpyBean
    private EventLoggingService eventLoggingService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @BeforeEach
    void resetLastLoginDate() {
        jdbcTemplate.update(
            "update users set last_login_date = ? where user_id = ?",
            java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 4, 14, 10, 15, 30)),
            500000000L
        );
    }

    private ObjectMapper objectMapper = new ObjectMapper();


    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("V2 with ID Should return 200 and full V2 user state for a user with permissions")
    void getV2UserStateWithId_returnsFullState(boolean newLogin) throws Exception {
        long userIdWithPermissions = 500000000L;
        String subject = "k9LpT2xVqR8m";
        Authentication auth = TestHelperUtil.createJwtPrincipal(subject, "opal-test@HMCTS.NET", "Pablo");
        SecurityContextHolder.getContext().setAuthentication(auth);
        // clear any existing user state in the cache
        String cacheKey = "USER_STATE_" + subject;
        redisTemplate.delete(cacheKey);

        MockHttpServletRequestBuilder builder = get("/v2" + URL_BASE + "/" + userIdWithPermissions + "/state");
        addLoginHeader(newLogin, builder);
        ResultActions actions = mockMvc.perform(builder);

        String body = actions.andReturn().getResponse().getContentAsString();

        actions.andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        JsonNode expectedNode = expectedV2UserState(newLogin);

        JsonNode actualNode = objectMapper.readTree(body);
        assertThat(actualNode).isEqualTo(expectedNode);

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
        // verification of redis caching of user state
        JsonNode actualFromCacheNode = objectMapper.readTree(redisTemplate.opsForValue().get(cacheKey));
        assertThat(actualFromCacheNode).isEqualTo(expectedNode);


        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.MINUTES);
        assertThat(ttl).isBetween(29L, 30L); //30 mins TTL seems to immediately tick down to 29 mins.
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("PO-2816, AC3: V2 with ID should update last_login_date only when newLogin is true")
    void getV2UserStateWithId_updatesLastLoginDateInDb(boolean newLogin) throws Exception {
        long userIdWithPermissions = 500000000L;

        String subject = "k9LpT2xVqR8m";
        Authentication auth = TestHelperUtil.createJwtPrincipal(subject, "opal-test@HMCTS.NET", "Pablo");
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

    @Test
    @DisplayName("PO-2835 AC2: should refresh cached user state and TTL")
    void getV2UserStateViaPrincipal_refreshesTtlOfRedisEntry() throws Exception {
        long userId = 500000000L;
        String subject = "k9LpT2xVqR8m";
        String cacheKey = "USER_STATE_" + subject;
        Authentication auth = TestHelperUtil.createJwtPrincipal(subject, "opal-test@HMCTS.NET", "Pablo");
        SecurityContextHolder.getContext().setAuthentication(auth);

        //call once, check we have the initial TTL
        ResultActions firstCall = mockMvc.perform(get("/v2" + URL_BASE + "/" + userId + "/state"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        String firstBody = firstCall.andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(redisTemplate.opsForValue().get(cacheKey)))
            .isEqualTo(objectMapper.readTree(firstBody));
        Long ttlBeforeRefreshMinutes1 = redisTemplate.getExpire(cacheKey, TimeUnit.MINUTES);
        assertThat(ttlBeforeRefreshMinutes1).isBetween(29L, 30L);

        // expire the cache entry so that only 5 minutes left
        redisTemplate.expire(cacheKey, 5, TimeUnit.MINUTES);
        Long ttlBeforeRefreshMinutes2 = redisTemplate.getExpire(cacheKey, TimeUnit.MINUTES);
        assertThat(ttlBeforeRefreshMinutes2).isBetween(4L, 5L);

        //call a second time and check we get the new TTL.
        ResultActions secondCall = mockMvc.perform(get("/v2" + URL_BASE + "/" + userId + "/state"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        String secondBody = secondCall.andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(redisTemplate.opsForValue().get(cacheKey)))
            .isEqualTo(objectMapper.readTree(secondBody));
        Long ttlBeforeRefreshMinutes3 = redisTemplate.getExpire(cacheKey, TimeUnit.MINUTES);
        assertThat(ttlBeforeRefreshMinutes3).isBetween(29L, 30L);
    }

    @Test
    @DisplayName("PO-2835 AC3: cached user state should expire and return no data")
    void getV2UserStateWithId_cachedStateExpiresAfterTtl() throws Exception {
        long userId = 500000000L;
        String subject = "k9LpT2xVqR8m";
        String cacheKey = "USER_STATE_" + subject;
        Authentication auth = TestHelperUtil.createJwtPrincipal(subject, "opal-test@HMCTS.NET", "Pablo");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Populate cache by calling the API with valid data.
        mockMvc.perform(get("/v2" + URL_BASE + "/" + userId + "/state"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        JsonNode expectedNode = expectedV2UserState(false);
        JsonNode actualNode = objectMapper.readTree(redisTemplate.opsForValue().get(cacheKey));
        assertThat(actualNode).isEqualTo(expectedNode);

        // Simulate "30 minutes later"
        Boolean ttlSet = redisTemplate.expire(cacheKey, 1, TimeUnit.SECONDS);
        assertThat(ttlSet).isTrue();

        // Poll until the entry disappears from Redis
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (cachedValue != null && System.nanoTime() < deadlineNanos) {
            Thread.sleep(100L);
            cachedValue = redisTemplate.opsForValue().get(cacheKey);
        }

        assertThat(cachedValue).isNull();
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
        Clock fixedClock() {
            return Clock.fixed(
                Instant.parse("2026-04-14T10:20:30Z"),
                ZoneId.of("UTC")
            );
        }
    }

    public static final String EXPECTED_V2_USER_STATE =
        """
            {
              "user_id" : 500000000,
              "username" : "opal-test@HMCTS.NET",
              "name" : "Pablo",
              "status" : "PENDING",
              "version" : 0,
              "cache_name" : "USER_STATE_k9LpT2xVqR8m",
              "domains" : {
                "fines" : {
                  "business_unit_users" : [ {
                    "business_unit_user_id" : "L065JG",
                    "business_unit_id" : 70,
                    "permissions" : [ {
                      "permission_id" : 1,
                      "permission_name" : "Create and Manage Draft Accounts"
                    }, {
                      "permission_id" : 3,
                      "permission_name" : "Account Enquiry"
                    }, {
                      "permission_id" : 4,
                      "permission_name" : "Collection Order"
                    }, {
                      "permission_id" : 5,
                      "permission_name" : "Check and Validate Draft Accounts"
                    }, {
                      "permission_id" : 6,
                      "permission_name" : "Search and view accounts"
                    }, {
                       "permission_id": 7,
                       "permission_name": "Account Maintenance"
                     } ]
                  }, {
                    "business_unit_user_id" : "L066JG",
                    "business_unit_id" : 68,
                    "permissions" : [ ]
                  }, {
                    "business_unit_user_id" : "L067JG",
                    "business_unit_id" : 73,
                    "permissions" : [ ]
                  }, {
                    "business_unit_user_id" : "L073JG",
                    "business_unit_id" : 71,
                    "permissions" : [ ]
                  }, {
                    "business_unit_user_id" : "L077JG",
                    "business_unit_id" : 67,
                    "permissions" : [ ]
                  }, {
                    "business_unit_user_id" : "L078JG",
                    "business_unit_id" : 69,
                    "permissions" : [ ]
                  }, {
                    "business_unit_user_id" : "L080JG",
                    "business_unit_id" : 61,
                    "permissions" : [ ]
                  } ]
                }
              }
            }""";

    private JsonNode expectedV2UserState(boolean newLogin) throws Exception {
        ObjectNode expectedNode = (ObjectNode) objectMapper.readTree(EXPECTED_V2_USER_STATE);
        expectedNode.put("version", newLogin ? 1 : 0);
        return expectedNode;
    }

    private void addLoginHeader(Boolean newLogin, MockHttpServletRequestBuilder builder) {
        Optional.ofNullable(newLogin).ifPresent(value -> builder.header(X_NEW_LOGIN, value));

    }
}
