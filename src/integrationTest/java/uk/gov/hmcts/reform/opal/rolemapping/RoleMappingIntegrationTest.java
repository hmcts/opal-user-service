package uk.gov.hmcts.reform.opal.rolemapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

import com.azure.storage.blob.BlobClient;
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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;
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
@DisplayName("User role mapping refresh integration tests")
@EnabledIfEnvironmentVariable(
    named = "RUN_INTEGRATION_TESTS",
    matches = "true"
)
class RoleMappingIntegrationTest extends AbstractIntegrationTest {

    private static final String PREVIOUS_LAST_UPDATE_AT = "2025-01-01T00:00:00.000";

    private static final String OPAL_TEST_KEY = "ROLE_MAPPING_USER_k9LpT2xVqR8m";
    private static final String NO_GO_USER_KEY = "ROLE_MAPPING_USER_8hqucbw874fg3";
    private static final String TEST_USER_KEY = "ROLE_MAPPING_USER_GfsHbIMt49WjQ";

    @Autowired
    private UserRoleMappingRefreshService refreshService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private BlobClient blobClient;

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
    @DisplayName("AC1: when USER_MAPPING_FILE_LAST_UPDATE_AT does not exist, cache should be refreshed")
    void ac1_whenLastUpdateMarkerMissing_refreshesCache() throws Exception {
        String lastModifiedAt = uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU70,R2
                opal-test@HMCTS.NET,BU68,R3
                no-go-user@HMCTS.NET,BU67,R4
                """);

        refreshService.refreshMappings();

        assertEquals(
            lastModifiedAt,
            redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT)
        );

        assertRedisJsonEquals(OPAL_TEST_KEY, Map.of(
            "BU70", List.of("R1", "R2"),
            "BU68", List.of("R3")
        ));

        assertRedisJsonEquals(NO_GO_USER_KEY, Map.of(
            "BU67", List.of("R4")
        ));

        assertTrue(redisTemplate.getExpire(OPAL_TEST_KEY, TimeUnit.SECONDS) > 0);
        assertTrue(redisTemplate.getExpire(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT,
                                           TimeUnit.SECONDS) > 0);
    }

    @Test
    @DisplayName("AC2: when USER_MAPPING_FILE_LAST_UPDATE_AT differs from file last updated time, cache should update")
    void ac2_whenLastUpdateMarkerDifferent_refreshesCache() throws Exception {
        redisTemplate.opsForValue().set(
            UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT,
            PREVIOUS_LAST_UPDATE_AT,
            Duration.ofSeconds(1)
        );

        String lastModifiedAt = uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU70,R2
                opal-test@HMCTS.NET,BU68,R3
                no-go-user@HMCTS.NET,BU67,R4
                """);

        refreshService.refreshMappings();

        assertEquals(
            lastModifiedAt,
            redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT)
        );

        assertRedisJsonEquals(OPAL_TEST_KEY, Map.of(
            "BU70", List.of("R1", "R2"),
            "BU68", List.of("R3")
        ));

        assertRedisJsonEquals(NO_GO_USER_KEY, Map.of(
            "BU67", List.of("R4")
        ));

    }

    @Test
    @DisplayName("AC3: when USER_MAPPING_FILE_LAST_UPDATE_AT matches the file last updated time, "
        + "cache should not update")
    void ac3_whenLastUpdateMarkerMatches_skipsRefresh() throws Exception {
        String lastModifiedAt = uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                """);

        redisTemplate.opsForValue().set(
            OPAL_TEST_KEY,
            "{\"BU70\":[\"R1\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT,
            lastModifiedAt,
            Duration.ofHours(1)
        );

        refreshService.refreshMappings();

        assertEquals("{\"BU70\":[\"R1\"]}", redisTemplate.opsForValue().get(OPAL_TEST_KEY));
        assertEquals(
            lastModifiedAt,
            redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT)
        );
    }

    @Test
    @DisplayName("AC4: when an email appears again non-contiguously, that user's cache should be deleted")
    void ac4_whenEmailReappearsNonContiguously_deletesInvalidUserCache() throws Exception {

        redisTemplate.opsForValue().set(
            OPAL_TEST_KEY,
            "{\"BU70\":[\"R1\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            NO_GO_USER_KEY,
            "{\"BU67\":[\"R2\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT,
            PREVIOUS_LAST_UPDATE_AT,
            Duration.ofHours(1)
        );

        String lastModifiedAt = uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU70,R2
                no-go-user@HMCTS.NET,BU67,R3
                opal-test@HMCTS.NET,BU68,R4
                """);

        refreshService.refreshMappings();

        assertEquals(
            lastModifiedAt,
            redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT)
        );

        assertNull(redisTemplate.opsForValue().get(OPAL_TEST_KEY));
        assertNotNull(redisTemplate.opsForValue().get(NO_GO_USER_KEY));
    }

    @Test
    @DisplayName("AC5: when a user is removed from the file, that user's cache should be deleted")
    void ac5_whenUserRemovedFromFile_deletesStaleCacheEntry() throws Exception {


        redisTemplate.opsForValue().set(
            OPAL_TEST_KEY,
            "{\"BU70\":[\"R1\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            NO_GO_USER_KEY,
            "{\"BU67\":[\"R2\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            TEST_USER_KEY,
            "{\"BU73\":[\"R3\"]}",
            Duration.ofHours(1)
        );
        redisTemplate.opsForValue().set(
            UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT,
            PREVIOUS_LAST_UPDATE_AT,
            Duration.ofHours(1)
        );

        String lastModifiedAt = uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU68,R2
                no-go-user@HMCTS.NET,BU67,R3
                no-go-user@HMCTS.NET,BU69,R4
                """);

        refreshService.refreshMappings();

        assertEquals(
            lastModifiedAt,
            redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT)
        );

        assertNotNull(redisTemplate.opsForValue().get(OPAL_TEST_KEY));
        assertNotNull(redisTemplate.opsForValue().get(NO_GO_USER_KEY));
        assertNull(redisTemplate.opsForValue().get(TEST_USER_KEY));

    }

    @Test
    @DisplayName("AC6: cached data should expire automatically after TTL")
    void ac6_cachedDataExpiresAutomatically() throws Exception {
        uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                """);

        refreshService.refreshMappings();

        assertNotNull(redisTemplate.opsForValue().get(OPAL_TEST_KEY));
        assertNotNull(redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT));

        Thread.sleep(3500);

        assertNull(redisTemplate.opsForValue().get(OPAL_TEST_KEY));
        assertNull(redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT));
    }

    @Test
    @DisplayName("AC7: each cache entry should contain all business unit and role mappings for the user")
    void ac7_eachCacheEntryContainsAllMappings() throws Exception {
        String lastModifiedAt = uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU70,R2
                opal-test@HMCTS.NET,BU68,R3
                opal-test@HMCTS.NET,BU61,R4
                no-go-user@HMCTS.NET,BU67,R1
                no-go-user@HMCTS.NET,BU69,R1
                """);

        refreshService.refreshMappings();

        assertEquals(
            lastModifiedAt,
            redisTemplate.opsForValue().get(UserRoleMappingCacheService.USER_MAPPING_FILE_LAST_UPDATE_AT)
        );

        assertRedisJsonEquals(OPAL_TEST_KEY, Map.of(
            "BU70", List.of("R1", "R2"),
            "BU68", List.of("R3"),
            "BU61", List.of("R4")
        ));

        assertRedisJsonEquals(NO_GO_USER_KEY, Map.of(
            "BU67", List.of("R1"),
            "BU69", List.of("R1")
        ));


    }

    private String uploadCsv(String csv) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        blobClient.getContainerClient().createIfNotExists();
        blobClient.upload(
            new ByteArrayInputStream(bytes),
            bytes.length,
            true
        );
        return blobClient.getProperties().getLastModified().toString();
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
