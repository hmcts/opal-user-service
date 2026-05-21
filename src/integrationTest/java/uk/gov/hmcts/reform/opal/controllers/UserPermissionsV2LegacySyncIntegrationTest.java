package uk.gov.hmcts.reform.opal.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.LegacyWireMockXmlStubHelper;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.service.synchronise.TestHelperService;
import uk.gov.hmcts.reform.opal.service.synchronise.TestHelperUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.opal.entity.BusinessEventLogType.ACCOUNT_ACTIVATION_INITIATED;
import static uk.gov.hmcts.reform.opal.entity.BusinessEventLogType.BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED;
import static uk.gov.hmcts.reform.opal.entity.BusinessEventLogType.ROLE_ASSIGNED_TO_USER;
import static uk.gov.hmcts.reform.opal.entity.BusinessEventLogType.ROLE_UNASSIGNED_FROM_USER;

@ActiveProfiles({"integration"})
@TestPropertySource(properties = {"app-mode=legacy"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_METHOD)
@DisplayName("UserPermissionsV2 legacy synchronisation integration tests")
class UserPermissionsV2LegacySyncIntegrationTest extends AbstractIntegrationTest {

    private static final long USER_ID = 500000001L; // seeded with no permissions
    private static final long USER_WITH_EXISTING_ROLE = 500000000L;
    private static final long DIFFERENT_USER_ID = 500000006L;
    private static final short BUSINESS_UNIT_ID = 69;
    private static final short LEGACY_BUSINESS_UNIT_ID = 67;
    private static final String BUSINESS_UNIT_USER_ID = "L099JG";
    private static final String EXISTING_BUSINESS_UNIT_USER_ID = "L081JG";
    private static final long ROLE_ID = 1L;
    private static final String ROLE_MAPPING_USER_PREFIX = "ROLE_MAPPING_USER_";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestHelperService testHelperService;

    private LegacyWireMockXmlStubHelper legacyWireMockXmlStubHelper;

    @BeforeEach
    void initialiseEachTest() throws Exception {
        legacyWireMockXmlStubHelper = LegacyWireMockXmlStubHelper.initialise(objectMapper);
        testHelperService.resetBusinessEventsTable();
        testHelperService.clearRoleMappingCacheEntries(ROLE_MAPPING_USER_PREFIX);
        legacyWireMockXmlStubHelper.registerSystemUserLookupStub(List.of("SU001"));
    }

    @AfterEach
    void clearTestState() throws Exception {
        SecurityContextHolder.clearContext();
        if (legacyWireMockXmlStubHelper != null) {
            legacyWireMockXmlStubHelper.clearRegisteredStubs();
        }
    }

    @Test
    @DisplayName("AC1/AC13: should remove roles and keep activation date unset when legacy returns no BU data")
    void getUserStateV2_whenLegacyReturnsNoData_removesAllRoles() throws Exception {
        UserEntity user = userRepository.findById(USER_WITH_EXISTING_ROLE).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        testHelperService.assertUserHasNoActivationDate(user.getUserId());
        long roleCountBefore = testHelperService.countRoleAssignments(user.getUserId());
        legacyWireMockXmlStubHelper.registerSystemUserLookupStub(List.of(), 1);
        testHelperService.setRoleMappingCache(
            user,
            Map.of(ROLE_ID, Set.of(LEGACY_BUSINESS_UNIT_ID)),
            ROLE_MAPPING_USER_PREFIX
        );

        assertThat(roleCountBefore).isGreaterThan(0L);

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        assertThat(testHelperService.countRoleAssignments(user.getUserId())).isEqualTo(0L);
        testHelperService.assertUserHasNoActivationDate(user.getUserId());
        testHelperService.assertLoggedBusinessEventTypes();
    }

    @Test
    @DisplayName("AC2: should create missing BUU and assign role from cache when legacy returns BUU")
    void getUserStateV2_createsMissingBusinessUnitUserAndAssignsRole() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID))
        );
        testHelperService.setRoleMappingCache(user, Map.of(1L, Set.of(BUSINESS_UNIT_ID)), ROLE_MAPPING_USER_PREFIX);

        assertThat(testHelperService.businessUnitUserExists(BUSINESS_UNIT_USER_ID)).isFalse();

        mockMvc.perform(get(userStateUri(user.getUserId())));

        testHelperService.assertBusinessUnitUserRow(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        testHelperService.assertLoggedBusinessEventTypes(ROLE_ASSIGNED_TO_USER, ACCOUNT_ACTIVATION_INITIATED);
    }

    @Test
    @DisplayName("AC3: should reassign existing BUU to the requested user and assign role from cache")
    void getUserStateV2_reassignsExistingBusinessUnitUserToRequestedUser() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID))
        );
        testHelperService.setRoleMappingCache(user, Map.of(1L, Set.of(LEGACY_BUSINESS_UNIT_ID)), ROLE_MAPPING_USER_PREFIX);

        testHelperService.assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, DIFFERENT_USER_ID);

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        testHelperService.assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        testHelperService.assertLoggedBusinessEventTypes(ROLE_ASSIGNED_TO_USER, ACCOUNT_ACTIVATION_INITIATED);
    }

    @Test
    @DisplayName("AC4: should update BU on existing BUU and assign role from cache")
    void getUserStateV2_updatesBusinessUnitIdOnExistingBusinessUnitUser() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        testHelperService.updateBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID))
        );
        testHelperService.setRoleMappingCache(user, Map.of(1L, Set.of(BUSINESS_UNIT_ID)), ROLE_MAPPING_USER_PREFIX);

        testHelperService.assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID);

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        testHelperService.assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        testHelperService.assertLoggedBusinessEventTypes(ROLE_ASSIGNED_TO_USER, ACCOUNT_ACTIVATION_INITIATED);
    }

    @Test
    @DisplayName("AC5/AC12: should return valid state, remove roles, and keep activation date unset when cache miss")
    void getUserStateV2_whenRoleMappingCacheEntryMissing_removesAllRoles() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        testHelperService.assertUserHasNoActivationDate(user.getUserId());
        testHelperService.updateBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID))
        );
        testHelperService.insertBusinessUnitUserRole(EXISTING_BUSINESS_UNIT_USER_ID, ROLE_ID);
        long roleCountBefore = testHelperService.countRoleAssignments(user.getUserId());

        assertThat(roleCountBefore).isGreaterThan(0L);

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        assertThat(testHelperService.countRoleAssignments(user.getUserId())).isEqualTo(0L);
        testHelperService.assertUserHasNoActivationDate(user.getUserId());
        testHelperService.assertLoggedBusinessEventTypes(ROLE_UNASSIGNED_FROM_USER);
    }

    @Test
    @DisplayName("AC6: should ignore cached BU ids not returned by legacy and retain role assignments on returned BUUs")
    void getUserStateV2_ignoresCachedBusinessUnitIdsNotReturnedByLegacy() throws Exception {
        final String buuId1 = "L091JG";
        final String buuId2 = "L092JG";
        final short buId1 = 67;
        final short buId2 = 69;
        final short buId3NotReturnedByLegacy = 68;

        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        testHelperService.insertBusinessUnitUser(buuId1, buId1, USER_ID);
        testHelperService.insertBusinessUnitUser(buuId2, buId2, USER_ID);
        testHelperService.insertBusinessUnitUserRole(buuId1, ROLE_ID);
        testHelperService.insertBusinessUnitUserRole(buuId2, ROLE_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(buuId1, buId1),
            TestHelperUtil.legacyBusinessUnitUser(buuId2, buId2)
        ));
        testHelperService.setRoleMappingCache(
            user,
            Map.of(ROLE_ID, Set.of(buId1, buId2, buId3NotReturnedByLegacy)),
            ROLE_MAPPING_USER_PREFIX
        );

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        testHelperService.assertUserBusinessUnitIds(user.getUserId(), buId1, buId2);
        testHelperService.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        testHelperService.assertUserBusinessUnitRoleCount(user.getUserId(), buId3NotReturnedByLegacy, ROLE_ID, 0L);
        testHelperService.assertLoggedBusinessEventTypes(BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED, ACCOUNT_ACTIVATION_INITIATED);
    }

    //AC7/AC9/AC14 is the happy path
    @Test
    @DisplayName("AC7/AC9/AC14: should assign cached role to BU returned by legacy and keep role cache unchanged")
    void getUserStateV2_assignsRoleToAdditionalLegacyBusinessUnitAndDoesNotUpdateRoleMappingCache() throws Exception {
        final String buuId1 = "L094JG";
        final String buuId2 = "L095JG";
        final String buuId3 = "L096JG";
        final short buId1 = 67;
        final short buId2 = 69;
        final short buId3 = 68;

        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        testHelperService.assertUserHasNoActivationDate(user.getUserId());
        testHelperService.insertBusinessUnitUser(buuId1, buId1, USER_ID);
        testHelperService.insertBusinessUnitUser(buuId2, buId2, USER_ID);
        testHelperService.insertBusinessUnitUserRole(buuId1, ROLE_ID);
        testHelperService.insertBusinessUnitUserRole(buuId2, ROLE_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(buuId1, buId1),
            TestHelperUtil.legacyBusinessUnitUser(buuId2, buId2),
            TestHelperUtil.legacyBusinessUnitUser(buuId3, buId3)
        ));
        Map<Long, Set<Short>> roleMappingCache = Map.of(ROLE_ID, Set.of(buId1, buId2, buId3));
        testHelperService.setRoleMappingCache(user, roleMappingCache, ROLE_MAPPING_USER_PREFIX);

        //assertions on the response verify changes from the inner transaction are visible to outer calling code
        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.domains.*.business_unit_users[*].business_unit_id",
                containsInAnyOrder((int) buId1, (int) buId2, (int) buId3)
            ));

        testHelperService.assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3);
        testHelperService.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        testHelperService.assertUserActivationDateIsToday(user.getUserId());
        testHelperService.assertLoggedBusinessEventTypes(BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED, ACCOUNT_ACTIVATION_INITIATED);
        testHelperService.assertRoleMappingCache(user, roleMappingCache, ROLE_MAPPING_USER_PREFIX);
    }

    @Test
    @DisplayName("AC8: should remove role not in cache mapping and assign cached role to all legacy BUUs")
    void getUserStateV2_removesUnmappedRoleAndAssignsCachedRoleAcrossLegacyBusinessUnits() throws Exception {
        final String buuId1 = "L097JG";
        final String buuId2 = "L098JG";
        final String buuId3 = "L100JG";
        final short buId1 = 67;
        final short buId2 = 69;
        final short buId3 = 68;
        final long roleNotInCache = 2L;

        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        testHelperService.insertBusinessUnitUser(buuId1, buId1, USER_ID);
        testHelperService.insertBusinessUnitUser(buuId2, buId2, USER_ID);
        testHelperService.insertBusinessUnitUserRole(buuId1, roleNotInCache);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(buuId1, buId1),
            TestHelperUtil.legacyBusinessUnitUser(buuId2, buId2),
            TestHelperUtil.legacyBusinessUnitUser(buuId3, buId3)
        ));
        testHelperService.setRoleMappingCache(
            user,
            Map.of(ROLE_ID, Set.of(buId1, buId2, buId3)),
            ROLE_MAPPING_USER_PREFIX
        );

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        testHelperService.assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3);
        testHelperService.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        testHelperService.assertUserRoleAssignmentCount(user.getUserId(), roleNotInCache, 0L);
        testHelperService.assertLoggedBusinessEventTypes(ROLE_ASSIGNED_TO_USER, ROLE_UNASSIGNED_FROM_USER, ACCOUNT_ACTIVATION_INITIATED);
    }

    @Test
    @DisplayName("AC10: should keep existing activation date unchanged when user is already activated")
    void getUserStateV2_keepsExistingActivationDateWhenAlreadySet() throws Exception {
        final String buuId1 = "L101JG";
        final String buuId2 = "L102JG";
        final String buuId3 = "L103JG";
        final short buId1 = 67;
        final short buId2 = 69;
        final short buId3 = 68;
        final LocalDateTime existingActivationDate = LocalDateTime.parse("2025-01-02T03:04:05.678");

        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        testHelperService.updateUserActivationDate(user.getUserId(), existingActivationDate);
        testHelperService.insertBusinessUnitUser(buuId1, buId1, USER_ID);
        testHelperService.insertBusinessUnitUser(buuId2, buId2, USER_ID);
        testHelperService.insertBusinessUnitUserRole(buuId1, ROLE_ID);
        testHelperService.insertBusinessUnitUserRole(buuId2, ROLE_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(buuId1, buId1),
            TestHelperUtil.legacyBusinessUnitUser(buuId2, buId2),
            TestHelperUtil.legacyBusinessUnitUser(buuId3, buId3)
        ));
        Map<Long, Set<Short>> roleMappingCache = Map.of(ROLE_ID, Set.of(buId1, buId2, buId3));
        testHelperService.setRoleMappingCache(user, roleMappingCache, ROLE_MAPPING_USER_PREFIX);

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        testHelperService.assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3);
        testHelperService.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        testHelperService.assertUserActivationDate(user.getUserId(), existingActivationDate);
        testHelperService.assertLoggedBusinessEventTypes(BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED);
        testHelperService.assertRoleMappingCache(user, roleMappingCache, ROLE_MAPPING_USER_PREFIX);
    }

    @Test
    @DisplayName("AC11: should synchronise multiple cached roles and remove stale BUU role allocations")
    void getUserStateV2_synchronisesMultipleRolesFromCache() throws Exception {
        final String buuId1 = "L104JG";
        final String buuId2 = "L105JG";
        final String buuId3 = "L106JG";
        final String buuId4 = "L107JG";
        final String buuId5NotInLegacy = "L108JG";
        final short buId1 = 67;
        final short buId2 = 69;
        final short buId3 = 68;
        final short buId4 = 70;
        final short buId5NotInLegacy = 61;
        final long secondRoleId = 2L;
        final long thirdRoleId = 3L;

        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        testHelperService.assertUserHasNoActivationDate(user.getUserId());
        testHelperService.insertBusinessUnitUser(buuId1, buId1, USER_ID);
        testHelperService.insertBusinessUnitUser(buuId2, buId2, USER_ID);
        testHelperService.insertBusinessUnitUser(buuId5NotInLegacy, buId5NotInLegacy, USER_ID);
        testHelperService.insertBusinessUnitUserRole(buuId1, ROLE_ID);
        testHelperService.insertBusinessUnitUserRole(buuId1, secondRoleId);
        testHelperService.insertBusinessUnitUserRole(buuId2, ROLE_ID);
        testHelperService.insertBusinessUnitUserRole(buuId2, secondRoleId);
        testHelperService.insertBusinessUnitUserRole(buuId5NotInLegacy, thirdRoleId);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(buuId1, buId1),
            TestHelperUtil.legacyBusinessUnitUser(buuId2, buId2),
            TestHelperUtil.legacyBusinessUnitUser(buuId3, buId3),
            TestHelperUtil.legacyBusinessUnitUser(buuId4, buId4)
        ));
        testHelperService.setRoleMappingCache(
            user,
            Map.of(
                ROLE_ID, Set.of(buId1, buId2, buId3),
                secondRoleId, Set.of(buId1, buId2, buId3),
                thirdRoleId, Set.of(buId3, buId4)
            ),
            ROLE_MAPPING_USER_PREFIX
        );

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        testHelperService.assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3, buId4);
        testHelperService.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, secondRoleId, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, secondRoleId, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, secondRoleId, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, thirdRoleId, 1L);
        testHelperService.assertBusinessUnitUserRow(buuId4, buId4, USER_ID, thirdRoleId, 1L);
        assertThat(testHelperService.businessUnitUserExists(buuId5NotInLegacy)).isFalse();
        testHelperService.assertUserRoleAssignmentCount(user.getUserId(), ROLE_ID, 3L);
        testHelperService.assertUserRoleAssignmentCount(user.getUserId(), secondRoleId, 3L);
        testHelperService.assertUserRoleAssignmentCount(user.getUserId(), thirdRoleId, 2L);
        assertThat(testHelperService.countRoleAssignments(user.getUserId())).isEqualTo(8L);
        testHelperService.assertUserActivationDateIsToday(user.getUserId());
        testHelperService.assertLoggedBusinessEventTypesInAnyOrder(
            BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED,
            BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED,
            ROLE_ASSIGNED_TO_USER,
            ACCOUNT_ACTIVATION_INITIATED
        );
    }

    // Causing the db exception that triggers the rollback:
    // business unit event with id 1 is inserted, but 'RESTART IDENTITY' means next id used will be 1 so collision
    @Test
    @DisplayName("Should roll back synchronisation when business event persistence fails")
    void getUserStateV2_whenBusinessEventPersistenceFails_rollsBackSynchronisation() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user.getTokenSubject());
        testHelperService.assertUserHasNoActivationDate(user.getUserId());
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID))
        );
        testHelperService.setRoleMappingCache(user, Map.of(ROLE_ID, Set.of(BUSINESS_UNIT_ID)), ROLE_MAPPING_USER_PREFIX);
        testHelperService.insertBusinessEvent(1L, ROLE_ASSIGNED_TO_USER, USER_ID, USER_ID, "{}");
        assertThat(testHelperService.businessUnitUserExists(BUSINESS_UNIT_USER_ID)).isFalse();
        long roleCountBefore = testHelperService.countRoleAssignments(user.getUserId());

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Internal Server Error"))
            .andExpect(jsonPath("$.type").value("https://hmcts.gov.uk/problems/permissions-synchronization"))
            .andExpect(jsonPath("$.detail").value(
                "Could not synchronise permissions for user " + user.getUserId()
                    + " at stage: synchronise roles. Reason: unexpected runtime exception"
            ));

        assertThat(testHelperService.businessUnitUserExists(BUSINESS_UNIT_USER_ID)).isFalse();
        assertThat(testHelperService.countRoleAssignments(user.getUserId())).isEqualTo(roleCountBefore);
        testHelperService.assertUserHasNoActivationDate(user.getUserId());
        testHelperService.assertLoggedBusinessEventTypes(ROLE_ASSIGNED_TO_USER);
    }

    private String userStateUri(long userId) {
        return "/v2/users/" + userId + "/state";
    }
}
