package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.LegacyWireMockXmlStubHelper;
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
class SynchronisePermissionsServiceIntegrationTest extends AbstractIntegrationTest {

    private static final long TARGET_USER_ID = 500000000L;
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

        legacyWireMockXmlStubHelper.registerSystemUserLookupStub(List.of("123"));
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(legacyBusinessUnitUsersForTargetUser());
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();
        redisTemplate.opsForValue().set(
            cacheKey,
            objectMapper.writeValueAsString(roleMappingWithSingleRoleAddition())
        );

        LocalDateTime activationBefore = testHelperService.getActivationDate(TARGET_USER_ID);
        assertThat(activationBefore).isNull();
        assertThat(testHelperService.countRoleAssignmentsForUserBusinessUnit(TARGET_USER_ID, (short) 70, 3L))
            .isEqualTo(0L);
        long roleAssignmentsBefore = testHelperService.countRoleAssignments(TARGET_USER_ID);

        try {
            synchronisePermissionsService.synchronise(user);
        } finally {
            redisTemplate.delete(cacheKey);
        }

        assertThat(testHelperService.countRoleAssignments(TARGET_USER_ID)).isEqualTo(roleAssignmentsBefore + 1);
        assertThat(testHelperService.countRoleAssignmentsForUserBusinessUnit(TARGET_USER_ID, (short) 70, 1L))
            .isGreaterThan(0L);
        assertThat(testHelperService.countRoleAssignmentsForUserBusinessUnit(TARGET_USER_ID, (short) 70, 2L))
            .isGreaterThan(0L);
        assertThat(testHelperService.countRoleAssignmentsForUserBusinessUnit(TARGET_USER_ID, (short) 70, 3L))
            .isGreaterThan(0L);
        assertThat(testHelperService.getActivationDate(TARGET_USER_ID)).isNotNull();
    }

    @Test
    @DisplayName("Should rollback role change when activateUser throws")
    void synchronise_rollsBackRoleChange_whenActivateUserThrows() throws Exception {
        UserEntity user = userRepository.findById(TARGET_USER_ID).orElseThrow();
        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();

        // Same setup as happy path, but without security context to force activateUser failure.
        legacyWireMockXmlStubHelper.registerSystemUserLookupStub(List.of("123"));
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(legacyBusinessUnitUsersForTargetUser());
        redisTemplate.opsForValue().set(
            cacheKey,
            objectMapper.writeValueAsString(roleMappingWithSingleRoleAddition())
        );

        long roleAssignmentsBefore = testHelperService.countRoleAssignments(TARGET_USER_ID);
        assertThat(testHelperService.getActivationDate(TARGET_USER_ID)).isNull();

        try {
            assertThatThrownBy(() -> synchronisePermissionsService.synchronise(user))
                .isInstanceOf(SynchronisePermissionsException.class)
                .hasMessage(TestHelperUtil.synchronisePermissionsErrorMessage(
                    TARGET_USER_ID,
                    SYNC_STAGE,
                    UNEXPECTED_RUNTIME_EXCEPTION_REASON
                ));
        } finally {
            redisTemplate.delete(cacheKey);
        }

        assertThat(testHelperService.countRoleAssignments(TARGET_USER_ID)).isEqualTo(roleAssignmentsBefore);
        assertThat(testHelperService.countRoleAssignmentsForUserBusinessUnit(TARGET_USER_ID, (short) 70, 3L))
            .isEqualTo(0L);
        assertThat(testHelperService.getActivationDate(TARGET_USER_ID)).isNull();
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
