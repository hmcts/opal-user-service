package uk.gov.hmcts.reform.opal.service.synchronise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.opal.AbstractIntegrationTest;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

@ActiveProfiles({"integration"})
@Sql(scripts = "classpath:db.reset/clean_test_data.sql", executionPhase = BEFORE_TEST_CLASS)
@Sql(scripts = "classpath:db.insertData/insert_authorisation_data.sql", executionPhase = BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@DisplayName("RoleMappingCacheLookupService integration tests")
class RoleMappingCacheLookupServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String ROLE_MAPPING_USER_PREFIX = "ROLE_MAPPING_USER_";
    private static final String TOKEN_SUBJECT = "subject-lookup-123";

    @Autowired
    private RoleMappingCacheLookupService roleMappingCacheLookupService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("Should read role mapping from Redis and convert ids to numeric types")
    void getRoleMappingByTokenSubject_readsFromRedisAndConvertsToNumericMap() throws Exception {
        String cacheKey = ROLE_MAPPING_USER_PREFIX + TOKEN_SUBJECT;
        redisTemplate.opsForValue().set(
            cacheKey,
            objectMapper.writeValueAsString(Map.of(
                "101", Set.of("7", "8"),
                "202", Set.of("9")
            ))
        );

        try {
            Map<Long, Set<Short>> result = roleMappingCacheLookupService.getRoleMappingByTokenSubject(TOKEN_SUBJECT);

            assertThat(result).isEqualTo(Map.of(
                101L, Set.of((short) 7, (short) 8),
                202L, Set.of((short) 9)
            ));
        } finally {
            redisTemplate.delete(cacheKey);
        }
    }

    @Test
    @DisplayName("Should throw SynchronisePermissionsException when Redis payload is invalid JSON")
    void getRoleMappingByTokenSubject_throwsWhenRedisPayloadIsInvalid() {
        String cacheKey = ROLE_MAPPING_USER_PREFIX + TOKEN_SUBJECT;
        redisTemplate.opsForValue().set(cacheKey, "not-json");

        try {
            assertThatThrownBy(() -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(TOKEN_SUBJECT))
                .isInstanceOf(SynchronisePermissionsException.class)
                .hasMessage("Could not parse role mapping cache : could not parse JSON");
        } finally {
            redisTemplate.delete(cacheKey);
        }
    }

    @Test
    @DisplayName("Should throw UserMissingFromCacheException when Redis payload is missing")
    void getRoleMappingByTokenSubject_throwsWhenRedisPayloadIsMissing() {
        String cacheKey = ROLE_MAPPING_USER_PREFIX + TOKEN_SUBJECT;
        redisTemplate.delete(cacheKey);

        assertThatThrownBy(() -> roleMappingCacheLookupService.getRoleMappingByTokenSubject(TOKEN_SUBJECT))
            .isInstanceOf(UserMissingFromCacheException.class)
            .hasMessage("Nothing in cache for : " + TOKEN_SUBJECT);
    }
}
