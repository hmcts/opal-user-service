package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.LegacyWireMockXmlStubHelper;
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

    private LegacyWireMockXmlStubHelper legacyWireMockXmlStubHelper;

    @BeforeEach
    void initialiseLegacyGatewayWireMock() throws Exception {
        legacyWireMockXmlStubHelper = LegacyWireMockXmlStubHelper.initialise(objectMapper);
    }

    @AfterEach
    void clearTestState() throws Exception {
        SecurityContextHolder.clearContext();
        if (legacyWireMockXmlStubHelper != null) {
            legacyWireMockXmlStubHelper.clearRegisteredStubs();
        }
    }

    @Test
    @DisplayName("Happy path should add one role to one business unit")
    void synchronise_happyPath_addsSingleRoleAssignment() throws Exception {
        UserEntity user = userRepository.findById(TARGET_USER_ID).orElseThrow();
        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();

        legacyWireMockXmlStubHelper.registerSystemUserLookupStub(List.of("123"));
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(legacyBusinessUnitUsersForTargetUser());
        setAuthenticatedUser(user.getTokenSubject());
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
        legacyWireMockXmlStubHelper.registerSystemUserLookupStub(List.of("123"));
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(legacyBusinessUnitUsersForTargetUser());
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

    private List<LegacyBusinessUnitUserId> legacyBusinessUnitUsersForTargetUser() {
        return List.of(
            legacyBusinessUnitUserId("L065JG", "70"),
            legacyBusinessUnitUserId("L066JG", "68"),
            legacyBusinessUnitUserId("L067JG", "73"),
            legacyBusinessUnitUserId("L073JG", "71"),
            legacyBusinessUnitUserId("L077JG", "67"),
            legacyBusinessUnitUserId("L078JG", "69"),
            legacyBusinessUnitUserId("L080JG", "61")
        );
    }

    private Map<String, Set<String>> roleMappingWithSingleRoleAddition() {
        return Map.of(
            "1", Set.of("70"),
            "2", Set.of("70"),
            "3", Set.of("70")
        );
    }

    private LegacyBusinessUnitUserId legacyBusinessUnitUserId(String businessUnitUserId, String businessUnitId) {
        return LegacyBusinessUnitUserId.builder()
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

}
