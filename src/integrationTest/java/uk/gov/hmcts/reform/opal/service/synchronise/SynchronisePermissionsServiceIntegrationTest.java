package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUser;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUsersRequest;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUsersResponse;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyGetUserRequest;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyGetUserResponse;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_METHOD)
@Import(SynchronisePermissionsServiceIntegrationTest.TestLegacyUserServiceConfiguration.class)
@DisplayName("SynchronisePermissionsService integration tests")
class SynchronisePermissionsServiceIntegrationTest extends AbstractIntegrationTest {

    private static final long TARGET_USER_ID = 500000000L;
    private static final String ROLE_MAPPING_USER_PREFIX = "ROLE_MAPPING_USER_";

    @Autowired
    private SynchronisePermissionsService synchronisePermissionsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TestLegacyUserService legacyUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        legacyUserService.reset();
    }

    @Test
    @DisplayName("Should exercise BUU update/insert/delete and role sync end-to-end using real DB and Redis")
    void synchronise_exercises_child_service_scenarios_end_to_end() throws Exception {
        UserEntity user = userRepository.findById(TARGET_USER_ID).orElseThrow();
        setAuthenticatedUser(user.getTokenSubject());
        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();

        legacyUserService.setBusinessUnitUsers(List.of(
            legacyBusinessUnitUser("L081JG", "70"),
            legacyBusinessUnitUser("L066JG", "68"),
            legacyBusinessUnitUser("L067JG", "73"),
            legacyBusinessUnitUser("L099JG", "69")
        ));

        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(Map.of(
            "2", Set.of("68", "73"),
            "3", Set.of("68", "70")
        )));

        jdbcTemplate.update(
            "INSERT INTO business_unit_users (business_unit_user_id, business_unit_id, user_id) VALUES (?, ?, ?)",
            "L092JG", 69, TARGET_USER_ID
        );
        jdbcTemplate.update(
            "INSERT INTO user_entitlements (user_entitlement_id, business_unit_user_id, application_function_id) "
                + "VALUES (?, ?, ?)",
            995001L, "L092JG", 41L
        );
        jdbcTemplate.update(
            "INSERT INTO business_unit_user_roles (business_unit_user_role_id, business_unit_user_id, role_id) "
                + "VALUES (?, ?, ?)",
            995001L, "L092JG", 2L
        );

        assertThat(asLong(getBusinessUnitUserRow("L081JG").get("user_id"))).isEqualTo(500000006L);
        assertThat(asInt(getBusinessUnitUserRow("L081JG").get("business_unit_id"))).isEqualTo(67);
        assertThat(businessUnitUserExists("L099JG")).isFalse();
        assertThat(userEntitlementCount("L092JG")).isEqualTo(1L);
        assertThat(userRoleMappingCount("L092JG")).isEqualTo(1L);

        try {
            synchronisePermissionsService.synchronise(user);
        } finally {
            redisTemplate.delete(cacheKey);
        }

        assertThat(asLong(getBusinessUnitUserRow("L081JG").get("user_id"))).isEqualTo(TARGET_USER_ID);
        assertThat(asInt(getBusinessUnitUserRow("L081JG").get("business_unit_id"))).isEqualTo(70);
        assertThat(asLong(getBusinessUnitUserRow("L099JG").get("user_id"))).isEqualTo(TARGET_USER_ID);
        assertThat(asInt(getBusinessUnitUserRow("L099JG").get("business_unit_id"))).isEqualTo(69);

        assertThat(businessUnitUserExists("L092JG")).isFalse();
        assertThat(userEntitlementCount("L092JG")).isZero();
        assertThat(userRoleMappingCount("L092JG")).isZero();

        assertThat(getBusinessUnitUserIdsForUser(TARGET_USER_ID))
            .containsExactly("L066JG", "L067JG", "L081JG", "L099JG");

        assertThat(getAssignedBusinessUnitIds(TARGET_USER_ID, 1L)).isEmpty();
        assertThat(getAssignedBusinessUnitIds(TARGET_USER_ID, 2L))
            .containsExactlyInAnyOrder((short) 68, (short) 73);
        assertThat(getAssignedBusinessUnitIds(TARGET_USER_ID, 3L))
            .containsExactlyInAnyOrder((short) 68, (short) 70);
    }

    @Test
    @DisplayName("Should rollback all synchronise changes when downstream role processing throws")
    void synchronise_rolls_back_all_changes_when_roles_processing_throws() {
        UserEntity user = userRepository.findById(TARGET_USER_ID).orElseThrow();
        setAuthenticatedUser(user.getTokenSubject());
        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();

        legacyUserService.setBusinessUnitUsers(List.of(
            legacyBusinessUnitUser("L081JG", "70"),
            legacyBusinessUnitUser("L066JG", "68"),
            legacyBusinessUnitUser("L067JG", "73"),
            legacyBusinessUnitUser("L099JG", "69")
        ));

        redisTemplate.opsForValue().set(cacheKey, "not-json");

        Map<String, Object> l081Before = getBusinessUnitUserRow("L081JG");
        List<String> buuIdsBefore = getBusinessUnitUserIdsForUser(TARGET_USER_ID);
        Map<Long, Set<Short>> rolesBefore = getAssignedRolesByBusinessUnitIds(TARGET_USER_ID);

        try {
            assertThatThrownBy(() -> synchronisePermissionsService.synchronise(user))
                .isInstanceOf(RoleMappingCacheLookupException.class)
                .hasMessage("Could not parse role mapping cache");
        } finally {
            redisTemplate.delete(cacheKey);
        }

        assertThat(getBusinessUnitUserRow("L081JG")).isEqualTo(l081Before);
        assertThat(businessUnitUserExists("L099JG")).isFalse();
        assertThat(getBusinessUnitUserIdsForUser(TARGET_USER_ID)).isEqualTo(buuIdsBefore);
        assertThat(getAssignedRolesByBusinessUnitIds(TARGET_USER_ID)).isEqualTo(rolesBefore);
    }

    private void setAuthenticatedUser(String tokenSubject) {
        Jwt jwt = new Jwt(
            "integration-test-token",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "none"),
            Map.of("sub", tokenSubject)
        );

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private LegacyBusinessUnitUser legacyBusinessUnitUser(String businessUnitUserId, String businessUnitId) {
        return LegacyBusinessUnitUser.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnitId(businessUnitId)
            .build();
    }

    private Map<String, Object> getBusinessUnitUserRow(String businessUnitUserId) {
        return jdbcTemplate.queryForMap(
            "SELECT business_unit_user_id, business_unit_id, user_id "
                + "FROM business_unit_users WHERE business_unit_user_id = ?",
            businessUnitUserId
        );
    }

    private List<String> getBusinessUnitUserIdsForUser(Long userId) {
        return jdbcTemplate.queryForList(
            "SELECT business_unit_user_id FROM business_unit_users WHERE user_id = ? ORDER BY business_unit_user_id",
            String.class,
            userId
        );
    }

    private Set<Short> getAssignedBusinessUnitIds(Long userId, Long roleId) {
        List<Short> businessUnitIds = jdbcTemplate.queryForList(
            """
                SELECT buu.business_unit_id
                FROM business_unit_user_roles buur
                JOIN business_unit_users buu ON buu.business_unit_user_id = buur.business_unit_user_id
                WHERE buu.user_id = ? AND buur.role_id = ?
                """,
            Short.class,
            userId,
            roleId
        );
        return new LinkedHashSet<>(businessUnitIds);
    }

    private Map<Long, Set<Short>> getAssignedRolesByBusinessUnitIds(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
                SELECT buur.role_id, buu.business_unit_id
                FROM business_unit_user_roles buur
                JOIN business_unit_users buu ON buu.business_unit_user_id = buur.business_unit_user_id
                WHERE buu.user_id = ?
                ORDER BY buur.role_id, buu.business_unit_id
                """,
            userId
        );

        return rows.stream().collect(Collectors.groupingBy(
            row -> ((Number) row.get("role_id")).longValue(),
            Collectors.mapping(row -> ((Number) row.get("business_unit_id")).shortValue(),
                               Collectors.toCollection(LinkedHashSet::new))
        ));
    }

    private boolean businessUnitUserExists(String businessUnitUserId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM business_unit_users WHERE business_unit_user_id = ?",
            Long.class,
            businessUnitUserId
        );
        return count != null && count > 0;
    }

    private Long userEntitlementCount(String businessUnitUserId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM user_entitlements WHERE business_unit_user_id = ?",
            Long.class,
            businessUnitUserId
        );
    }

    private Long userRoleMappingCount(String businessUnitUserId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM business_unit_user_roles WHERE business_unit_user_id = ?",
            Long.class,
            businessUnitUserId
        );
    }

    private int asInt(Object value) {
        return ((Number) value).intValue();
    }

    private long asLong(Object value) {
        return ((Number) value).longValue();
    }

    @TestConfiguration
    static class TestLegacyUserServiceConfiguration {
        @Bean
        @Primary
        TestLegacyUserService testLegacyUserService() {
            return new TestLegacyUserService();
        }
    }

    static class TestLegacyUserService extends LegacyUserService {

        private volatile LegacyGetUserResponse getUserResponse = LegacyGetUserResponse.builder()
            .count(1)
            .libraUserIds(List.of("123"))
            .build();
        private volatile LegacyBusinessUnitUsersResponse businessUnitUsersResponse = LegacyBusinessUnitUsersResponse
            .builder()
            .count(0)
            .businessUnitUsers(List.of())
            .build();

        @Override
        public LegacyGetUserResponse getUserIds(LegacyGetUserRequest requestDto) {
            return getUserResponse;
        }

        @Override
        public LegacyBusinessUnitUsersResponse getBusinessUnitUsers(LegacyBusinessUnitUsersRequest requestDto) {
            return businessUnitUsersResponse;
        }

        void setBusinessUnitUsers(List<LegacyBusinessUnitUser> businessUnitUsers) {
            this.businessUnitUsersResponse = LegacyBusinessUnitUsersResponse.builder()
                .count(businessUnitUsers.size())
                .businessUnitUsers(businessUnitUsers)
                .build();
        }

        void reset() {
            getUserResponse = LegacyGetUserResponse.builder()
                .count(1)
                .libraUserIds(List.of("123"))
                .build();
            businessUnitUsersResponse = LegacyBusinessUnitUsersResponse.builder()
                .count(0)
                .businessUnitUsers(List.of())
                .build();
        }
    }
}
