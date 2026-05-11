package uk.gov.hmcts.reform.opal.service.synchronise;

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
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUser;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;

import java.util.List;
import java.util.Map;

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

    @Autowired
    private SynchroniseBusinessUnitUsersService refreshBusinessUnitUsersService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should update existing business unit user when business unit and user differ")
    void refreshBusinessUnitUsers_updatesExistingBusinessUnitUser() throws SynchronisePermissionsException {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        Map<String, Object> rowBefore = getBusinessUnitUserRow("L081JG");
        assertThat(asInt(rowBefore.get("business_unit_id"))).isEqualTo(67);
        assertThat(asLong(rowBefore.get("user_id"))).isEqualTo(500000006L);

        refreshBusinessUnitUsersService.refreshBusinessUnitUsers(
            user,
            List.of(legacyBusinessUnitUser("L081JG", "70"))
        );

        Map<String, Object> updatedRow = getBusinessUnitUserRow("L081JG");
        assertThat(asInt(updatedRow.get("business_unit_id"))).isEqualTo(70);
        assertThat(asLong(updatedRow.get("user_id"))).isEqualTo(500000000L);
    }

    @Test
    @DisplayName("Should insert a missing business unit user with mapped user and business unit")
    void refreshBusinessUnitUsers_insertsMissingBusinessUnitUser() throws SynchronisePermissionsException {
        UserEntity user = userRepository.findById(500000001L).orElseThrow();
        Long countBefore = businessUnitUserCount();

        refreshBusinessUnitUsersService.refreshBusinessUnitUsers(
            user,
            List.of(legacyBusinessUnitUser("L099JG", "69"))
        );

        assertThat(businessUnitUserCount()).isEqualTo(countBefore + 1);
        Map<String, Object> insertedRow = getBusinessUnitUserRow("L099JG");
        assertThat(asInt(insertedRow.get("business_unit_id"))).isEqualTo(69);
        assertThat(asLong(insertedRow.get("user_id"))).isEqualTo(500000001L);
    }

    @Test
    @DisplayName("Should delete business unit users no longer returned by legacy")
    void refreshBusinessUnitUsers_deletesBusinessUnitUsersMissingFromLegacy() throws SynchronisePermissionsException {
        UserEntity user = userRepository.findById(500000001L).orElseThrow();

        jdbcTemplate.update(
            "INSERT INTO business_unit_users (business_unit_user_id, business_unit_id, user_id) VALUES (?, ?, ?)",
            "L091JG", 67, user.getUserId()
        );
        jdbcTemplate.update(
            "INSERT INTO business_unit_users (business_unit_user_id, business_unit_id, user_id) VALUES (?, ?, ?)",
            "L092JG", 69, user.getUserId()
        );
        jdbcTemplate.update(
            "INSERT INTO user_entitlements (user_entitlement_id, business_unit_user_id, application_function_id) "
                + "VALUES (?, ?, ?)",
            950001L, "L092JG", 41L
        );
        jdbcTemplate.update(
            "INSERT INTO business_unit_user_roles (business_unit_user_role_id, business_unit_user_id, role_id) "
                + "VALUES (?, ?, ?)",
            950001L, "L092JG", 2L
        );

        refreshBusinessUnitUsersService.refreshBusinessUnitUsers(
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
    void refreshBusinessUnitUsers_rejectsUnknownBusinessUnit() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        Map<String, Object> rowBefore = getBusinessUnitUserRow("L082JG");
        Long countBefore = businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.refreshBusinessUnitUsers(
            user,
            List.of(legacyBusinessUnitUser("L082JG", "999"))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage("legacyBusinessUnitUser not found for businessUnit");

        assertThat(businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(getBusinessUnitUserRow("L082JG")).isEqualTo(rowBefore);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "40000"})
    @DisplayName("Should reject malformed business unit id and leave existing business unit users unchanged")
    void refreshBusinessUnitUsers_rejectsMalformedBusinessUnitId(String malformedBusinessUnitId) {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        Map<String, Object> rowBefore = getBusinessUnitUserRow("L082JG");
        Long countBefore = businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.refreshBusinessUnitUsers(
            user,
            List.of(legacyBusinessUnitUser("L082JG", malformedBusinessUnitId))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage("Invalid business unit id: " + malformedBusinessUnitId);

        assertThat(businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(getBusinessUnitUserRow("L082JG")).isEqualTo(rowBefore);
    }

    // Bad BUU_ID

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("Should reject missing business unit user id and leave existing business unit users unchanged")
    void refreshBusinessUnitUsers_rejectsMissingBusinessUnitUserId(String malformedBusinessUnitUserId) {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        Map<String, Object> rowBefore = getBusinessUnitUserRow("L082JG");
        Long countBefore = businessUnitUserCount();

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.refreshBusinessUnitUsers(
            user,
            List.of(legacyBusinessUnitUser(malformedBusinessUnitUserId, "69"))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage("Invalid business unit user id: " + malformedBusinessUnitUserId);

        assertThat(businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(getBusinessUnitUserRow("L082JG")).isEqualTo(rowBefore);
    }

    @Test
    @DisplayName("Should reject overlength business unit user id and leave existing business unit users unchanged")
    void refreshBusinessUnitUsers_rejectsOverlengthBusinessUnitUserId() {
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        Map<String, Object> rowBefore = getBusinessUnitUserRow("L082JG");
        Long countBefore = businessUnitUserCount();
        String malformedBusinessUnitUserId = "L082JG1";

        assertThatThrownBy(() -> refreshBusinessUnitUsersService.refreshBusinessUnitUsers(
            user,
            List.of(legacyBusinessUnitUser(malformedBusinessUnitUserId, "69"))
        ))
            .isInstanceOf(SynchronisePermissionsException.class)
            .hasMessage("Invalid business unit user id: " + malformedBusinessUnitUserId);

        assertThat(businessUnitUserCount()).isEqualTo(countBefore);
        assertThat(getBusinessUnitUserRow("L082JG")).isEqualTo(rowBefore);
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

    private Long businessUnitUserCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM business_unit_users", Long.class);
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
}
