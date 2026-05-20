package uk.gov.hmcts.reform.opal.service.synchronise;

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
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("SynchroniseBusinessUnitUsersService integration tests")
class SynchroniseBusinessUnitUsersServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String SYNC_STAGE = "synchronise business unit users";

    @Autowired
    private SynchroniseBusinessUnitUsersService refreshBusinessUnitUsersService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestHelperService testHelperService;

    @Test
    @DisplayName("Should update existing business unit user when business unit and user differ")
    void synchroniseBusinessUnitUsers_updatesExistingBusinessUnitUser() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        TestHelperService.BusinessUnitUserSnapshot rowBefore = testHelperService.getBusinessUnitUserSnapshot("L081JG");
        assertThat(rowBefore.businessUnitId()).isEqualTo((short) 67);
        assertThat(rowBefore.userId()).isEqualTo(500000006L);

        refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(TestHelperUtil.legacyBusinessUnitUser("L081JG", "70"))
        );

        TestHelperService.BusinessUnitUserSnapshot updatedRow = testHelperService.getBusinessUnitUserSnapshot("L081JG");
        assertThat(updatedRow.businessUnitId()).isEqualTo((short) 70);
        assertThat(updatedRow.userId()).isEqualTo(500000000L);
    }

    @Test
    @DisplayName("Should insert a missing business unit user with mapped user and business unit")
    void synchroniseBusinessUnitUsers_insertsMissingBusinessUnitUser() {
        UserEntity user = userRepository.findById(500000001L).orElseThrow();
        long countBefore = testHelperService.businessUnitUserCount();

        refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(TestHelperUtil.legacyBusinessUnitUser("L099JG", "69"))
        );

        assertThat(testHelperService.businessUnitUserCount()).isEqualTo(countBefore + 1);
        TestHelperService.BusinessUnitUserSnapshot insertedRow = testHelperService.getBusinessUnitUserSnapshot("L099JG");
        assertThat(insertedRow.businessUnitId()).isEqualTo((short) 69);
        assertThat(insertedRow.userId()).isEqualTo(500000001L);
    }

    @Test
    @DisplayName("Should delete business unit users no longer returned by legacy")
    void synchroniseBusinessUnitUsers_deletesBusinessUnitsUserMissingFromLegacy() {
        UserEntity user = userRepository.findById(500000001L).orElseThrow();

        testHelperService.insertBusinessUnitUser("L091JG", (short) 67, user.getUserId());
        testHelperService.insertBusinessUnitUser("L092JG", (short) 69, user.getUserId());
        testHelperService.insertUserEntitlement("L092JG", 41L);
        testHelperService.insertBusinessUnitUserRole("L092JG", 2L);

        refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(TestHelperUtil.legacyBusinessUnitUser("L091JG", "67"))
        );

        assertThat(testHelperService.businessUnitUserExists("L091JG")).isTrue();
        assertThat(testHelperService.businessUnitUserExists("L092JG")).isFalse();
        assertThat(testHelperService.userEntitlementCount("L092JG")).isZero();
        assertThat(testHelperService.userRoleMappingCount("L092JG")).isZero();
    }

    // Bad BU_ID

    @Test
    @DisplayName("Should reject unknown business unit and leave existing business unit users unchanged")
    void synchroniseBusinessUnitUsers_rejectsUnknownBusinessUnit() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        TestHelperService.BusinessUnitUserSnapshot rowBefore = testHelperService.getBusinessUnitUserSnapshot("L082JG");
        long countBefore = testHelperService.businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(TestHelperUtil.legacyBusinessUnitUser("L082JG", "999"))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(TestHelperUtil.synchronisePermissionsErrorMessage(
                user.getUserId(),
                SYNC_STAGE,
                "legacy business unit not found: 999"
            ));

        assertThat(testHelperService.businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(testHelperService.getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "40000"})
    @DisplayName("Should reject malformed business unit id and leave existing business unit users unchanged")
    void synchroniseBusinessUnitUsers_rejectsMalformedBusinessUnitId(String malformedBusinessUnitId) {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        TestHelperService.BusinessUnitUserSnapshot rowBefore = testHelperService.getBusinessUnitUserSnapshot("L082JG");
        long countBefore = testHelperService.businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(TestHelperUtil.legacyBusinessUnitUser("L082JG", malformedBusinessUnitId))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(TestHelperUtil.synchronisePermissionsErrorMessage(
                user.getUserId(),
                SYNC_STAGE,
                "invalid business unit id: " + malformedBusinessUnitId
            ));

        assertThat(testHelperService.businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(testHelperService.getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }

    // Bad BUU_ID

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("Should reject missing business unit user id and leave existing business unit users unchanged")
    void synchroniseBusinessUnitUsers_rejectsMissingBusinessUnitUserId(String malformedBusinessUnitUserId) {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        TestHelperService.BusinessUnitUserSnapshot rowBefore = testHelperService.getBusinessUnitUserSnapshot("L082JG");
        long countBefore = testHelperService.businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(TestHelperUtil.legacyBusinessUnitUser(malformedBusinessUnitUserId, "69"))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(TestHelperUtil.synchronisePermissionsErrorMessage(
                user.getUserId(),
                SYNC_STAGE,
                "invalid business unit user id: " + malformedBusinessUnitUserId
            ));

        assertThat(testHelperService.businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(testHelperService.getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }

    @Test
    @DisplayName("Should reject overlength business unit user id and leave existing business unit users unchanged")
    void synchroniseBusinessUnitUsers_rejectsOverlengthBusinessUnitUserId() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        TestHelperService.BusinessUnitUserSnapshot rowBefore = testHelperService.getBusinessUnitUserSnapshot("L082JG");
        long countBefore = testHelperService.businessUnitUserCount();
        String malformedBusinessUnitUserId = "L082JG1";

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            List.of(TestHelperUtil.legacyBusinessUnitUser(malformedBusinessUnitUserId, "69"))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(TestHelperUtil.synchronisePermissionsErrorMessage(
                user.getUserId(),
                SYNC_STAGE,
                "invalid business unit user id: " + malformedBusinessUnitUserId
            ));

        assertThat(testHelperService.businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(testHelperService.getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }

    @Test
    @DisplayName("Should reject null legacy BU user payload and leave existing BU users unchanged")
    void synchroniseBusinessUnitUsers_rejectsNullLegacyBusinessUnitUsersPayload() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        TestHelperService.BusinessUnitUserSnapshot rowBefore = testHelperService.getBusinessUnitUserSnapshot("L082JG");
        long countBefore = testHelperService.businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(user, null))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(TestHelperUtil.synchronisePermissionsErrorMessage(
                user.getUserId(),
                SYNC_STAGE,
                "legacy business unit user payload is missing"
            ));

        assertThat(testHelperService.businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(testHelperService.getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }

    @Test
    @DisplayName("Should reject null entry in legacy BU user payload and leave existing BU users unchanged")
    void synchroniseBusinessUnitUsers_rejectsNullEntryInLegacyBusinessUnitUsersPayload() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        TestHelperService.BusinessUnitUserSnapshot rowBefore = testHelperService.getBusinessUnitUserSnapshot("L082JG");
        long countBefore = testHelperService.businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.synchroniseBusinessUnitsUsers(
            user,
            java.util.Collections.singletonList(null)
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage(TestHelperUtil.synchronisePermissionsErrorMessage(
                user.getUserId(),
                SYNC_STAGE,
                "legacy business unit user entry is missing"
            ));

        assertThat(testHelperService.businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(testHelperService.getBusinessUnitUserSnapshot("L082JG")).isEqualTo(rowBefore);
    }
}
