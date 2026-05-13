package uk.gov.hmcts.reform.opal.service.synchronise;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.dto.synchronise.LegacyBusinessUnitUser;
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("SynchroniseRolesService integration tests")
@Slf4j(topic = "opal.SynchroniseRolesServiceIntegrationTest")
class SynchroniseRolesServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String ROLE_MAPPING_USER_PREFIX = "ROLE_MAPPING_USER_";

    @Autowired
    private SynchroniseRolesService synchroniseRolesService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private UserPermissionsService userPermissionsService;

    @Test
    @DisplayName("Should apply cached role mappings and remove stale roles for legacy business units")
    void process_appliesCachedRolesAndRemovesStaleRoles() throws JsonProcessingException {
        // Arrange
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        when(userPermissionsService.getAuthenticatedUserId()).thenReturn(500000000L);

        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(Map.of(
            "2", Set.of("68", "73"),
            "3", Set.of("68", "70")
        )));

        assertThat(getAssignedBusinessUnitIds(500000000L, 1L)).containsExactly((short) 70);
        assertThat(getAssignedBusinessUnitIds(500000000L, 2L)).containsExactly((short) 70);
        assertThat(getAssignedBusinessUnitIds(500000000L, 3L)).isEmpty();
        logPermissionsSnapshot("before", 500000000L);

        try {
            // Act
            synchroniseRolesService.process(
                user,
                List.of(
                    legacyBusinessUnitUser("L066JG", "68"),
                    legacyBusinessUnitUser("L067JG", "73")
                )
            );

            // Assert
            assertThat(getAssignedBusinessUnitIds(500000000L, 1L)).isEmpty();
            assertThat(getAssignedBusinessUnitIds(500000000L, 2L)).containsExactlyInAnyOrder((short) 68, (short) 73);
            assertThat(getAssignedBusinessUnitIds(500000000L, 3L)).containsExactly((short) 68);
            assertThat(getReturnedPermissionNames("L065JG")).isEmpty();
            assertThat(getReturnedPermissionNames("L066JG")).isNotEmpty();
            assertThat(getReturnedPermissionNames("L067JG")).isNotEmpty();
            logPermissionsSnapshot("after", 500000000L);
        } finally {
            redisTemplate.delete(cacheKey);
        }
    }

    private LegacyBusinessUnitUser legacyBusinessUnitUser(String businessUnitUserId, String businessUnitId) {
        return LegacyBusinessUnitUser.builder()
            .businessUnitUserId(businessUnitUserId)
            .businessUnitId(businessUnitId)
            .build();
    }

    private Set<Short> getAssignedBusinessUnitIds(Long userId, Long roleId) {
        List<Short> businessUnitIds = jdbcTemplate.queryForList(
            """
                SELECT buu.business_unit_id
                FROM business_unit_user_roles buur
                JOIN business_unit_users buu ON buu.business_unit_user_id = buur.business_unit_user_id
                WHERE buu.user_id = ? AND buur.role_id = ?
                """,
            Short.class,
            userId,
            roleId
        );
        return new LinkedHashSet<>(businessUnitIds);
    }

    private Set<String> getReturnedPermissionNames(String businessUnitUserId) {
        List<String> permissionNames = jdbcTemplate.queryForList(
            """
                SELECT DISTINCT permission_name
                FROM business_unit_users buu
                JOIN business_unit_user_roles buur ON buur.business_unit_user_id = buu.business_unit_user_id
                JOIN v_current_roles role ON role.role_id = buur.role_id
                CROSS JOIN LATERAL unnest(role.application_function_list) AS permission_name
                WHERE buu.business_unit_user_id = ?
                ORDER BY permission_name
                """,
            String.class,
            businessUnitUserId
        );
        return new LinkedHashSet<>(permissionNames);
    }

    private void logPermissionsSnapshot(String label, Long userId) {
        log.info("User {} permissions {} sync:\n{}", userId, label, formatPermissionsSnapshotAsJson(userId));
    }

    private String formatPermissionsSnapshotAsJson(Long userId) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(getPermissionsSnapshot(userId));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialise permissions snapshot for user " + userId, exception);
        }
    }

    private List<Map<String, Object>> getPermissionsSnapshot(Long userId) {
        return jdbcTemplate.query(
            """
                SELECT
                  buu.business_unit_user_id,
                  buu.business_unit_id
                FROM business_unit_users buu
                WHERE buu.user_id = ?
                ORDER BY buu.business_unit_user_id
                """,
            (rs, rowNum) -> {
                String businessUnitUserId = rs.getString("business_unit_user_id");
                Map<String, Object> businessUnitSnapshot = new LinkedHashMap<>();
                businessUnitSnapshot.put("business_unit_id", rs.getShort("business_unit_id"));
                businessUnitSnapshot.put("roles", getAssignedRoleIds(businessUnitUserId));
                return businessUnitSnapshot;
            },
            userId
        );
    }

    private Set<Long> getAssignedRoleIds(String businessUnitUserId) {
        List<Long> roleIds = jdbcTemplate.queryForList(
            """
                SELECT buur.role_id
                FROM business_unit_user_roles buur
                WHERE buur.business_unit_user_id = ?
                ORDER BY buur.role_id
                """,
            Long.class,
            businessUnitUserId
        );
        return new LinkedHashSet<>(roleIds);
    }
}
