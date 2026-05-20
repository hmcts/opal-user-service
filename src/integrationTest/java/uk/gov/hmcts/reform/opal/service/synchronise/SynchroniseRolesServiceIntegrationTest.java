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
import uk.gov.hmcts.reform.opal.entity.UserEntity;
import uk.gov.hmcts.reform.opal.repository.UserRepository;
import uk.gov.hmcts.reform.opal.service.UserPermissionsService;

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

    @Autowired
    private TestHelperService testHelperService;

    @MockitoBean
    private UserPermissionsService userPermissionsService;

    @Test
    @DisplayName("Should apply cached role mappings and remove stale roles for legacy business units")
    void synchroniseRoles_appliesCachedRolesAndRemovesStaleRoles() throws JsonProcessingException {
        // Arrange
        UserEntity user = userRepository.findById(500000000L).orElseThrow();
        when(userPermissionsService.getAuthenticatedUserId()).thenReturn(500000000L);

        String cacheKey = ROLE_MAPPING_USER_PREFIX + user.getTokenSubject();
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(Map.of(
            "2", Set.of("68", "73"),
            "3", Set.of("68", "70")
        )));

        assertThat(testHelperService.getAssignedBusinessUnitIds(500000000L, 1L)).containsExactly((short) 70);
        assertThat(testHelperService.getAssignedBusinessUnitIds(500000000L, 2L)).containsExactly((short) 70);
        assertThat(testHelperService.getAssignedBusinessUnitIds(500000000L, 3L)).isEmpty();
        log.info(
            "User {} permissions {} sync:\n{}",
            500000000L,
            "before",
            testHelperService.formatPermissionsSnapshotAsJson(500000000L)
        );

        try {
            // Act
            synchroniseRolesService.synchroniseRoles(
                user,
                List.of(
                    testHelperService.legacyBusinessUnitUser("L066JG", "68"),
                    testHelperService.legacyBusinessUnitUser("L067JG", "73")
                )
            );

            // Assert
            assertThat(testHelperService.getAssignedBusinessUnitIds(500000000L, 1L)).isEmpty();
            assertThat(testHelperService.getAssignedBusinessUnitIds(500000000L, 2L))
                .containsExactlyInAnyOrder((short) 68, (short) 73);
            assertThat(testHelperService.getAssignedBusinessUnitIds(500000000L, 3L)).containsExactly((short) 68);
            assertThat(testHelperService.getReturnedPermissionNames("L065JG")).isEmpty();
            assertThat(testHelperService.getReturnedPermissionNames("L066JG")).isNotEmpty();
            assertThat(testHelperService.getReturnedPermissionNames("L067JG")).isNotEmpty();
            log.info(
                "User {} permissions {} sync:\n{}",
                500000000L,
                "after",
                testHelperService.formatPermissionsSnapshotAsJson(500000000L)
            );
        } finally {
            redisTemplate.delete(cacheKey);
        }
    }
}
