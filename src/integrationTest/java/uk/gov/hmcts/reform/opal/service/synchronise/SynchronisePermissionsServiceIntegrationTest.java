package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUser;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUsersRequest;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUsersResponse;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyGetUserRequest;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyGetUserResponse;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_METHOD)
@Import(SynchronisePermissionsServiceIntegrationTest.LegacyUserServiceStubConfiguration.class)
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
    private FakeLegacyUserServiceStub legacyUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        legacyUserService.reset();
    }

    @Test
    @DisplayName("Happy path should add one role to one business unit")
    void synchronise_happyPath_addsSingleRoleAssignment() throws Exception {
        UserEntity user = userRepository.findById(TARGET_USER_ID).orElseThrow();
        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();

        setAuthenticatedUser(user.getTokenSubject());
        legacyUserService.setBusinessUnitUsers(legacyBusinessUnitUsersForTargetUser());
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(roleMappingWithSingleRoleAddition()));

        Timestamp activationBefore = getActivationDate(TARGET_USER_ID);
        long roleAssignmentsBefore = countRoleAssignments(TARGET_USER_ID);
        assertThat(activationBefore).isNull();
        assertThat(hasRoleAssignment(TARGET_USER_ID, (short) 70, 3L)).isFalse();

        try {
            synchronisePermissionsService.synchronise(user);
        } finally {
            redisTemplate.delete(cacheKey);
        }

        assertThat(countRoleAssignments(TARGET_USER_ID)).isEqualTo(roleAssignmentsBefore + 1);
        assertThat(hasRoleAssignment(TARGET_USER_ID, (short) 70, 1L)).isTrue();
        assertThat(hasRoleAssignment(TARGET_USER_ID, (short) 70, 2L)).isTrue();
        assertThat(hasRoleAssignment(TARGET_USER_ID, (short) 70, 3L)).isTrue();
        assertThat(getActivationDate(TARGET_USER_ID)).isNotNull();
    }

    @Test
    @DisplayName("Should rollback role change when activateUser throws")
    void synchronise_rollsBackRoleChange_whenActivateUserThrows() throws Exception {
        UserEntity user = userRepository.findById(TARGET_USER_ID).orElseThrow();
        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();

        // Same setup as happy path, but without security context to force activateUser failure.
        legacyUserService.setBusinessUnitUsers(legacyBusinessUnitUsersForTargetUser());
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(roleMappingWithSingleRoleAddition()));

        long roleAssignmentsBefore = countRoleAssignments(TARGET_USER_ID);
        assertThat(getActivationDate(TARGET_USER_ID)).isNull();

        try {
            assertThatThrownBy(() -> synchronisePermissionsService.synchronise(user))
                .isInstanceOf(InternalServerErrorException.class)
                .hasMessageContaining("No authenticated user found");
        } finally {
            redisTemplate.delete(cacheKey);
        }

        assertThat(countRoleAssignments(TARGET_USER_ID)).isEqualTo(roleAssignmentsBefore);
        assertThat(hasRoleAssignment(TARGET_USER_ID, (short) 70, 3L)).isFalse();
        assertThat(getActivationDate(TARGET_USER_ID)).isNull();
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

    private List<LegacyBusinessUnitUser> legacyBusinessUnitUsersForTargetUser() {
        return List.of(
            legacyBusinessUnitUser("L065JG", "70"),
            legacyBusinessUnitUser("L066JG", "68"),
            legacyBusinessUnitUser("L067JG", "73"),
            legacyBusinessUnitUser("L073JG", "71"),
            legacyBusinessUnitUser("L077JG", "67"),
            legacyBusinessUnitUser("L078JG", "69"),
            legacyBusinessUnitUser("L080JG", "61")
        );
    }

    private Map<String, Set<String>> roleMappingWithSingleRoleAddition() {
        return Map.of(
            "1", Set.of("70"),
            "2", Set.of("70"),
            "3", Set.of("70")
        );
    }

    private LegacyBusinessUnitUser legacyBusinessUnitUser(String businessUnitUserId, String businessUnitId) {
        return LegacyBusinessUnitUser.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnitId(businessUnitId)
            .build();
    }

    private long countRoleAssignments(Long userId) {
        Long count = jdbcTemplate.queryForObject(
            """
                SELECT count(*)
                FROM business_unit_user_roles buur
                JOIN business_unit_users buu ON buu.business_unit_user_id = buur.business_unit_user_id
                WHERE buu.user_id = ?
                """,
            Long.class,
            userId
        );
        return count == null ? 0L : count;
    }

    private boolean hasRoleAssignment(Long userId, short businessUnitId, long roleId) {
        Long count = jdbcTemplate.queryForObject(
            """
                SELECT count(*)
                FROM business_unit_user_roles buur
                JOIN business_unit_users buu ON buu.business_unit_user_id = buur.business_unit_user_id
                WHERE buu.user_id = ? AND buu.business_unit_id = ? AND buur.role_id = ?
                """,
            Long.class,
            userId,
            businessUnitId,
            roleId
        );
        return count != null && count > 0;
    }

    private Timestamp getActivationDate(Long userId) {
        return jdbcTemplate.queryForObject(
            "SELECT activation_date FROM users WHERE user_id = ?",
            Timestamp.class,
            userId
        );
    }

    @TestConfiguration
    static class LegacyUserServiceStubConfiguration {
        @Bean
        @Primary
        FakeLegacyUserServiceStub legacyUserServiceStub() {
            return new FakeLegacyUserServiceStub();
        }
    }

    static class FakeLegacyUserServiceStub extends FakeLegacyUserService {

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
