package uk.gov.hmcts.reform.opal.rolemapping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.azure.storage.blob.BlobClient;
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
import uk.gov.hmcts.reform.opal.service.rolemapping.UserRoleMappingRefreshService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@TestPropertySource(properties = {
    "cache.role-mapping.user-ttl=3s",
    "cache.role-mapping.last-update-ttl=2s"
})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("User role mapping refresh integration tests")
@EnabledIfEnvironmentVariable(
    named = "ROLE_MAPPING_INTEGRATION_TESTS",
    matches = "true"
)
class RoleMappingIntegrationTest extends AbstractIntegrationTest {

    private static final String PREFIX = "ROLE_MAPPING_USER_";

    private static final String PREVIOUS_LAST_UPDATE_AT = "2025-01-01T00:00:00.000";

    private static final String OPAL_TEST_KEY = PREFIX + "k9LpT2xVqR8m";
    private static final String NO_GO_USER_KEY = PREFIX + "8hqucbw874fg3";
    private static final String TEST_USER_KEY = PREFIX + "GfsHbIMt49WjQ";

    @Autowired
    private UserRoleMappingRefreshService refreshService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private BlobClient blobClient;

    @BeforeEach
    void cleanRedis() {
        Set<String> keys = redisTemplate.keys(PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        redisTemplate.delete("USER_MAPPING_FILE_LAST_UPDATE_AT");
    }

    // --- AC1 ---
    @Test
    void ac1_whenLastUpdateMarkerMissing_refreshesCache() throws Exception {
        String lastModifiedAt = uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU70,R2
                opal-test@HMCTS.NET,BU68,R3
                no-go-user@HMCTS.NET,BU67,R4
                """);

        refreshService.refreshMappings();

        assertEquals(lastModifiedAt,
                     redisTemplate.opsForValue().get("USER_MAPPING_FILE_LAST_UPDATE_AT"));

        assertRedisJsonEquals(OPAL_TEST_KEY, Map.of(
            "BU70", List.of("R1", "R2"),
            "BU68", List.of("R3")
        ));

        assertRedisJsonEquals(NO_GO_USER_KEY, Map.of(
            "BU67", List.of("R4")
        ));
    }

    // --- AC2 ---
    @Test
    void ac2_whenLastUpdateMarkerDifferent_refreshesCache() throws Exception {
        redisTemplate.opsForValue().set(
            "USER_MAPPING_FILE_LAST_UPDATE_AT",
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

        assertEquals(lastModifiedAt,
                     redisTemplate.opsForValue().get("USER_MAPPING_FILE_LAST_UPDATE_AT"));
    }

    // --- AC3 ---
    @Test
    void ac3_whenLastUpdateMarkerMatches_skipsRefresh() throws Exception {
        String lastModifiedAt = uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                """);

        redisTemplate.opsForValue().set(OPAL_TEST_KEY, "{\"BU70\":[\"R1\"]}", Duration.ofHours(1));
        redisTemplate.opsForValue().set("USER_MAPPING_FILE_LAST_UPDATE_AT", lastModifiedAt, Duration.ofHours(1));

        refreshService.refreshMappings();

        assertEquals("{\"BU70\":[\"R1\"]}",
                     redisTemplate.opsForValue().get(OPAL_TEST_KEY));
    }

    // --- AC4 ---
    @Test
    void ac4_whenEmailReappearsNonContiguously_deletesInvalidUserCache() throws Exception {
        redisTemplate.opsForValue().set(OPAL_TEST_KEY, "{\"BU70\":[\"R1\"]}", Duration.ofHours(1));
        redisTemplate.opsForValue().set(NO_GO_USER_KEY, "{\"BU67\":[\"R2\"]}", Duration.ofHours(1));

        uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU70,R2
                no-go-user@HMCTS.NET,BU67,R3
                opal-test@HMCTS.NET,BU68,R4
                """);

        refreshService.refreshMappings();

        assertNull(redisTemplate.opsForValue().get(OPAL_TEST_KEY));
        assertNotNull(redisTemplate.opsForValue().get(NO_GO_USER_KEY));
    }

    // --- AC5 ---
    @Test
    void ac5_whenUserRemovedFromFile_deletesStaleCacheEntry() throws Exception {
        redisTemplate.opsForValue().set(OPAL_TEST_KEY, "{\"BU70\":[\"R1\"]}", Duration.ofHours(1));
        redisTemplate.opsForValue().set(NO_GO_USER_KEY, "{\"BU67\":[\"R2\"]}", Duration.ofHours(1));
        redisTemplate.opsForValue().set(TEST_USER_KEY, "{\"BU73\":[\"R3\"]}", Duration.ofHours(1));

        uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU68,R2
                no-go-user@HMCTS.NET,BU67,R3
                """);

        refreshService.refreshMappings();

        assertNull(redisTemplate.opsForValue().get(TEST_USER_KEY));
    }

    // --- AC6 ---
    @Test
    void ac6_cachedDataExpiresAutomatically() throws Exception {
        uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                """);

        refreshService.refreshMappings();

        Thread.sleep(3500);

        assertNull(redisTemplate.opsForValue().get(OPAL_TEST_KEY));
    }

    // --- AC7 ---
    @Test
    void ac7_eachCacheEntryContainsAllMappings() throws Exception {
        uploadCsv("""
                email_address,business_unit_id,role_id
                opal-test@HMCTS.NET,BU70,R1
                opal-test@HMCTS.NET,BU70,R2
                opal-test@HMCTS.NET,BU68,R3
                """);

        refreshService.refreshMappings();

        assertRedisJsonEquals(OPAL_TEST_KEY, Map.of(
            "BU70", List.of("R1", "R2"),
            "BU68", List.of("R3")
        ));
    }

    private String uploadCsv(String csv) {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        blobClient.getContainerClient().createIfNotExists();
        blobClient.upload(new ByteArrayInputStream(bytes), bytes.length, true);
        return blobClient.getProperties().getLastModified().toString();
    }

    private void assertRedisJsonEquals(String redisKey, Map<String, List<String>> expected) throws Exception {
        String json = redisTemplate.opsForValue().get(redisKey);
        assertNotNull(json);

        Map<String, List<String>> actual = objectMapper.readValue(
            json,
            new TypeReference<>() {
            }
        );

        assertEquals(expected, actual);
    }
}
