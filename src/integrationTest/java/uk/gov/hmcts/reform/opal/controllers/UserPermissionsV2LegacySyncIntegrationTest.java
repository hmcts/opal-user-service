package uk.gov.hmcts.reform.opal.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import tools.jackson.databind.JsonNode;
import uk.gov.hmcts.reform.opal.AbstractLegacyWireMockIntegrationTest;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleMappingCacheService;
import uk.gov.hmcts.reform.opal.service.synchronise.TestHelperService;
import uk.gov.hmcts.reform.opal.service.synchronise.TestHelperUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
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

@ActiveProfiles({"integration", "legacy"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_METHOD)
@DisplayName("UserPermissionsV2 legacy synchronisation integration tests")
class UserPermissionsV2LegacySyncIntegrationTest extends AbstractLegacyWireMockIntegrationTest {

    private static final long USER_ID = 500000003L; // seeded with no permissions
    private static final long USER_WITH_EXISTING_ROLE = 500000000L;
    private static final long DIFFERENT_USER_ID = 500000006L;
    private static final short BUSINESS_UNIT_ID = 69;
    private static final short LEGACY_BUSINESS_UNIT_ID = 67;
    private static final String BUSINESS_UNIT_USER_ID = "L099JG";
    private static final String EXISTING_BUSINESS_UNIT_USER_ID = "L081JG";
    private static final long ROLE_ID = 1L;
    private static final String ROLE_MAPPING_USER_PREFIX = "ROLE_MAPPING_USER_";
    private static final String CURRENT_USER_STATE_URI = "/v2/users/0/state";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestHelperService helper;

    @Autowired
    private UserRoleMappingCacheService userRoleMappingCacheService;

    @BeforeEach
    void initialiseEachTest() throws Exception {
        helper.resetBusinessEventsTable();
        helper.clearRoleMappingCacheEntries(ROLE_MAPPING_USER_PREFIX);
        legacyWireMockXmlStubHelper.registerSystemUserLookupStub(List.of("SU001"));
    }

    @Test
    @DisplayName("AC1/AC13: should remove roles and keep activation date unset when legacy returns no BU data")
    void getUserStateV2_whenLegacyReturnsNoData_removesAllRoles() throws Exception {
        UserEntity user = userRepository.findById(USER_WITH_EXISTING_ROLE).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user);
        assertThat(helper.getActivationDate(user.getUserId())).isNull();
        long roleCountBefore = helper.countRoleAssignments(user.getUserId());
        legacyWireMockXmlStubHelper.registerSystemUserLookupStub(List.of(), 1);
        helper.setRoleMappingCache(
            user,
            Map.of(ROLE_ID, Set.of(LEGACY_BUSINESS_UNIT_ID)),
            ROLE_MAPPING_USER_PREFIX);

        assertThat(roleCountBefore).isGreaterThan(0L);

        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk());

        assertThat(helper.countRoleAssignments(user.getUserId())).isEqualTo(0L);
        assertThat(helper.getActivationDate(user.getUserId())).isNull();
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(ROLE_UNASSIGNED_FROM_USER,
                                                                         ROLE_UNASSIGNED_FROM_USER);
    }

    @Test
    @DisplayName("AC2: should create missing BUU and assign role from cache when legacy returns BUU")
    void getUserStateV2_createsMissingBusinessUnitUserAndAssignsRole() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID)));
        helper.setRoleMappingCache(user, Map.of(1L, Set.of(BUSINESS_UNIT_ID)), ROLE_MAPPING_USER_PREFIX);

        assertThat(helper.businessUnitUserExists(BUSINESS_UNIT_USER_ID)).isFalse();

        mockMvc.perform(get(CURRENT_USER_STATE_URI));

        helper.assertBusinessUnitUserRow(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(
            ROLE_ASSIGNED_TO_USER,
            ACCOUNT_ACTIVATION_INITIATED);
    }

    @Test
    @DisplayName("PO-6491: should discard legacy business units that are not present in the cached role mapping")
    void getUserStateV2_doesNotPersistLegacyBusinessUnitsWithoutCachedRoleMappings() throws Exception {
        final String mappedBusinessUnitUserId = "L109JG";
        final String unmappedBusinessUnitUserId = "L110JG";
        final short unmappedBusinessUnitId = 68;

        // Arrange
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(mappedBusinessUnitUserId, BUSINESS_UNIT_ID),
            TestHelperUtil.legacyBusinessUnitUser(unmappedBusinessUnitUserId, unmappedBusinessUnitId)
        ));
        helper.setRoleMappingCache(user, Map.of(ROLE_ID, Set.of(BUSINESS_UNIT_ID)), ROLE_MAPPING_USER_PREFIX);

        // Act
        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.domains.*.business_unit_users[*].business_unit_id",
                containsInAnyOrder((int) BUSINESS_UNIT_ID)
            ));

        // Assert
        helper.assertUserBusinessUnitIds(user.getUserId(), BUSINESS_UNIT_ID);
        helper.assertBusinessUnitUserRow(mappedBusinessUnitUserId, BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        assertThat(helper.businessUnitUserExists(unmappedBusinessUnitUserId)).isFalse();
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(
            ROLE_ASSIGNED_TO_USER,
            ACCOUNT_ACTIVATION_INITIATED
        );
    }

    @Test
    @DisplayName("AC2b: should ignore invalid cached role ids and still return user state")
    void getUserStateV2_ignoresInvalidCachedRoleIdsAndReturnsUserState() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID))
        );
        userRoleMappingCacheService.putUserMapping(
            user.getTokenSubject(),
            Map.of(
                "1", Set.of("69"),
                "999", Set.of("70")
            )
        );

        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.domains.fines.business_unit_users[0].business_unit_id")
                           .value((int) BUSINESS_UNIT_ID));

        helper.assertBusinessUnitUserRow(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        assertThat(helper.countRoleAssignments(user.getUserId())).isEqualTo(1L);
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(
            ROLE_ASSIGNED_TO_USER,
            ACCOUNT_ACTIVATION_INITIATED
        );
    }

    @Test
    @DisplayName(
            "PO-6486 should not add new business events when the same user logs in again without permission changes")
    void getUserStateV2_whenPermissionsAreUnchangedOnSecondLogin_doesNotAddBusinessEvents() throws Exception {
        //Arrange
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID))
        );
        helper.setRoleMappingCache(user, Map.of(ROLE_ID, Set.of(BUSINESS_UNIT_ID)), ROLE_MAPPING_USER_PREFIX);

        //Act & Assert - first login, events should be created
        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk());

        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(
            ROLE_ASSIGNED_TO_USER,
            ACCOUNT_ACTIVATION_INITIATED
        );

        //Act & Assert - second login, no new events should be created as permissions are unchanged
        mockMvc.perform(get(CURRENT_USER_STATE_URI))
                .andExpect(status().isOk());

        helper.assertBusinessUnitUserRow(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(
            ROLE_ASSIGNED_TO_USER,
            ACCOUNT_ACTIVATION_INITIATED
        );
    }

    @Test
    @DisplayName("AC3: should reassign existing BUU to the requested user and assign role from cache")
    void getUserStateV2_reassignsExistingBusinessUnitUserToRequestedUser() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID)));
        helper.setRoleMappingCache(user, Map.of(1L, Set.of(LEGACY_BUSINESS_UNIT_ID)), ROLE_MAPPING_USER_PREFIX);

        helper.assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, DIFFERENT_USER_ID);

        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk());

        helper.assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(
            ROLE_ASSIGNED_TO_USER,
            ACCOUNT_ACTIVATION_INITIATED);
    }

    @Test
    @DisplayName("AC4: should update BU on existing BUU and assign role from cache")
    void getUserStateV2_updatesBusinessUnitIdOnExistingBusinessUnitUser() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user);
        helper.updateBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID)));
        helper.setRoleMappingCache(user, Map.of(1L, Set.of(BUSINESS_UNIT_ID)), ROLE_MAPPING_USER_PREFIX);

        helper.assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID);

        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk());

        helper.assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(
            ROLE_ASSIGNED_TO_USER,
            ACCOUNT_ACTIVATION_INITIATED);
    }

    @Test
    @DisplayName("AC5/AC12: should return valid state, remove roles, and keep activation date unset when cache miss")
    void getUserStateV2_whenRoleMappingCacheEntryMissing_removesAllRoles() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user);
        assertThat(helper.getActivationDate(user.getUserId())).isNull();
        helper.updateBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID)));
        helper.insertBusinessUnitUserRole(EXISTING_BUSINESS_UNIT_USER_ID, ROLE_ID);
        long roleCountBefore = helper.countRoleAssignments(user.getUserId());

        assertThat(roleCountBefore).isGreaterThan(0L);

        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk());

        assertThat(helper.countRoleAssignments(user.getUserId())).isEqualTo(0L);
        assertThat(helper.getActivationDate(user.getUserId())).isNull();
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(ROLE_UNASSIGNED_FROM_USER);
    }

    @Test
    @DisplayName("PO-6466: first returned user state should reflect complete role replacement")
    void getUserStateV2_whenRolesAreFullyReplaced_returnsOnlyReplacementRolePermissions() throws Exception {
        final long userId = DIFFERENT_USER_ID;
        final long oldRoleId = 1L;
        final long replacementRoleId = 2L;
        final short firstBusinessUnitId = 67;
        final short secondBusinessUnitId = 69;
        final String firstBusinessUnitUserId = "L081JG";
        final String secondBusinessUnitUserId = "L082JG";

        UserEntity user = userRepository.findById(userId).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user);
        helper.insertBusinessUnitUserRole(firstBusinessUnitUserId, oldRoleId);
        helper.insertBusinessUnitUserRole(secondBusinessUnitUserId, oldRoleId);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(firstBusinessUnitUserId, firstBusinessUnitId),
            TestHelperUtil.legacyBusinessUnitUser(secondBusinessUnitUserId, secondBusinessUnitId)));
        helper.setRoleMappingCache(
            user,
            Map.of(replacementRoleId, Set.of(firstBusinessUnitId, secondBusinessUnitId)),
            ROLE_MAPPING_USER_PREFIX);

        String responseBody = mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.domains.fines.business_unit_users.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(permissionNamesForBusinessUnit(responseBody, firstBusinessUnitId))
            .contains("Collection Order", "Check and Validate Draft Accounts", "Search and view accounts")
            .doesNotContain("Create and Manage Draft Accounts", "Account Enquiry - Account Notes");
        assertThat(permissionNamesForBusinessUnit(responseBody, secondBusinessUnitId))
            .contains("Collection Order", "Check and Validate Draft Accounts", "Search and view accounts")
            .doesNotContain("Create and Manage Draft Accounts", "Account Enquiry - Account Notes");

        assertThat(helper.countRoleAssignmentsForUserRole(userId, oldRoleId)).isZero();
        assertThat(helper.countRoleAssignmentsForUserRole(userId, replacementRoleId)).isEqualTo(2L);
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
        TestHelperUtil.setAuthenticatedUser(user);
        helper.insertBusinessUnitUser(buuId1, buId1, USER_ID);
        helper.insertBusinessUnitUser(buuId2, buId2, USER_ID);
        helper.insertBusinessUnitUserRole(buuId1, ROLE_ID);
        helper.insertBusinessUnitUserRole(buuId2, ROLE_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(buuId1, buId1),
            TestHelperUtil.legacyBusinessUnitUser(buuId2, buId2)));
        helper.setRoleMappingCache(
            user,
            Map.of(ROLE_ID, Set.of(buId1, buId2, buId3NotReturnedByLegacy)),
            ROLE_MAPPING_USER_PREFIX);

        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk());

        helper.assertUserBusinessUnitIds(user.getUserId(), buId1, buId2);
        helper.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        helper.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        assertThat(helper.countRoleAssignmentsForUserBusinessUnit(
            user.getUserId(),
            buId3NotReturnedByLegacy,
            ROLE_ID
        )).isEqualTo(0L);
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(ACCOUNT_ACTIVATION_INITIATED);
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
        TestHelperUtil.setAuthenticatedUser(user);
        assertThat(helper.getActivationDate(user.getUserId())).isNull();
        helper.insertBusinessUnitUser(buuId1, buId1, USER_ID);
        helper.insertBusinessUnitUser(buuId2, buId2, USER_ID);
        helper.insertBusinessUnitUserRole(buuId1, ROLE_ID);
        helper.insertBusinessUnitUserRole(buuId2, ROLE_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(buuId1, buId1),
            TestHelperUtil.legacyBusinessUnitUser(buuId2, buId2),
            TestHelperUtil.legacyBusinessUnitUser(buuId3, buId3)
        ));
        Map<Long, Set<Short>> roleMappingCache = Map.of(ROLE_ID, Set.of(buId1, buId2, buId3));
        helper.setRoleMappingCache(user, roleMappingCache, ROLE_MAPPING_USER_PREFIX);

        //assertions on the response verify changes from the inner transaction are visible to outer calling code
        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.domains.*.business_unit_users[*].business_unit_id",
                containsInAnyOrder((int) buId1, (int) buId2, (int) buId3)
            ));

        helper.assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3);
        helper.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        helper.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        helper.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        LocalDateTime activationDateAfter = helper.getActivationDate(user.getUserId());
        assertThat(activationDateAfter).isNotNull();
        assertThat(activationDateAfter.toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(
            BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED,
            ACCOUNT_ACTIVATION_INITIATED);
        helper.assertRoleMappingCache(user, roleMappingCache, ROLE_MAPPING_USER_PREFIX);
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
        TestHelperUtil.setAuthenticatedUser(user);
        helper.insertBusinessUnitUser(buuId1, buId1, USER_ID);
        helper.insertBusinessUnitUser(buuId2, buId2, USER_ID);
        helper.insertBusinessUnitUserRole(buuId1, roleNotInCache);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(buuId1, buId1),
            TestHelperUtil.legacyBusinessUnitUser(buuId2, buId2),
            TestHelperUtil.legacyBusinessUnitUser(buuId3, buId3)
        ));
        helper.setRoleMappingCache(
            user,
            Map.of(ROLE_ID, Set.of(buId1, buId2, buId3)),
            ROLE_MAPPING_USER_PREFIX);

        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk());

        helper.assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3);
        helper.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        helper.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        helper.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        assertThat(helper.countRoleAssignmentsForUserRole(user.getUserId(), roleNotInCache)).isEqualTo(0L);
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(
            ROLE_ASSIGNED_TO_USER,
            ROLE_UNASSIGNED_FROM_USER,
            ACCOUNT_ACTIVATION_INITIATED);
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
        TestHelperUtil.setAuthenticatedUser(user);
        helper.updateUserActivationDate(user.getUserId(), existingActivationDate);
        helper.insertBusinessUnitUser(buuId1, buId1, USER_ID);
        helper.insertBusinessUnitUser(buuId2, buId2, USER_ID);
        helper.insertBusinessUnitUserRole(buuId1, ROLE_ID);
        helper.insertBusinessUnitUserRole(buuId2, ROLE_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(buuId1, buId1),
            TestHelperUtil.legacyBusinessUnitUser(buuId2, buId2),
            TestHelperUtil.legacyBusinessUnitUser(buuId3, buId3)
        ));
        Map<Long, Set<Short>> roleMappingCache = Map.of(ROLE_ID, Set.of(buId1, buId2, buId3));
        helper.setRoleMappingCache(user, roleMappingCache, ROLE_MAPPING_USER_PREFIX);

        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk());

        helper.assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3);
        helper.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        helper.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        helper.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        assertThat(helper.getActivationDate(user.getUserId())).isEqualTo(existingActivationDate);
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED);
        helper.assertRoleMappingCache(user, roleMappingCache, ROLE_MAPPING_USER_PREFIX);
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
        TestHelperUtil.setAuthenticatedUser(user);
        assertThat(helper.getActivationDate(user.getUserId())).isNull();
        helper.insertBusinessUnitUser(buuId1, buId1, USER_ID);
        helper.insertBusinessUnitUser(buuId2, buId2, USER_ID);
        helper.insertBusinessUnitUser(buuId5NotInLegacy, buId5NotInLegacy, USER_ID);
        helper.insertBusinessUnitUserRole(buuId1, ROLE_ID);
        helper.insertBusinessUnitUserRole(buuId1, secondRoleId);
        helper.insertBusinessUnitUserRole(buuId2, ROLE_ID);
        helper.insertBusinessUnitUserRole(buuId2, secondRoleId);
        helper.insertBusinessUnitUserRole(buuId5NotInLegacy, thirdRoleId);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            TestHelperUtil.legacyBusinessUnitUser(buuId1, buId1),
            TestHelperUtil.legacyBusinessUnitUser(buuId2, buId2),
            TestHelperUtil.legacyBusinessUnitUser(buuId3, buId3),
            TestHelperUtil.legacyBusinessUnitUser(buuId4, buId4)
        ));
        helper.setRoleMappingCache(
            user,
            Map.of(
                ROLE_ID, Set.of(buId1, buId2, buId3),
                secondRoleId, Set.of(buId1, buId2, buId3),
                thirdRoleId, Set.of(buId3, buId4)
            ),
            ROLE_MAPPING_USER_PREFIX);

        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isOk());

        helper.assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3, buId4);
        helper.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        helper.assertBusinessUnitUserRow(buuId1, buId1, USER_ID, secondRoleId, 1L);
        helper.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        helper.assertBusinessUnitUserRow(buuId2, buId2, USER_ID, secondRoleId, 1L);
        helper.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        helper.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, secondRoleId, 1L);
        helper.assertBusinessUnitUserRow(buuId3, buId3, USER_ID, thirdRoleId, 1L);
        helper.assertBusinessUnitUserRow(buuId4, buId4, USER_ID, thirdRoleId, 1L);
        assertThat(helper.businessUnitUserExists(buuId5NotInLegacy)).isFalse();
        assertThat(helper.countRoleAssignmentsForUserRole(user.getUserId(), ROLE_ID)).isEqualTo(3L);
        assertThat(helper.countRoleAssignmentsForUserRole(user.getUserId(), secondRoleId)).isEqualTo(3L);
        assertThat(helper.countRoleAssignmentsForUserRole(user.getUserId(), thirdRoleId)).isEqualTo(2L);
        assertThat(helper.countRoleAssignments(user.getUserId())).isEqualTo(8L);
        LocalDateTime activationDateAfter = helper.getActivationDate(user.getUserId());
        assertThat(activationDateAfter).isNotNull();
        assertThat(activationDateAfter.toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactlyInAnyOrder(
            BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED,
            BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED,
            ROLE_ASSIGNED_TO_USER,
            ACCOUNT_ACTIVATION_INITIATED,
            ROLE_UNASSIGNED_FROM_USER);
    }

    // Causing the db exception that triggers the rollback:
    // business unit event with id 1 is inserted, but 'RESTART IDENTITY' means next id used will be 1 so collision
    @Test
    @DisplayName("Should roll back synchronisation when business event persistence fails")
    void getUserStateV2_whenBusinessEventPersistenceFails_rollsBackSynchronisation() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        TestHelperUtil.setAuthenticatedUser(user);
        assertThat(helper.getActivationDate(user.getUserId())).isNull();
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(TestHelperUtil.legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID)));
        helper.setRoleMappingCache(user, Map.of(ROLE_ID, Set.of(BUSINESS_UNIT_ID)), ROLE_MAPPING_USER_PREFIX);
        helper.insertBusinessEvent(1L, ROLE_ASSIGNED_TO_USER, USER_ID, USER_ID, "{}");
        assertThat(helper.businessUnitUserExists(BUSINESS_UNIT_USER_ID)).isFalse();
        long roleCountBefore = helper.countRoleAssignments(user.getUserId());

        mockMvc.perform(get(CURRENT_USER_STATE_URI))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Internal Server Error"))
            .andExpect(jsonPath("$.type").value("https://hmcts.gov.uk/problems/permissions-synchronization"))
            .andExpect(jsonPath("$.detail").value(
                "Could not synchronise permissions for user " + user.getUserId()
                    + " at stage: synchronise roles. Reason: unexpected runtime exception"
            ));

        assertThat(helper.businessUnitUserExists(BUSINESS_UNIT_USER_ID)).isFalse();
        assertThat(helper.countRoleAssignments(user.getUserId())).isEqualTo(roleCountBefore);
        assertThat(helper.getActivationDate(user.getUserId())).isNull();
        assertThat(helper.getLoggedBusinessEventTypes()).containsExactly(ROLE_ASSIGNED_TO_USER);
    }

    private Set<String> permissionNamesForBusinessUnit(String responseBody, short businessUnitId) throws Exception {
        JsonNode businessUnitUsers = objectMapper.readTree(responseBody)
            .path("domains")
            .path("fines")
            .path("business_unit_users");

        for (JsonNode businessUnitUser : businessUnitUsers) {
            if (businessUnitUser.path("business_unit_id").asInt() == businessUnitId) {
                Set<String> permissionNames = new LinkedHashSet<>();
                for (JsonNode permission : businessUnitUser.path("permissions")) {
                    permissionNames.add(permission.path("permission_name").asText());
                }
                return permissionNames;
            }
        }

        throw new IllegalStateException("Missing business unit in response: " + businessUnitId);
    }

}
