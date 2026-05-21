package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractLegacyWireMockIntegrationTest;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static uk.gov.hmcts.reform.opal.service.synchronise.TestHelperUtil.legacyBusinessUnitUser;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_METHOD)
@DisplayName("SynchronisePermissionsService integration tests")
class SynchronisePermissionsServiceIntegrationTest extends AbstractLegacyWireMockIntegrationTest {

    private static final long USER_WITH_EXISTING_ROLE = 500000000L;
    private static final String ROLE_MAPPING_USER_PREFIX = "ROLE_MAPPING_USER_";
    private static final String SYNC_STAGE = "synchronise roles";
    private static final String UNEXPECTED_RUNTIME_EXCEPTION_REASON = "unexpected runtime exception";

    @Autowired
    private SynchronisePermissionsService synchronisePermissionsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private TestHelperService testHelperService;

    @BeforeEach
    void initialiseEachTest() {
        testHelperService.clearRoleMappingCacheEntries(ROLE_MAPPING_USER_PREFIX);
    }

    @AfterEach
    void clearTestState() {
        testHelperService.clearRoleMappingCacheEntries(ROLE_MAPPING_USER_PREFIX);
    }

    @Test
    @DisplayName("Happy path should add one role to one business unit")
    void synchronise_happyPath_addsSingleRoleAssignment() throws Exception {
        UserEntity user = userRepository.findById(USER_WITH_EXISTING_ROLE).orElseThrow();

        legacyWireMockXmlStubHelper.registerSystemUserLookupStub(List.of("123"));
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(legacyBusinessUnitUsersForTargetUser());
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();
        redisTemplate.opsForValue().set(
            cacheKey,
            objectMapper.writeValueAsString(roleMappingWithSingleRoleAddition())
        );

        LocalDateTime activationBefore = testHelperService.getActivationDate(USER_WITH_EXISTING_ROLE);
        assertThat(activationBefore).isNull();
        assertThat(testHelperService.countRoleAssignmentsForUserBusinessUnit(USER_WITH_EXISTING_ROLE, (short) 70, 3L))
            .isEqualTo(0L);
        long roleAssignmentsBefore = testHelperService.countRoleAssignments(USER_WITH_EXISTING_ROLE);

        try {
            synchronisePermissionsService.synchronise(user);
        } finally {
            redisTemplate.delete(cacheKey);
        }

        assertThat(testHelperService.countRoleAssignments(USER_WITH_EXISTING_ROLE)).isEqualTo(roleAssignmentsBefore + 1);
        assertThat(testHelperService.countRoleAssignmentsForUserBusinessUnit(USER_WITH_EXISTING_ROLE, (short) 70, 1L))
            .isGreaterThan(0L);
        assertThat(testHelperService.countRoleAssignmentsForUserBusinessUnit(USER_WITH_EXISTING_ROLE, (short) 70, 2L))
            .isGreaterThan(0L);
        assertThat(testHelperService.countRoleAssignmentsForUserBusinessUnit(USER_WITH_EXISTING_ROLE, (short) 70, 3L))
            .isGreaterThan(0L);
        assertThat(testHelperService.getActivationDate(USER_WITH_EXISTING_ROLE)).isNotNull();
    }

    @Test
    @DisplayName("Should rollback role change when activateUser throws")
    void synchronise_rollsBackRoleChange_whenActivateUserThrows() throws Exception {
        UserEntity user = userRepository.findById(USER_WITH_EXISTING_ROLE).orElseThrow();
        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();

        // Same setup as happy path, but without security context to force activateUser failure.
        legacyWireMockXmlStubHelper.registerSystemUserLookupStub(List.of("123"));
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(legacyBusinessUnitUsersForTargetUser());
        redisTemplate.opsForValue().set(
            cacheKey,
            objectMapper.writeValueAsString(roleMappingWithSingleRoleAddition())
        );

        long roleAssignmentsBefore = testHelperService.countRoleAssignments(USER_WITH_EXISTING_ROLE);
        assertThat(testHelperService.getActivationDate(USER_WITH_EXISTING_ROLE)).isNull();

        try {
            assertThatThrownBy(() -> synchronisePermissionsService.synchronise(user))
                .isInstanceOf(SynchronisePermissionsException.class)
                .hasMessage(TestHelperUtil.synchronisePermissionsErrorMessage(
                    USER_WITH_EXISTING_ROLE,
                    SYNC_STAGE,
                    UNEXPECTED_RUNTIME_EXCEPTION_REASON
                ));
        } finally {
            redisTemplate.delete(cacheKey);
        }

        assertThat(testHelperService.countRoleAssignments(USER_WITH_EXISTING_ROLE)).isEqualTo(roleAssignmentsBefore);
        assertThat(testHelperService.countRoleAssignmentsForUserBusinessUnit(USER_WITH_EXISTING_ROLE, (short) 70, 3L))
            .isEqualTo(0L);
        assertThat(testHelperService.getActivationDate(USER_WITH_EXISTING_ROLE)).isNull();
    }

    public static List<LegacyBusinessUnitUserId> legacyBusinessUnitUsersForTargetUser() {
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

    public static Map<String, Set<String>> roleMappingWithSingleRoleAddition() {
        return Map.of(
            "1", Set.of("70"),
            "2", Set.of("70"),
            "3", Set.of("70")
        );
    }
}
