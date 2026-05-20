package uk.gov.hmcts.reform.opal.service.synchronise;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.dto.legacy.LegacyBusinessUnitUserId;
import uk.gov.hmcts.reform.opal.entity.ApplicationFunctionEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserEntity;
import uk.gov.hmcts.reform.opal.entity.BusinessUnitUserRoleEntity;
import uk.gov.hmcts.reform.opal.entity.RoleEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.entity.UserEntitlementEntity;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRepository;
import uk.gov.hmcts.reform.opal.repository.BusinessUnitUserRoleRepository;
import uk.gov.hmcts.reform.opal.repository.RoleRepository;
import uk.gov.hmcts.reform.opal.repository.TestRepository;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.repository.UserEntitlementRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("SynchroniseBusinessUnitUsersService integration tests")
@Slf4j(topic = "opal.SynchroniseBusinessUnitUsersServiceIntegrationTest")
class SynchroniseBusinessUnitUsersServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String SYNC_STAGE = "synchronise business unit users";

    @Autowired
    private SynchroniseBusinessUnitUsersService refreshBusinessUnitUsersService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusinessUnitRepository businessUnitRepository;

    @Autowired
    private BusinessUnitUserRepository businessUnitUserRepository;

    @Autowired
    private BusinessUnitUserRoleRepository businessUnitUserRoleRepository;

    @Autowired
    private UserEntitlementRepository userEntitlementRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should update existing business unit user when business unit and user differ")
    void synchroniseBusinessUnitUsers_updatesExistingBusinessUnitUser() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        BusinessUnitUserSnapshot rowBefore = getBusinessUnitUserSnapshot("L081JG");
        assertThat(rowBefore.businessUnitId()).isEqualTo((short) 67);
        assertThat(rowBefore.userId()).isEqualTo(500000006L);

        refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(legacyBusinessUnitUser("L081JG", "70"))
        );

        BusinessUnitUserSnapshot updatedRow = getBusinessUnitUserSnapshot("L081JG");
        assertThat(updatedRow.businessUnitId()).isEqualTo((short) 70);
        assertThat(updatedRow.userId()).isEqualTo(500000000L);
    }

    @Test
    @DisplayName("Should insert a missing business unit user with mapped user and business unit")
    void synchroniseBusinessUnitUsers_insertsMissingBusinessUnitUser() {
        UserEntity user = userRepository.findById(500000001L).orElseThrow();
        long countBefore = businessUnitUserCount();

        refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(legacyBusinessUnitUser("L099JG", "69"))
        );

        assertThat(businessUnitUserCount()).isEqualTo(countBefore + 1);
        BusinessUnitUserSnapshot insertedRow = getBusinessUnitUserSnapshot("L099JG");
        assertThat(insertedRow.businessUnitId()).isEqualTo((short) 69);
        assertThat(insertedRow.userId()).isEqualTo(500000001L);
    }

    @Test
    @DisplayName("Should delete business unit users no longer returned by legacy")
    void synchroniseBusinessUnitUsers_deletesBusinessUnitsUserMissingFromLegacy() {
        UserEntity user = userRepository.findById(500000001L).orElseThrow();

        insertBusinessUnitUser("L091JG", (short) 67, user.getUserId());
        insertBusinessUnitUser("L092JG", (short) 69, user.getUserId());
        insertUserEntitlement("L092JG", 41L);
        insertBusinessUnitUserRole("L092JG", 2L);

        refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(legacyBusinessUnitUser("L091JG", "67"))
        );

        assertThat(businessUnitUserExists("L091JG")).isTrue();
        assertThat(businessUnitUserExists("L092JG")).isFalse();
        assertThat(userEntitlementCount("L092JG")).isZero();
        assertThat(userRoleMappingCount("L092JG")).isZero();
    }

    // Bad BU_ID

    @Test
    @DisplayName("Should reject unknown business unit and leave existing business unit users unchanged")
    void synchroniseBusinessUnitUsers_rejectsUnknownBusinessUnit() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        BusinessUnitUserSnapshot rowBefore = getBusinessUnitUserSnapshot("L082JG");
        long countBefore = businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(legacyBusinessUnitUser("L082JG", "999"))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(errorMessage(user.getUserId(), "legacy business unit not found: 999"));

        assertThat(businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "40000"})
    @DisplayName("Should reject malformed business unit id and leave existing business unit users unchanged")
    void synchroniseBusinessUnitUsers_rejectsMalformedBusinessUnitId(String malformedBusinessUnitId) {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        BusinessUnitUserSnapshot rowBefore = getBusinessUnitUserSnapshot("L082JG");
        long countBefore = businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(legacyBusinessUnitUser("L082JG", malformedBusinessUnitId))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(errorMessage(user.getUserId(), "invalid business unit id: " + malformedBusinessUnitId));

        assertThat(businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }

    // Bad BUU_ID

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("Should reject missing business unit user id and leave existing business unit users unchanged")
    void synchroniseBusinessUnitUsers_rejectsMissingBusinessUnitUserId(String malformedBusinessUnitUserId) {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        BusinessUnitUserSnapshot rowBefore = getBusinessUnitUserSnapshot("L082JG");
        long countBefore = businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(legacyBusinessUnitUser(malformedBusinessUnitUserId, "69"))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(errorMessage(user.getUserId(), "invalid business unit user id: "
                + malformedBusinessUnitUserId));

        assertThat(businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }

    @Test
    @DisplayName("Should reject overlength business unit user id and leave existing business unit users unchanged")
    void synchroniseBusinessUnitUsers_rejectsOverlengthBusinessUnitUserId() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        BusinessUnitUserSnapshot rowBefore = getBusinessUnitUserSnapshot("L082JG");
        long countBefore = businessUnitUserCount();
        String malformedBusinessUnitUserId = "L082JG1";

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(legacyBusinessUnitUser(malformedBusinessUnitUserId, "69"))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(errorMessage(user.getUserId(), "invalid business unit user id: "
                + malformedBusinessUnitUserId));

        assertThat(businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }

    @Test
    @DisplayName("Should reject null legacy BU user payload and leave existing BU users unchanged")
    void synchroniseBusinessUnitUsers_rejectsNullLegacyBusinessUnitUsersPayload() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        BusinessUnitUserSnapshot rowBefore = getBusinessUnitUserSnapshot("L082JG");
        long countBefore = businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(user, null))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(errorMessage(user.getUserId(), "legacy business unit user payload is missing"));

        assertThat(businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }

    @Test
    @DisplayName("Should reject null entry in legacy BU user payload and leave existing BU users unchanged")
    void synchroniseBusinessUnitUsers_rejectsNullEntryInLegacyBusinessUnitUsersPayload() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        BusinessUnitUserSnapshot rowBefore = getBusinessUnitUserSnapshot("L082JG");
        long countBefore = businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            java.util.Collections.singletonList(null)
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(errorMessage(user.getUserId(), "legacy business unit user entry is missing"));

        assertThat(businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }

    private LegacyBusinessUnitUserId legacyBusinessUnitUser(String businessUnitUserId, String businessUnitId) {
        return LegacyBusinessUnitUserId.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnitId(businessUnitId)
            .build();
    }

    private void insertBusinessUnitUser(String businessUnitUserId, short businessUnitId, long userId) {
        BusinessUnitUserEntity businessUnitUser = BusinessUnitUserEntity.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnit(getRequiredBusinessUnit(businessUnitId))
            .user(getRequiredUser(userId))
            .build();
        businessUnitUserRepository.saveAndFlush(businessUnitUser);
    }

    private void insertUserEntitlement(String businessUnitUserId, long applicationFunctionId) {
        UserEntitlementEntity userEntitlement = UserEntitlementEntity.builder()
            .businessUnitUser(getRequiredBusinessUnitUser(businessUnitUserId))
            .applicationFunction(entityManager.getReference(ApplicationFunctionEntity.class, applicationFunctionId))
            .build();
        userEntitlementRepository.saveAndFlush(userEntitlement);
    }

    private void insertBusinessUnitUserRole(String businessUnitUserId, long roleId) {
        BusinessUnitUserRoleEntity businessUnitUserRole = BusinessUnitUserRoleEntity.builder()
            .businessUnitUser(getRequiredBusinessUnitUser(businessUnitUserId))
            .role(getRequiredRole(roleId))
            .build();
        businessUnitUserRoleRepository.saveAndFlush(businessUnitUserRole);
    }

    private BusinessUnitUserSnapshot getBusinessUnitUserSnapshot(String businessUnitUserId) {
        TestRepository.BusinessUnitUserRow row = testRepository.findBusinessUnitUserRowByBusinessUnitUserId(
            businessUnitUserId
        ).orElseThrow(() -> new IllegalStateException("Missing business unit user fixture: " + businessUnitUserId));
        return new BusinessUnitUserSnapshot(
            row.getBusinessUnitUserId(),
            row.getBusinessUnitId(),
            row.getUserId()
        );
    }

    private long businessUnitUserCount() {
        return testRepository.count();
    }

    private boolean businessUnitUserExists(String businessUnitUserId) {
        return testRepository.countByBusinessUnitUserId(businessUnitUserId) > 0;
    }

    private long userEntitlementCount(String businessUnitUserId) {
        return testRepository.countUserEntitlementsByBusinessUnitUserId(businessUnitUserId);
    }

    private long userRoleMappingCount(String businessUnitUserId) {
        return testRepository.countRoleMappingsByBusinessUnitUserId(businessUnitUserId);
    }

    private UserEntity getRequiredUser(long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("Missing user fixture: " + userId));
    }

    private BusinessUnitEntity getRequiredBusinessUnit(short businessUnitId) {
        return businessUnitRepository.findById(businessUnitId)
            .orElseThrow(() -> new IllegalStateException("Missing business unit fixture: " + businessUnitId));
    }

    private BusinessUnitUserEntity getRequiredBusinessUnitUser(String businessUnitUserId) {
        return businessUnitUserRepository.findById(businessUnitUserId)
            .orElseThrow(() -> new IllegalStateException("Missing business unit user fixture: " + businessUnitUserId));
    }

    private RoleEntity getRequiredRole(long roleId) {
        return roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalStateException("Missing role fixture: " + roleId));
    }

    private String errorMessage(long userId, String reason) {
        return "Could not synchronise permissions for user " + userId
            + " at stage: " + SYNC_STAGE
            + ". Reason: " + reason;
    }

    private record BusinessUnitUserSnapshot(String businessUnitUserId, short businessUnitId, long userId) {
    }
}
