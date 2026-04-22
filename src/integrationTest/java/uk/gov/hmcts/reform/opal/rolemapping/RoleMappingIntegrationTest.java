package uk.gov.hmcts.reform.opal.rolemapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
import uk.gov.hmcts.reform.opal.service.rolemapping.MappingFileClient;
import uk.gov.hmcts.reform.opal.service.rolemapping.MappingFileSnapshot;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleMappingCacheService;
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleMappingRefreshService;

@ActiveProfiles({"integration"})
@TestPropertySource(properties = {
    "cache.role-mapping.user-ttl=3s",
    "cache.role-mapping.last-update-ttl=2s"
})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_user_state_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("User role mapping refresh integration tests")
class RoleMappingIntegrationTest extends AbstractIntegrationTest {

    private static final String LAST_UPDATE_AT = "2025-01-02T03:04:05.678";
    private static final String PREVIOUS_LAST_UPDATE_AT = "2025-01-01T00:00:00.000";

    @Autowired
    private UserRoleMappingRefreshService refreshService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private MappingFileClient mappingFileClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanRedis() {
        Set<String> keys = redisTemplate.keys(UserRoleMappingCacheService.ROLE_MAPPING_USER_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        redisTemplate.delete(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT);
    }

    @Test
    @DisplayName("AC1: when last update marker does not exist, cache should be refreshed")
    void ac1_whenLastUpdateMarkerMissing_refreshesCache() throws Exception {
        String csv = """
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU70,R2
                opal-test@HMCTS.NET,BU68,R3
                no-go-user@HMCTS.NET,BU67,R4
                """;

        when(mappingFileClient.readSnapshot()).thenReturn(snapshot(csv, LAST_UPDATE_AT));

        refreshService.refreshMappings();

        assertRedisJsonEquals("ROLE_MAPPING_USER_k9LpT2xVqR8m", Map.of(
            "BU70", List.of("R1", "R2"),
            "BU68", List.of("R3")
        ));

        assertRedisJsonEquals("ROLE_MAPPING_USER_8hqucbw874fg3", Map.of(
            "BU67", List.of("R4")
        ));

        assertEquals(
            LAST_UPDATE_AT,
            redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT)
        );

        assertTrue(redisTemplate.getExpire("ROLE_MAPPING_USER_k9LpT2xVqR8m", TimeUnit.SECONDS) > 0);
        assertTrue(redisTemplate.getExpire(
            UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT, TimeUnit.SECONDS) > 0);
    }

    @Test
    @DisplayName("AC2: when last update marker differs, cache should update")
    void ac2_whenLastUpdateMarkerDifferent_refreshesCache() throws Exception {
        redisTemplate.opsForValue().set(
            UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT,
            PREVIOUS_LAST_UPDATE_AT,
            Duration.ofSeconds(1)
        );

        String csv = """
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU70,R2
                opal-test@HMCTS.NET,BU68,R3
                no-go-user@HMCTS.NET,BU67,R4
                """;

        when(mappingFileClient.readSnapshot()).thenReturn(snapshot(csv, LAST_UPDATE_AT));

        refreshService.refreshMappings();

        assertRedisJsonEquals("ROLE_MAPPING_USER_k9LpT2xVqR8m", Map.of(
            "BU70", List.of("R1", "R2"),
            "BU68", List.of("R3")
        ));

        assertRedisJsonEquals("ROLE_MAPPING_USER_8hqucbw874fg3", Map.of(
            "BU67", List.of("R4")
        ));

        assertEquals(
            LAST_UPDATE_AT,
            redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT)
        );
    }

    @Test
    @DisplayName("AC3: when last update marker matches, cache should not update")
    void ac3_whenLastUpdateMarkerMatches_skipsRefresh(CapturedOutput output) throws Exception {
        String redisKey = "ROLE_MAPPING_USER_opal-test-subject-01";

        redisTemplate.opsForValue().set(
            redisKey,
            "{\"BU70\":[\"R1\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT,
            LAST_UPDATE_AT,
            Duration.ofHours(1)
        );

        String csv = """
            email_address,business_unit_id,role_id
            opal-test@HMCTS.NET,BU70,R1
            """;

        when(mappingFileClient.readSnapshot()).thenReturn(snapshot(csv, LAST_UPDATE_AT));

        refreshService.refreshMappings();

        assertEquals("{\"BU70\":[\"R1\"]}", redisTemplate.opsForValue().get(redisKey));
        assertEquals(LAST_UPDATE_AT, redisTemplate.opsForValue()
            .get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT));

    }

    @Test
    @DisplayName("AC4: when an email appears again non-contiguously, that user's cache should be deleted")
    void ac4_whenEmailReappearsNonContiguously_deletesInvalidUserCache() throws Exception {
        redisTemplate.opsForValue().set(
            "ROLE_MAPPING_USER_opal-test-subject-01",
            "{\"BU70\":[\"R1\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            "ROLE_MAPPING_USER_8hqucbw874fg3",
            "{\"BU67\":[\"R2\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT,
            PREVIOUS_LAST_UPDATE_AT,
            Duration.ofHours(1)
        );

        String csv = """
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU70,R2
                no-go-user@HMCTS.NET,BU67,R3
                opal-test@HMCTS.NET,BU68,R4
                """;

        when(mappingFileClient.readSnapshot()).thenReturn(snapshot(csv, LAST_UPDATE_AT));

        refreshService.refreshMappings();

        assertNull(redisTemplate.opsForValue().get("ROLE_MAPPING_USER_opal-test-subject-01"));
        assertNotNull(redisTemplate.opsForValue().get("ROLE_MAPPING_USER_8hqucbw874fg3"));
        assertEquals(LAST_UPDATE_AT, redisTemplate.opsForValue()
            .get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT));
    }

    @Test
    @DisplayName("AC5: user removed from file should have cache removed")
    void ac5_whenUserRemovedFromFile_deletesStaleCacheEntry() throws Exception {
        redisTemplate.opsForValue().set(
            "ROLE_MAPPING_USER_opal-test-subject-01",
            "{\"BU70\":[\"R1\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            "ROLE_MAPPING_USER_8hqucbw874fg3",
            "{\"BU67\":[\"R2\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            "ROLE_MAPPING_USER_GfsHbIMt49WjQ",
            "{\"BU73\":[\"R3\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT,
            PREVIOUS_LAST_UPDATE_AT,
            Duration.ofHours(1)
        );

        String csv = """
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU68,R2
                no-go-user@HMCTS.NET,BU67,R3
                no-go-user@HMCTS.NET,BU69,R4
                """;

        when(mappingFileClient.readSnapshot()).thenReturn(snapshot(csv, LAST_UPDATE_AT));

        refreshService.refreshMappings();

        assertNotNull(redisTemplate.opsForValue().get("ROLE_MAPPING_USER_k9LpT2xVqR8m"));
        assertNotNull(redisTemplate.opsForValue().get("ROLE_MAPPING_USER_8hqucbw874fg3"));
        assertNull(redisTemplate.opsForValue().get("ROLE_MAPPING_USER_GfsHbIMt49WjQ"));
        assertEquals(LAST_UPDATE_AT, redisTemplate.opsForValue()
            .get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT));
    }

    @Test
    @DisplayName("AC6: cached data should expire automatically after TTL")
    void ac6_cachedDataExpiresAutomatically() throws Exception {
        String csv = """
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                """;

        when(mappingFileClient.readSnapshot()).thenReturn(snapshot(csv, LAST_UPDATE_AT));

        refreshService.refreshMappings();

        assertNotNull(redisTemplate.opsForValue().get("ROLE_MAPPING_USER_k9LpT2xVqR8m"));
        assertNotNull(redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT));

        Thread.sleep(3500);

        assertNull(redisTemplate.opsForValue().get("ROLE_MAPPING_USER_opal-test-subject-01"));
        assertNull(redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT));
    }

    @Test
    @DisplayName("AC7: each cache entry should contain all business unit and role mappings for the user")
    void ac7_eachCacheEntryContainsAllMappings() throws Exception {
        String csv = """
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU70,R2
                opal-test@HMCTS.NET,BU68,R3
                opal-test@HMCTS.NET,BU61,R4
                no-go-user@HMCTS.NET,BU67,R1
                no-go-user@HMCTS.NET,BU69,R1
                """;

        when(mappingFileClient.readSnapshot()).thenReturn(snapshot(csv, LAST_UPDATE_AT));

        refreshService.refreshMappings();

        assertRedisJsonEquals("ROLE_MAPPING_USER_k9LpT2xVqR8m", Map.of(
            "BU70", List.of("R1", "R2"),
            "BU68", List.of("R3"),
            "BU61", List.of("R4")
        ));

        assertRedisJsonEquals("ROLE_MAPPING_USER_8hqucbw874fg3", Map.of(
            "BU67", List.of("R1"),
            "BU69", List.of("R1")
        ));

        assertEquals(
            LAST_UPDATE_AT,
            redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT)
        );
    }

    private MappingFileSnapshot snapshot(String csv, String lastModifiedAt) {
        return new MappingFileSnapshot(
            lastModifiedAt,
            new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        );
    }

    private void assertRedisJsonEquals(String redisKey, Map<String, List<String>> expected) throws Exception {
        String json = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(json);

        Map<String, List<String>> actual = objectMapper.readValue(
            json,
            new TypeReference<Map<String, List<String>>>() {
            }
        );

        assertEquals(expected, actual);
    }
}
