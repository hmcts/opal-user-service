package uk.gov.hmcts.reform.opal.controllers;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.LegacyWireMockXmlStubHelper;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.BusinessEventLogType;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private StringRedisTemplate redisTemplate;

    private LegacyWireMockXmlStubHelper legacyWireMockXmlStubHelper;

    @BeforeEach
    void initialiseEachTest() throws Exception {
        legacyWireMockXmlStubHelper = LegacyWireMockXmlStubHelper.initialise(objectMapper);
        resetBusinessEventsTable();
        clearRoleMappingCacheEntries();
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
        setAuthenticatedUser(user.getTokenSubject());
        assertUserHasNoActivationDate(user.getUserId());
        long roleCountBefore = countRoleAssignments(user.getUserId());
        legacyWireMockXmlStubHelper.registerSystemUserLookupStub(List.of(), 1);
        setRoleMappingCache(user, Map.of(ROLE_ID, Set.of(LEGACY_BUSINESS_UNIT_ID)));

        assertThat(roleCountBefore).isGreaterThan(0L);

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        assertUserRoleCount(user.getUserId(), 0L);
        assertUserHasNoActivationDate(user.getUserId());
        assertLoggedBusinessEventTypes();
    }


    @Test
    @DisplayName("AC2: should create missing BUU and assign role from cache when legacy returns BUU")
    void getUserStateV2_createsMissingBusinessUnitUserAndAssignsRole() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        setAuthenticatedUser(user.getTokenSubject());
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID))
        );
        setRoleMappingCache(user, Map.of(1L, Set.of(BUSINESS_UNIT_ID)));

        assertThat(businessUnitUserExists(BUSINESS_UNIT_USER_ID)).isFalse();

        mockMvc.perform(get(userStateUri(user.getUserId())));

        assertBusinessUnitUserRow(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        assertLoggedBusinessEventTypes(ROLE_ASSIGNED_TO_USER, ACCOUNT_ACTIVATION_INITIATED);
    }

    @Test
    @DisplayName("AC3: should reassign existing BUU to the requested user and assign role from cache")
    void getUserStateV2_reassignsExistingBusinessUnitUserToRequestedUser() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        setAuthenticatedUser(user.getTokenSubject());
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(legacyBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID))
        );
        setRoleMappingCache(user, Map.of(1L, Set.of(LEGACY_BUSINESS_UNIT_ID)));

        assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, DIFFERENT_USER_ID);

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        assertLoggedBusinessEventTypes(ROLE_ASSIGNED_TO_USER, ACCOUNT_ACTIVATION_INITIATED);
    }

    @Test
    @DisplayName("AC4: should update BU on existing BUU and assign role from cache")
    void getUserStateV2_updatesBusinessUnitIdOnExistingBusinessUnitUser() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        setAuthenticatedUser(user.getTokenSubject());
        updateBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(legacyBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID))
        );
        setRoleMappingCache(user, Map.of(1L, Set.of(BUSINESS_UNIT_ID)));

        assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID);

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        assertBusinessUnitUserRow(EXISTING_BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID, USER_ID, ROLE_ID, 1L);
        assertLoggedBusinessEventTypes(ROLE_ASSIGNED_TO_USER, ACCOUNT_ACTIVATION_INITIATED);
    }

    @Test
    @DisplayName("AC5/AC12: should return valid state, remove roles, and keep activation date unset when cache miss")
    void getUserStateV2_whenRoleMappingCacheEntryMissing_removesAllRoles() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        setAuthenticatedUser(user.getTokenSubject());
        assertUserHasNoActivationDate(user.getUserId());
        updateBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID, USER_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(legacyBusinessUnitUser(EXISTING_BUSINESS_UNIT_USER_ID, LEGACY_BUSINESS_UNIT_ID))
        );
        insertBusinessUnitUserRole(EXISTING_BUSINESS_UNIT_USER_ID, ROLE_ID);
        long roleCountBefore = countRoleAssignments(user.getUserId());

        assertThat(roleCountBefore).isGreaterThan(0L);

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        assertUserRoleCount(user.getUserId(), 0L);
        assertUserHasNoActivationDate(user.getUserId());
        assertLoggedBusinessEventTypes(ROLE_UNASSIGNED_FROM_USER);
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
        setAuthenticatedUser(user.getTokenSubject());
        insertBusinessUnitUser(buuId1, buId1, USER_ID);
        insertBusinessUnitUser(buuId2, buId2, USER_ID);
        insertBusinessUnitUserRole(buuId1, ROLE_ID);
        insertBusinessUnitUserRole(buuId2, ROLE_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            legacyBusinessUnitUser(buuId1, buId1),
            legacyBusinessUnitUser(buuId2, buId2)
        ));
        setRoleMappingCache(user, Map.of(ROLE_ID, Set.of(buId1, buId2, buId3NotReturnedByLegacy)));

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        assertUserBusinessUnitIds(user.getUserId(), buId1, buId2);
        assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        assertUserBusinessUnitRoleCount(user.getUserId(), buId3NotReturnedByLegacy, ROLE_ID, 0L);
        assertLoggedBusinessEventTypes(BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED, ACCOUNT_ACTIVATION_INITIATED);
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
        setAuthenticatedUser(user.getTokenSubject());
        assertUserHasNoActivationDate(user.getUserId());
        insertBusinessUnitUser(buuId1, buId1, USER_ID);
        insertBusinessUnitUser(buuId2, buId2, USER_ID);
        insertBusinessUnitUserRole(buuId1, ROLE_ID);
        insertBusinessUnitUserRole(buuId2, ROLE_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            legacyBusinessUnitUser(buuId1, buId1),
            legacyBusinessUnitUser(buuId2, buId2),
            legacyBusinessUnitUser(buuId3, buId3)
        ));
        Map<Long, Set<Short>> roleMappingCache = Map.of(ROLE_ID, Set.of(buId1, buId2, buId3));
        setRoleMappingCache(user, roleMappingCache);

        //assertions on the response verify changes from the inner transaction are visible to outer calling code
        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.domains.*.business_unit_users[*].business_unit_id",
                containsInAnyOrder((int) buId1, (int) buId2, (int) buId3)
            ));

        assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3);
        assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        assertUserActivationDateIsToday(user.getUserId());
        assertLoggedBusinessEventTypes(BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED, ACCOUNT_ACTIVATION_INITIATED);
        assertRoleMappingCache(user, roleMappingCache);
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
        setAuthenticatedUser(user.getTokenSubject());
        insertBusinessUnitUser(buuId1, buId1, USER_ID);
        insertBusinessUnitUser(buuId2, buId2, USER_ID);
        insertBusinessUnitUserRole(buuId1, roleNotInCache);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            legacyBusinessUnitUser(buuId1, buId1),
            legacyBusinessUnitUser(buuId2, buId2),
            legacyBusinessUnitUser(buuId3, buId3)
        ));
        setRoleMappingCache(user, Map.of(ROLE_ID, Set.of(buId1, buId2, buId3)));

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3);
        assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        assertUserRoleAssignmentCount(user.getUserId(), roleNotInCache, 0L);
        assertLoggedBusinessEventTypes(ROLE_ASSIGNED_TO_USER, ROLE_UNASSIGNED_FROM_USER, ACCOUNT_ACTIVATION_INITIATED);
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
        setAuthenticatedUser(user.getTokenSubject());
        updateUserActivationDate(user.getUserId(), existingActivationDate);
        insertBusinessUnitUser(buuId1, buId1, USER_ID);
        insertBusinessUnitUser(buuId2, buId2, USER_ID);
        insertBusinessUnitUserRole(buuId1, ROLE_ID);
        insertBusinessUnitUserRole(buuId2, ROLE_ID);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            legacyBusinessUnitUser(buuId1, buId1),
            legacyBusinessUnitUser(buuId2, buId2),
            legacyBusinessUnitUser(buuId3, buId3)
        ));
        Map<Long, Set<Short>> roleMappingCache = Map.of(ROLE_ID, Set.of(buId1, buId2, buId3));
        setRoleMappingCache(user, roleMappingCache);

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3);
        assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        assertUserActivationDate(user.getUserId(), existingActivationDate);
        assertLoggedBusinessEventTypes(BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED);
        assertRoleMappingCache(user, roleMappingCache);
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
        setAuthenticatedUser(user.getTokenSubject());
        assertUserHasNoActivationDate(user.getUserId());
        insertBusinessUnitUser(buuId1, buId1, USER_ID);
        insertBusinessUnitUser(buuId2, buId2, USER_ID);
        insertBusinessUnitUser(buuId5NotInLegacy, buId5NotInLegacy, USER_ID);
        insertBusinessUnitUserRole(buuId1, ROLE_ID);
        insertBusinessUnitUserRole(buuId1, secondRoleId);
        insertBusinessUnitUserRole(buuId2, ROLE_ID);
        insertBusinessUnitUserRole(buuId2, secondRoleId);
        insertBusinessUnitUserRole(buuId5NotInLegacy, thirdRoleId);
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(List.of(
            legacyBusinessUnitUser(buuId1, buId1),
            legacyBusinessUnitUser(buuId2, buId2),
            legacyBusinessUnitUser(buuId3, buId3),
            legacyBusinessUnitUser(buuId4, buId4)
        ));
        setRoleMappingCache(user, Map.of(
            ROLE_ID, Set.of(buId1, buId2, buId3),
            secondRoleId, Set.of(buId1, buId2, buId3),
            thirdRoleId, Set.of(buId3, buId4)
        ));

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isOk());

        assertUserBusinessUnitIds(user.getUserId(), buId1, buId2, buId3, buId4);
        assertBusinessUnitUserRow(buuId1, buId1, USER_ID, ROLE_ID, 1L);
        assertBusinessUnitUserRow(buuId1, buId1, USER_ID, secondRoleId, 1L);
        assertBusinessUnitUserRow(buuId2, buId2, USER_ID, ROLE_ID, 1L);
        assertBusinessUnitUserRow(buuId2, buId2, USER_ID, secondRoleId, 1L);
        assertBusinessUnitUserRow(buuId3, buId3, USER_ID, ROLE_ID, 1L);
        assertBusinessUnitUserRow(buuId3, buId3, USER_ID, secondRoleId, 1L);
        assertBusinessUnitUserRow(buuId3, buId3, USER_ID, thirdRoleId, 1L);
        assertBusinessUnitUserRow(buuId4, buId4, USER_ID, thirdRoleId, 1L);
        assertThat(businessUnitUserExists(buuId5NotInLegacy)).isFalse();
        assertUserRoleAssignmentCount(user.getUserId(), ROLE_ID, 3L);
        assertUserRoleAssignmentCount(user.getUserId(), secondRoleId, 3L);
        assertUserRoleAssignmentCount(user.getUserId(), thirdRoleId, 2L);
        assertUserRoleCount(user.getUserId(), 8L);
        assertUserActivationDateIsToday(user.getUserId());
        assertLoggedBusinessEventTypesInAnyOrder(
            BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED,
            BUSINESS_UNITS_ASSOCIATED_TO_ROLE_AMENDED,
            ROLE_ASSIGNED_TO_USER,
            ACCOUNT_ACTIVATION_INITIATED
        );
    }

    @Test
    @DisplayName("Should roll back synchronisation when business event persistence fails")
    void getUserStateV2_whenBusinessEventPersistenceFails_rollsBackSynchronisation() throws Exception {
        UserEntity user = userRepository.findById(USER_ID).orElseThrow();
        setAuthenticatedUser(user.getTokenSubject());
        assertUserHasNoActivationDate(user.getUserId());
        legacyWireMockXmlStubHelper.registerBusinessUnitUserLookupStub(
            List.of(legacyBusinessUnitUser(BUSINESS_UNIT_USER_ID, BUSINESS_UNIT_ID))
        );
        setRoleMappingCache(user, Map.of(ROLE_ID, Set.of(BUSINESS_UNIT_ID)));
        insertBusinessEvent(1L, ROLE_ASSIGNED_TO_USER, USER_ID, USER_ID, "{}");
        assertThat(businessUnitUserExists(BUSINESS_UNIT_USER_ID)).isFalse();
        long roleCountBefore = countRoleAssignments(user.getUserId());

        mockMvc.perform(get(userStateUri(user.getUserId())))
            .andExpect(status().isInternalServerError());

        assertThat(businessUnitUserExists(BUSINESS_UNIT_USER_ID)).isFalse();
        assertUserRoleCount(user.getUserId(), roleCountBefore);
        assertUserHasNoActivationDate(user.getUserId());
        assertLoggedBusinessEventTypes(ROLE_ASSIGNED_TO_USER);
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

    private LegacyBusinessUnitUserId legacyBusinessUnitUser(String businessUnitUserId, short businessUnitId) {
        return LegacyBusinessUnitUserId.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnitId(Short.toString(businessUnitId))
            .build();
    }

    private String userStateUri(long userId) {
        return "/v2/users/" + userId + "/state";
    }

    private void setRoleMappingCache(UserEntity user, Map<Long, Set<Short>> roleMapping) throws Exception {
        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(toCacheRoleMapping(roleMapping)));
    }

    private void assertRoleMappingCache(UserEntity user, Map<Long, Set<Short>> expectedRoleMapping) throws Exception {
        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();
        String actualRoleMappingCacheValue = redisTemplate.opsForValue().get(cacheKey);
        assertThat(actualRoleMappingCacheValue).isNotNull();
        assertThat(objectMapper.readTree(actualRoleMappingCacheValue)).isEqualTo(
            objectMapper.readTree(objectMapper.writeValueAsString(toCacheRoleMapping(expectedRoleMapping)))
        );
    }

    private Map<String, Set<String>> toCacheRoleMapping(Map<Long, Set<Short>> roleMapping) {
        Map<String, Set<String>> cacheRoleMapping = new LinkedHashMap<>();
        for (Map.Entry<Long, Set<Short>> roleMappingEntry : roleMapping.entrySet()) {
            Set<String> businessUnitIds = new LinkedHashSet<>();
            for (Short businessUnitId : roleMappingEntry.getValue()) {
                businessUnitIds.add(Short.toString(businessUnitId));
            }
            cacheRoleMapping.put(Long.toString(roleMappingEntry.getKey()), businessUnitIds);
        }
        return cacheRoleMapping;
    }

    private Map<String, Object> getBusinessUnitUserRow(long userId, String businessUnitUserId) {
        return jdbcTemplate.queryForMap(
            "SELECT business_unit_user_id, business_unit_id, user_id FROM business_unit_users WHERE user_id = ?"
                + " AND business_unit_user_id = ?",
            userId,
            businessUnitUserId
        );
    }

    private void assertBusinessUnitUserRow(String businessUnitUserId, short expectedBusinessUnitId,
                                           long expectedUserId) {
        Map<String, Object> businessUnitUserRow = getBusinessUnitUserRow(expectedUserId, businessUnitUserId);
        assertThat(((Number) businessUnitUserRow.get("user_id")).longValue()).isEqualTo(expectedUserId);
        assertThat(((Number) businessUnitUserRow.get("business_unit_id")).shortValue())
            .isEqualTo(expectedBusinessUnitId);
        assertThat(businessUnitUserRow.get("business_unit_user_id")).isEqualTo(businessUnitUserId);
    }

    private void assertBusinessUnitUserRow(
        String businessUnitUserId,
        short expectedBusinessUnitId,
        long expectedUserId,
        long roleId,
        long expectedRoleCount
    ) {
        assertBusinessUnitUserRow(businessUnitUserId, expectedBusinessUnitId, expectedUserId);
        assertThat(countRoleAssignmentsForBusinessUnitUser(businessUnitUserId, roleId)).isEqualTo(expectedRoleCount);
    }

    private void updateBusinessUnitUser(String businessUnitUserId, short businessUnitId, long userId) {
        jdbcTemplate.update(
            "UPDATE business_unit_users SET business_unit_id = ?, user_id = ? WHERE business_unit_user_id = ?",
            businessUnitId,
            userId,
            businessUnitUserId
        );
    }

    private void insertBusinessUnitUser(String businessUnitUserId, short businessUnitId, long userId) {
        jdbcTemplate.update(
            "INSERT INTO business_unit_users (business_unit_user_id, business_unit_id, user_id) VALUES (?, ?, ?)",
            businessUnitUserId,
            businessUnitId,
            userId
        );
    }

    private boolean businessUnitUserExists(String businessUnitUserId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM business_unit_users WHERE business_unit_user_id = ?",
            Long.class,
            businessUnitUserId
        );
        return count != null && count > 0;
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

    private long countRoleAssignmentsForBusinessUnitUser(String businessUnitUserId, long roleId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM business_unit_user_roles WHERE business_unit_user_id = ? AND role_id = ?",
            Long.class,
            businessUnitUserId,
            roleId
        );
        return count == null ? 0L : count;
    }

    private Set<Short> getUserBusinessUnitIds(long userId) {
        List<Short> businessUnitIds = jdbcTemplate.queryForList(
            "SELECT business_unit_id FROM business_unit_users WHERE user_id = ?",
            Short.class,
            userId
        );
        return new LinkedHashSet<>(businessUnitIds);
    }

    private long countRoleAssignmentsForUserBusinessUnit(long userId, short businessUnitId, long roleId) {
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
        return count == null ? 0L : count;
    }

    private long countRoleAssignmentsForUserRole(long userId, long roleId) {
        Long count = jdbcTemplate.queryForObject(
            """
                SELECT count(*)
                FROM business_unit_user_roles buur
                JOIN business_unit_users buu ON buu.business_unit_user_id = buur.business_unit_user_id
                WHERE buu.user_id = ? AND buur.role_id = ?
                """,
            Long.class,
            userId,
            roleId
        );
        return count == null ? 0L : count;
    }

    private LocalDateTime getUserActivationDate(long userId) {
        return jdbcTemplate.queryForObject(
            "SELECT activation_date FROM users WHERE user_id = ?",
            LocalDateTime.class,
            userId
        );
    }

    private void updateUserActivationDate(long userId, LocalDateTime activationDate) {
        jdbcTemplate.update(
            "UPDATE users SET activation_date = ? WHERE user_id = ?",
            activationDate,
            userId
        );
    }

    private void assertUserRoleCount(long userId, long expectedRoleCount) {
        assertThat(countRoleAssignments(userId)).isEqualTo(expectedRoleCount);
    }

    private void assertUserBusinessUnitIds(long userId, short... expectedBusinessUnitIds) {
        Set<Short> expectedBusinessUnitIdSet = new LinkedHashSet<>();
        for (short businessUnitId : expectedBusinessUnitIds) {
            expectedBusinessUnitIdSet.add(businessUnitId);
        }
        assertThat(getUserBusinessUnitIds(userId)).containsExactlyInAnyOrderElementsOf(expectedBusinessUnitIdSet);
    }

    private void assertUserBusinessUnitRoleCount(
        long userId,
        short businessUnitId,
        long roleId,
        long expectedRoleCount
    ) {
        assertThat(countRoleAssignmentsForUserBusinessUnit(userId, businessUnitId, roleId))
            .isEqualTo(expectedRoleCount);
    }

    private void assertUserRoleAssignmentCount(long userId, long roleId, long expectedRoleCount) {
        assertThat(countRoleAssignmentsForUserRole(userId, roleId)).isEqualTo(expectedRoleCount);
    }

    private void assertUserHasNoActivationDate(long userId) {
        assertThat(getUserActivationDate(userId)).isNull();
    }

    private void assertUserActivationDateIsToday(long userId) {
        LocalDateTime activationDate = getUserActivationDate(userId);
        assertThat(activationDate).isNotNull();
        assertThat(activationDate.toLocalDate()).isEqualTo(LocalDate.now());
    }

    private void assertUserActivationDate(long userId, LocalDateTime expectedActivationDate) {
        assertThat(getUserActivationDate(userId)).isEqualTo(expectedActivationDate);
    }

    private void insertBusinessUnitUserRole(String businessUnitUserId, long roleId) {
        jdbcTemplate.update(
            "INSERT INTO business_unit_user_roles (business_unit_user_role_id, business_unit_user_id, role_id)"
                + " VALUES (nextval('business_unit_user_role_id_seq'), ?, ?)",
            businessUnitUserId,
            roleId
        );
    }

    private void insertBusinessEvent(
        long businessEventId,
        BusinessEventLogType eventType,
        long subjectUserId,
        long initiatorUserId,
        String eventDetails
    ) {
        jdbcTemplate.update(
            "INSERT INTO business_events (business_event_id, event_type, subject_user_id, initiator_user_id,"
                + " event_details, event_date) VALUES (?, CAST(? AS t_event_type_enum), ?, ?, CAST(? AS json), NOW())",
            businessEventId,
            eventType.name(),
            subjectUserId,
            initiatorUserId,
            eventDetails
        );
    }

    private void resetBusinessEventsTable() {
        jdbcTemplate.execute("TRUNCATE TABLE business_events RESTART IDENTITY");
    }

    private void clearRoleMappingCacheEntries() {
        Set<String> cacheKeys = redisTemplate.keys(ROLE_MAPPING_USER_PREFIX + "*");
        if (cacheKeys != null && !cacheKeys.isEmpty()) {
            redisTemplate.delete(cacheKeys);
        }
    }

    private List<BusinessEventLogType> getLoggedBusinessEventTypes() {
        List<String> eventTypes = jdbcTemplate.queryForList(
            "SELECT event_type::text FROM business_events ORDER BY business_event_id",
            String.class
        );
        return eventTypes.stream()
            .map(BusinessEventLogType::valueOf)
            .toList();
    }

    private void assertLoggedBusinessEventTypes(BusinessEventLogType... expectedEventTypes) {
        assertThat(getLoggedBusinessEventTypes()).containsExactly(expectedEventTypes);
    }

    private void assertLoggedBusinessEventTypesInAnyOrder(BusinessEventLogType... expectedEventTypes) {
        assertThat(getLoggedBusinessEventTypes()).containsExactlyInAnyOrder(expectedEventTypes);
    }

}
